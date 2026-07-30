package com.rui.skillcore;

import com.rui.skillcore.api.capability.skill.ISkillData;
import com.rui.skillcore.api.capability.skill.SkillData;
import com.rui.skillcore.api.network.PacketHandler;
import com.rui.skillcore.client.keys.KeyBindings;
import com.rui.skillcore.client.keys.custom.KeybindConfigManager;
import com.rui.skillcore.libs.LibMisc;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(LibMisc.MOD_ID)
public class Main {
    public Main() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.register(this.getClass());
        bus.addListener(this::common);
        bus.addListener(this::client);

        PacketHandler.init();
    }

    private void common(FMLCommonSetupEvent event) {

    }

    private void client(FMLClientSetupEvent event) {
        event.enqueueWork(KeybindConfigManager::load);
    }
}
