package com.rui.skillcore.client.event;

import com.rui.skillcore.client.keys.custom.CustomKeyBind;
import com.rui.skillcore.client.keys.custom.CustomKeybindRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputMappings;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ClientInputHandler {
    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        if (event.getAction() == GLFW.GLFW_PRESS) {
            int keyCode = event.getKey();

            // 获取当前修饰键按下状态
            boolean ctrl = Screen.hasControlDown();
            boolean shift = Screen.hasShiftDown();
            boolean alt = Screen.hasAltDown();

            // 检查所有的分类按键
            for (CustomKeybindRegistry.KeybindCategory category : CustomKeybindRegistry.CATEGORIES) {
                for (CustomKeyBind bind : category.binds) {
                    if (bind.matches(keyCode, ctrl, shift, alt)) {
                        bind.handlePress();
                    }
                }
            }
        }
    }

    private static boolean matches(CustomKeyBind bind, int keyCode, InputMappings.Type type) {
        InputMappings.Input input = bind.getKey();
        return input.getType() == type && input.getValue() == keyCode;
    }
}
