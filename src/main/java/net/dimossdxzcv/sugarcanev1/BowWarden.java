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
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.text.Style;

@Environment(EnvType.CLIENT)
public class BowWarden implements ClientModInitializer {
    private static final KeyBinding toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.BowWarden.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "category.BowWarden"));

    private static boolean isRunning = false;
    private long lastRightClickTime = 0;
    private long lastMoveTime = 0;
    private long lastCommandTime = 0;
    private long deathTime = -1;
    private int commandState = 0;
    private int state = 1;
    private boolean wasDead = false;
    private static String warpCommand = "home warden";

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (toggleKey.wasPressed()) {
                isRunning = !isRunning;
                if (client.player != null) {
                    Text message = Text.literal("AutoAgility " + (isRunning ? "enabled" : "disabled"))
                            .setStyle(Style.EMPTY.withColor(isRunning ? 0x00FF00 : 0xFF0000));
                    client.player.sendMessage(message, false);
                };
                if (!isRunning) {
                    releaseKeys(client);
                } else {
                    lastRightClickTime = System.currentTimeMillis();
                    lastMoveTime = System.currentTimeMillis();
                    lastCommandTime = System.currentTimeMillis();
                    deathTime = -1;
                    wasDead = false;
                    commandState = 0;
                    state = 1;
                }
            }

            if (!isRunning) return;

            long currentTime = System.currentTimeMillis();
            MinecraftClient mc = MinecraftClient.getInstance();

            // Handle auto respawn and warp command with a proper death check
            if (mc.player != null) {
                if (mc.player.isDead()) {
                    if (!wasDead) {
                        deathTime = currentTime; // Set the time of death
                        wasDead = true;
                    }
                } else {
                    wasDead = false;
                }

                if (deathTime != -1 && (currentTime - deathTime) >= 5000) {
                    mc.player.networkHandler.sendChatCommand(warpCommand);
                    deathTime = -1; // Reset death time
                    wasDead = false; // Reset death flag
                }
            }

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
                case 0:
                    if (currentTime - lastCommandTime >= 120000) {
                        if (mc.player != null) {
                            mc.player.networkHandler.sendChatCommand("hub");
                        }
                        lastCommandTime = currentTime;
                        commandState = 1;
                    }
                    break;
                case 1:
                    if (currentTime - lastCommandTime >= 5000) {
                        commandState = 2;
                    }
                    break;
                case 2:
                    if (mc.player != null) {
                        mc.player.networkHandler.sendChatCommand("server survival");
                    }
                    lastCommandTime = currentTime;
                    commandState = 3;
                    break;
                case 3:
                    if (currentTime - lastCommandTime >= 4000) {
                        commandState = 0;
                        lastCommandTime = currentTime;
                    }
                    break;
            }

            // Movement sequence
            switch (state) {
                case 1:
                    mc.options.leftKey.setPressed(true);
                    if (currentTime - lastMoveTime >= 1000) {
                        mc.options.leftKey.setPressed(false);
                        lastMoveTime = currentTime;
                        state = 2;
                    }
                    break;
                case 2:
                    if (currentTime - lastMoveTime >= 20000) {
                        lastMoveTime = currentTime;
                        state = 3;
                    }
                    break;
                case 3:
                    mc.options.rightKey.setPressed(true);
                    if (currentTime - lastMoveTime >= 1000) {
                        mc.options.rightKey.setPressed(false);
                        lastMoveTime = currentTime;
                        state = 1;
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

    public static void setWarpCommand(String newCommand) {
        warpCommand = newCommand;
    }

    public static String getWarpCommand() {
        return warpCommand;
    }
}
