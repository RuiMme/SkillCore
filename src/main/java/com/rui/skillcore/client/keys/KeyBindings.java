package com.rui.skillcore.client.keys;

import com.mojang.blaze3d.platform.InputConstants;
import com.rui.skillcore.libs.LibMisc;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class KeyBindings extends KeyMapping {
    public static final List<KeyMapping> KEY_BINDINGS = new ArrayList<KeyMapping>();

    public static final KeyMapping OPEN_SKILL_SCREEN = new KeyBindings("open_skill_screen", KeyConflictContext.IN_GAME, KeyModifier.NONE, GLFW.GLFW_KEY_K, "key.category.skillcore");
    public static final KeyMapping OPEN_SKILL_CONFIG_SCREEN = new KeyBindings("open_skill_config_screen", KeyConflictContext.IN_GAME, KeyModifier.NONE, GLFW.GLFW_KEY_J, "key.category.skillcore");
    public static final KeyMapping OPEN_KEYBIND_SCREEN = new KeyBindings("open_keybind_screen", KeyConflictContext.IN_GAME, KeyModifier.NONE, GLFW.GLFW_KEY_H, "key.category.skillcore");


    public KeyBindings(String description, IKeyConflictContext keyConflictContext, KeyModifier keyModifier, int keyCode, String category) {
        super(String.format("key.%s.%s", LibMisc.MOD_ID, description), keyConflictContext, keyModifier, InputConstants.Type.KEYSYM, keyCode, category);
        KEY_BINDINGS.add(this);
    }
}
