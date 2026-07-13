package com.rui.skillcore.client.event;

import com.rui.skillcore.client.keys.KeyBindings;
import com.rui.skillcore.client.screen.skill.SkillScreen;
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
            while (KeyBindings.OPEN_SKILL_SCREEN.consumeClick()) {
                Minecraft mc = Minecraft.getInstance();
                // 确保当前没有打开其他界面（比如聊天框或暂停菜单），并且玩家处于世界中
                if (mc.screen == null && mc.level != null) {
                    // 打开天赋/技能面板
                    mc.setScreen(new SkillScreen());
                }
            }
        }
    }
}
