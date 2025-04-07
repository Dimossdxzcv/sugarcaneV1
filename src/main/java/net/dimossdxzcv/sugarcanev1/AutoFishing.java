package net.dimossdxzcv.sugarcanev1;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class AutoFishing implements ClientModInitializer {
    private static final KeyBinding toggleKey = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.autofishing.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F, "category.autofishing")
    );

    private static boolean isRunning = false;
    private static long lastCastTime = 0;
    private static long lastFeedTime = 0;
    private static long lastCommandTime = 0;
    private static long lastYawPitchTime = 0;
    private static boolean isFishing = false;
    private static boolean hasDetectedBite = false;
    private static int particleCount = 0;
    private static boolean isIncreasing = true; // Tracks whether we're incrementing or decrementing yaw/pitch
    private static final int CAST_DELAY = 1500; // Delay between casts in ms
    private static final int FEED_INTERVAL = 60000; // Run /feed every minute
    private static final int SERVER_REFRESH_INTERVAL = 120000; // Run hub/survival every 2 minutes
    private static final int YAW_PITCH_INTERVAL = 60000; // Yaw/pitch adjustment every minute
    private static final int YAW_PITCH_RESTORE_DELAY = 5000; // Wait 5 seconds before restoring yaw/pitch

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (toggleKey.wasPressed()) {
                toggle();
            }

            if (!isRunning) return;

            long currentTime = System.currentTimeMillis();
            MinecraftClient mc = MinecraftClient.getInstance();

            if (mc.player == null) return;

            // Run /feed every minute
            if (currentTime - lastFeedTime >= FEED_INTERVAL) {
                mc.player.networkHandler.sendChatCommand("feed");
                lastFeedTime = currentTime;
            }

            // Run /hub and /server survival sequence every 2 minutes to prevent disconnects
            if (currentTime - lastCommandTime >= SERVER_REFRESH_INTERVAL) {
                mc.player.networkHandler.sendChatCommand("hub");
                try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
                mc.player.networkHandler.sendChatCommand("server survival");
                lastCommandTime = currentTime;
            }

            // Handle yaw and pitch adjustments every minute
            if (currentTime - lastYawPitchTime >= YAW_PITCH_INTERVAL) {
                // Store current values
                float currentYaw = mc.player.getYaw();
                float currentPitch = mc.player.getPitch();

                // Increment by 1
                mc.player.setYaw(currentYaw + 1.0f);
                mc.player.setPitch(currentPitch + 1.0f);

                // Schedule restoration after 5 seconds
                new Thread(() -> {
                    try {
                        Thread.sleep(YAW_PITCH_RESTORE_DELAY);
                        mc.execute(() -> {
                            if (mc.player != null && isRunning) {
                                mc.player.setYaw(currentYaw);
                                mc.player.setPitch(currentPitch);
                            }
                        });
                    } catch (InterruptedException ignored) {}
                }).start();

                lastYawPitchTime = currentTime;
            }

            // Fishing logic
            FishingBobberEntity bobber = mc.player.fishHook;

            // If not fishing and enough time has passed since last cast, cast again
            if (bobber == null && !isFishing && currentTime - lastCastTime >= CAST_DELAY) {
                // Right-click to cast
                mc.options.useKey.setPressed(true);
                isFishing = true;

                // Schedule to release the button
                mc.execute(() -> {
                    try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                    mc.options.useKey.setPressed(false);
                });

                lastCastTime = currentTime;
            } else if (bobber != null) {
                // We have a bobber in the water
                isFishing = true;

                // Check for bite - when the bobber velocity is downward
                if (bobber.getVelocity().y < -0.1) {
                    hasDetectedBite = true;
                    particleCount++;

                    // When we detect enough movement, reel in
                    if (particleCount >= 2) {
                        // Right-click to reel in
                        mc.options.useKey.setPressed(true);
                        mc.execute(() -> {
                            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                            mc.options.useKey.setPressed(false);
                        });

                        isFishing = false;
                        hasDetectedBite = false;
                        particleCount = 0;
                        lastCastTime = currentTime;
                    }
                } else {
                    // Reset particle count if bobber isn't moving downward
                    if (hasDetectedBite) {
                        particleCount = 0;
                        hasDetectedBite = false;
                    }
                }
            }
        });
    }

    public static void toggle() {
        isRunning = !isRunning;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            Text message = Text.literal("AutoFishing " + (isRunning ? "enabled" : "disabled"))
                    .setStyle(Style.EMPTY.withColor(isRunning ? 0x00FF00 : 0xFF0000));
            client.player.sendMessage(message, false);
        }

        if (!isRunning) {
            // Release the use key when turning off
            client.options.useKey.setPressed(false);
        } else {
            // Reset timers when turning on
            lastCastTime = System.currentTimeMillis();
            lastFeedTime = System.currentTimeMillis();
            lastCommandTime = System.currentTimeMillis();
            lastYawPitchTime = System.currentTimeMillis();
            isFishing = false;
            hasDetectedBite = false;
            particleCount = 0;
            isIncreasing = true;
        }
    }

    public static boolean isRunning() {
        return isRunning;
    }

    public static KeyBinding getToggleKey() {
        return toggleKey;
    }
}