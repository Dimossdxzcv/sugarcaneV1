package net.dimossdxzcv.sugarcanev1;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class AutoFishing implements ClientModInitializer {
    private static final KeyBinding toggleKey = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.autofishing.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_O, "category.autofishing")
    );

    private static boolean isRunning = false;
    private static long lastRightClickTime = 0;
    private static long lastFeedTime = 0;
    private static long lastHomeTime = 0;
    private static boolean isReeling = false;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (toggleKey.wasPressed()) {
                toggle();
            }

            if (!isRunning) return;

            long currentTime = System.currentTimeMillis();
            MinecraftClient mc = MinecraftClient.getInstance();

            // Run /feed every 60 seconds
            if (currentTime - lastFeedTime >= 60000) {
                if (mc.player != null) {
                    mc.player.networkHandler.sendChatCommand("feed");
                }
                lastFeedTime = currentTime;
            }

            // Run /home fishing every 10 minutes
            if (currentTime - lastHomeTime >= 600000) {
                if (mc.player != null) {
                    mc.player.networkHandler.sendChatCommand("home fishing");
                }
                lastHomeTime = currentTime;
            }

            // Fishing logic: Cast and wait for bobber movement
            if (mc.player != null) {
                if (!isReeling && mc.player.fishHook == null) {
                    // Cast the fishing rod
                    mc.options.useKey.setPressed(true);
                    lastRightClickTime = currentTime;
                    isReeling = true;
                } else if (isReeling && currentTime - lastRightClickTime > 500) {
                    // Release button after casting
                    mc.options.useKey.setPressed(false);
                    isReeling = false;
                } else if (mc.player.fishHook != null && shouldReel(mc)) {
                    // Reel in when there's a bite
                    mc.options.useKey.setPressed(true);
                    lastRightClickTime = currentTime;
                    isReeling = true;
                }
            }
        });
    }

    // Simple detection for when to reel in
    // In a real implementation, you'd want to detect bobber movement or particles
    private boolean shouldReel(MinecraftClient mc) {
        if (mc.player.fishHook == null) return false;

        // This is a basic implementation
        // For more accurate detection you would check for bobber velocity changes
        // or fishing_bobber.isInOpenWater() in newer versions
        long hookTime = System.currentTimeMillis() - lastRightClickTime;
        return hookTime > 15000 && Math.random() < 0.1; // Random chance to simulate bite after 15s
    }

    public static void toggle() {
        isRunning = !isRunning;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            Text message = Text.literal("AutoFishing " + (isRunning ? "enabled" : "disabled"))
                    .setStyle(Style.EMPTY.withColor(isRunning ? 0x00FF00 : 0xFF0000));
            client.player.sendMessage(message, false);
        }

        if (isRunning) {
            lastRightClickTime = System.currentTimeMillis();
            lastFeedTime = System.currentTimeMillis();
            lastHomeTime = System.currentTimeMillis();
            isReeling = false;
        } else {
            client.options.useKey.setPressed(false);
        }
    }

    public static boolean isRunning() {
        return isRunning;
    }

    public static KeyBinding getToggleKey() {
        return toggleKey;
    }
}