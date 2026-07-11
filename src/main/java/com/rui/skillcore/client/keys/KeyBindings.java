package com.rui.skillcore.client.keys;

import com.rui.skillcore.libs.LibMisc;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class KeyBindings extends KeyBinding {
    public static final List<KeyBinding> KEY_BINDINGS = new ArrayList<KeyBinding>();

    public static final KeyBinding OPEN_SKILL_SCREEN = new KeyBindings("open_skill_screen", KeyConflictContext.IN_GAME, KeyModifier.NONE, GLFW.GLFW_KEY_K, "key.category.skillcore");

    public KeyBindings(String description, IKeyConflictContext keyConflictContext, KeyModifier keyModifier, int keyCode, String category) {
        super(String.format("key.%s.%s", LibMisc.MOD_ID, description), keyConflictContext, keyModifier, InputMappings.Type.KEYSYM, keyCode, category);
        KEY_BINDINGS.add(this);
    }
}
