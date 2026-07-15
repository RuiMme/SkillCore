package com.rui.skillcore.common.event;

import com.rui.skillcore.data.SkillDataLoader;
import com.rui.skillcore.libs.LibMisc;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LibMisc.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class JsonEvent {
    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new SkillDataLoader());
    }
}
