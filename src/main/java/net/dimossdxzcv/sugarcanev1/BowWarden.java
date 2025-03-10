package net.dimossdxzcv.sugarcanev1;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class BowWarden implements ClientModInitializer {
    private static final KeyBinding startKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.mymod.start", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "category.mymod"));
    private static final KeyBinding pauseKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.mymod.pause", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT, "category.mymod"));

    private boolean isRunning = false;
    private long lastRightClickTime = 0;
    private long lastMoveTime = 0;
    private long lastCommandTime = 0;
    private int commandState = 0; // 0 = /hub, 1 = wait 5s, 2 = /server survival, 3 = wait 4s
    private int state = 1; // 1 = move left, 2 = wait, 3 = move right

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (startKey.wasPressed()) {
                isRunning = true;
                lastRightClickTime = System.currentTimeMillis();
                lastMoveTime = System.currentTimeMillis();
                lastCommandTime = System.currentTimeMillis();
                commandState = 0;
                state = 1;
            }

            if (pauseKey.wasPressed()) {
                isRunning = false;
                releaseKeys(client);
                return;
            }

            if (!isRunning) return;

            long currentTime = System.currentTimeMillis();
            MinecraftClient mc = MinecraftClient.getInstance();

            // Always loop right-click every 6000ms (3000ms hold, 3000ms wait)
            if (currentTime - lastRightClickTime < 3000) {
                mc.options.useKey.setPressed(true);
            } else if (currentTime - lastRightClickTime < 6000) {
                mc.options.useKey.setPressed(false);
            } else {
                lastRightClickTime = currentTime;
            }

            // Chat command sequence
            switch (commandState) {
                case 0: // Send /hub
                    if (currentTime - lastCommandTime >= 120000) { // Every 120 seconds
                        if (mc.player != null) {
                            mc.player.networkHandler.sendChatCommand("hub");
                        }
                        lastCommandTime = currentTime;
                        commandState = 1;
                    }
                    break;
                case 1: // Wait 5 seconds
                    if (currentTime - lastCommandTime >= 5000) {
                        commandState = 2;
                    }
                    break;
                case 2: // Send /server survival
                    if (mc.player != null) {
                        mc.player.networkHandler.sendChatCommand("server survival");
                    }
                    lastCommandTime = currentTime;
                    commandState = 3;
                    break;
                case 3: // Wait 4 seconds before restarting
                    if (currentTime - lastCommandTime >= 4000) {
                        commandState = 0;
                        lastCommandTime = currentTime;
                    }
                    break;
            }

            switch (state) {
                case 1: // Move left for 1000ms
                    mc.options.leftKey.setPressed(true);
                    if (currentTime - lastMoveTime >= 1000) {
                        mc.options.leftKey.setPressed(false);
                        lastMoveTime = currentTime;
                        state = 2;
                    }
                    break;
                case 2: // Wait 20000ms
                    if (currentTime - lastMoveTime >= 20000) {
                        lastMoveTime = currentTime;
                        state = 3;
                    }
                    break;
                case 3: // Move right for 1000ms
                    mc.options.rightKey.setPressed(true);
                    if (currentTime - lastMoveTime >= 1000) {
                        mc.options.rightKey.setPressed(false);
                        lastMoveTime = currentTime;
                        state = 1; // Restart move sequence
                    }
                    break;
            }
        });
    }

    private void releaseKeys(MinecraftClient client) {
        client.options.useKey.setPressed(false);
        client.options.leftKey.setPressed(false);
        client.options.rightKey.setPressed(false);
    }
}
