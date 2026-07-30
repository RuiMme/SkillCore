package com.rui.skillcore.client.event;

import com.rui.skillcore.client.keys.KeyBindings;
import com.rui.skillcore.libs.LibMisc;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LibMisc.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public static void onKeyRegister(RegisterKeyMappingsEvent event) {
        event.register(KeyBindings.OPEN_SKILL_SCREEN);
        event.register(KeyBindings.OPEN_SKILL_CONFIG_SCREEN);
        event.register(KeyBindings.OPEN_KEYBIND_SCREEN);
    }
}
