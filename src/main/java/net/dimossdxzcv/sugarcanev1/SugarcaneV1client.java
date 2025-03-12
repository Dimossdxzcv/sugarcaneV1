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
public class SugarcaneV1client implements ClientModInitializer {
    private static final KeyBinding toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.mymod.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_N, "category.mymod"));

    private boolean isRunning = false;
    private long lastActionTime = 0;
    private long lastYawUpdateTime = 0;
    private long lastFeedUpdate = 0;
    private int state = 0; // 0 = breaking, 1 = moving left, 2 = moving right

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (toggleKey.wasPressed()) {
                isRunning = !isRunning;
                if (!isRunning) {
                    releaseKeys(client);
                    return;
                } else {
                    lastActionTime = System.currentTimeMillis();
                    lastYawUpdateTime = System.currentTimeMillis();
                    lastFeedUpdate = System.currentTimeMillis();
                    state = 0;
                    if (client.player != null) {
                        client.player.setPitch(0);
                    }
                }
            }

            if (!isRunning) return;

            long currentTime = System.currentTimeMillis();
            MinecraftClient mc = MinecraftClient.getInstance();

            // Always breaking blocks (even in title screen)
            mc.execute(() -> mc.options.attackKey.setPressed(true));

            if (client.player != null && currentTime - lastFeedUpdate >= 60000) {
                client.player.networkHandler.sendChatCommand("feed");
                lastFeedUpdate = currentTime;
            }

            if (client.player != null) {
                if (client.player.getPitch() != 0) {
                    client.player.setPitch(0);
                }

                if (currentTime - lastYawUpdateTime >= 4000) { // Modify pitch every 4 seconds
                    client.player.setYaw(client.player.getYaw() + 0.1f);
                    client.player.setPitch(client.player.getPitch() + 0.1f);
                    client.player.setYaw(client.player.getYaw() - 0.1f);
                    lastYawUpdateTime = currentTime;
                }

                switch (state) {
                    case 0: // Move Left
                        mc.options.leftKey.setPressed(true);
                        mc.options.rightKey.setPressed(false);
                        if (currentTime - lastActionTime >= 3500) {
                            mc.options.leftKey.setPressed(false);
                            state = 1;
                            lastActionTime = currentTime;
                        }
                        break;
                    case 1: // Move Right
                        mc.options.rightKey.setPressed(true);
                        mc.options.leftKey.setPressed(false);
                        if (currentTime - lastActionTime >= 3500) {
                            mc.options.rightKey.setPressed(false);
                            state = 0;
                            lastActionTime = currentTime;
                        }
                        break;
                }
            }
        });
    }

    private void releaseKeys(MinecraftClient client) {
        client.options.attackKey.setPressed(false);
        client.options.leftKey.setPressed(false);
        client.options.rightKey.setPressed(false);
    }
}
