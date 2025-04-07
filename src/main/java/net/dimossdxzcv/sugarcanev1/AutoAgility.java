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
public class AutoAgility implements ClientModInitializer {
    private static final KeyBinding toggleKey = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.autoagility.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_M, "category.autoagility")
    );

    private static boolean isRunning = false;
    private static long lastCommandTime = 0;
    private static long lastFeedTime = 0;
    private static long lastHomeTime = 0;
    private static long deathTime = -1;
    private static boolean wasDead = false;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (toggleKey.wasPressed()) {
                toggle();
            }

            if (!isRunning) return;

            long currentTime = System.currentTimeMillis();
            MinecraftClient mc = MinecraftClient.getInstance();

            // Handle auto respawn and /home agility after death
            if (mc.player != null) {
                if (mc.player.isDead()) {
                    if (!wasDead) {
                        deathTime = currentTime;
                        wasDead = true;
                    }
                } else {
                    wasDead = false;
                }

                if (deathTime != -1 && (currentTime - deathTime) >= 5000) {
                    mc.player.networkHandler.sendChatCommand("home agility");
                    deathTime = -1;
                    wasDead = false;
                }
            }

            // Run /hub and /server survival sequence every 2 minutes
            if (currentTime - lastCommandTime >= 120000) {
                if (mc.player != null) {
                    mc.player.networkHandler.sendChatCommand("hub");
                    try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
                    mc.player.networkHandler.sendChatCommand("server survival");
                }
                lastCommandTime = currentTime;
            }

            // Run /feed every 60 seconds
            if (currentTime - lastFeedTime >= 60000) {
                if (mc.player != null) {
                    mc.player.networkHandler.sendChatCommand("feed");
                }
                lastFeedTime = currentTime;
            }

            // Run /home agility every 10 minutes
            if (currentTime - lastHomeTime >= 600000) {
                if (mc.player != null) {
                    mc.player.networkHandler.sendChatCommand("home agility");
                }
                lastHomeTime = currentTime;
            }
        });
    }

    public static void toggle() {
        isRunning = !isRunning;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            Text message = Text.literal("AutoAgility " + (isRunning ? "enabled" : "disabled"))
                    .setStyle(Style.EMPTY.withColor(isRunning ? 0x00FF00 : 0xFF0000));
            client.player.sendMessage(message, false);
        }

        if (isRunning) {
            lastCommandTime = System.currentTimeMillis();
            lastFeedTime = System.currentTimeMillis();
            lastHomeTime = System.currentTimeMillis();
            deathTime = -1;
            wasDead = false;
        }
    }

    public static boolean isRunning() {
        return isRunning;
    }

    public static KeyBinding getToggleKey() {
        return toggleKey;
    }
}