package com.rui.skillcore.common.event;

import com.rui.skillcore.common.command.SkillCommand;
import com.rui.skillcore.libs.LibMisc;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LibMisc.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommandEvent {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        // 将调度器传入我们刚刚写的类中
        SkillCommand.register(event.getDispatcher());
    }
}