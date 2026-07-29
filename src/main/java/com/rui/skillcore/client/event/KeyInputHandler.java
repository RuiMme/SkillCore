package com.rui.skillcore.client.event;

import com.rui.skillcore.client.keys.KeyBindings;
import com.rui.skillcore.client.screen.skill.SkillScreen;
import com.rui.skillcore.client.screen.skillconfig.SkillSettingsScreen;
import com.rui.skillcore.client.screen.skillkeybind.KeybindSettingsScreen;
import com.rui.skillcore.libs.LibMisc;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LibMisc.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class KeyInputHandler {
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        // 确保在 Tick 的末尾执行，且玩家在游戏中
        if (event.phase == TickEvent.Phase.END) {
            // consumeClick() 会消耗掉一次按键点击，如果是按住不放，它也只会返回一次 true
            Minecraft mc = Minecraft.getInstance();
            if (KeyBindings.OPEN_SKILL_SCREEN.consumeClick()) {
                mc.tell(() -> mc.setScreen(new SkillScreen()));
            }
            if (KeyBindings.OPEN_SKILL_CONFIG_SCREEN.consumeClick()) {
                mc.tell(() -> mc.setScreen(new SkillSettingsScreen()));
            }
            if (KeyBindings.OPEN_KEYBIND_SCREEN.consumeClick()) {
                mc.tell(() -> mc.setScreen(new KeybindSettingsScreen(null)));
            }
        }
    }
}
