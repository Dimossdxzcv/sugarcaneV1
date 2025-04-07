package net.dimossdxzcv.sugarcanev1;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ModMenuScreen extends Screen {
    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_HEIGHT = 20;
    private static final int PADDING = 10;

    private List<ModModule> modules;
    private ModModule selectedModule = null;
    private boolean changingKeybind = false;

    public ModMenuScreen() {
        super(Text.literal("SugarcaneV1 Controls"));
        this.modules = new ArrayList<>();

        // Add all mod modules
        this.modules.add(new ModModule("Sugarcane Farm", SugarcaneV1client.isRunning(), SugarcaneV1client.getToggleKey()));
        this.modules.add(new ModModule("Bow Warden", BowWarden.isRunning(), BowWarden.getToggleKey()));
        this.modules.add(new ModModule("Auto Agility", AutoAgility.isRunning(), AutoAgility.getToggleKey()));
        this.modules.add(new ModModule("Auto Fishing", AutoFishing.isRunning(), AutoFishing.getToggleKey()));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = 60;

        // Add module toggle buttons
        for (int i = 0; i < modules.size(); i++) {
            ModModule module = modules.get(i);
            int yPos = startY + i * (BUTTON_HEIGHT + PADDING);

            // Status toggle button
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal(module.getName() + ": " + (module.isEnabled() ? "ON" : "OFF")),
                    button -> {
                        module.toggleEnabled();
                        button.setMessage(Text.literal(module.getName() + ": " + (module.isEnabled() ? "ON" : "OFF")));

                        // Call actual toggle methods in respective classes
                        if (module.getName().equals("Sugarcane Farm")) {
                            SugarcaneV1client.toggle();
                        } else if (module.getName().equals("Bow Warden")) {
                            BowWarden.toggle();
                        } else if (module.getName().equals("Auto Agility")) {
                            AutoAgility.toggle();
                        } else if (module.getName().equals("Auto Fishing")) {
                            AutoFishing.toggle();
                        }
                    }
            ).dimensions(centerX - BUTTON_WIDTH - 5, yPos, BUTTON_WIDTH, BUTTON_HEIGHT).build());

            // Keybind button
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("Keybind: " + InputUtil.Type.KEYSYM.createFromCode(module.getKeyBinding().getDefaultKey().getCode()).getLocalizedText().getString()),
                    button -> {
                        selectedModule = module;
                        changingKeybind = true;
                        button.setMessage(Text.literal("Press any key..."));
                    }
            ).dimensions(centerX + 5, yPos, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        }

        // Close button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Close"),
                button -> this.close()
        ).dimensions(centerX - 50, this.height - 40, 100, BUTTON_HEIGHT).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);

        // Additional instructions
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Toggle modules or change keybindings"), this.width / 2, 40, 0xAAAAAA);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (changingKeybind && selectedModule != null) {
            // Don't allow escape key as a keybind
            if (keyCode != GLFW.GLFW_KEY_ESCAPE) {
                // Update keybinding
                selectedModule.getKeyBinding().setBoundKey(InputUtil.Type.KEYSYM.createFromCode(keyCode));
                changingKeybind = false;
                selectedModule = null;
                this.init(); // Refresh the screen
            } else {
                // Cancel keybind change
                changingKeybind = false;
                selectedModule = null;
                this.init(); // Refresh the screen
            }
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !changingKeybind;
    }

    // Helper class to store module info
    private static class ModModule {
        private String name;
        private boolean enabled;
        private KeyBinding keyBinding;

        public ModModule(String name, boolean enabled, KeyBinding keyBinding) {
            this.name = name;
            this.enabled = enabled;
            this.keyBinding = keyBinding;
        }

        public String getName() {
            return name;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void toggleEnabled() {
            this.enabled = !this.enabled;
        }

        public KeyBinding getKeyBinding() {
            return keyBinding;
        }
    }
}