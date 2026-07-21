package com.rui.skillcore;

import com.rui.skillcore.api.network.PacketHandler;
import com.rui.skillcore.libs.LibMisc;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(LibMisc.MOD_ID)
public class Main {
    public Main() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.register(this.getClass());
        bus.addListener(this::common);

        PacketHandler.init();
    }

    private void common(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
//            CapabilityManager.INSTANCE.register(ISkillData.class, new SkillData.Storage(), SkillData::new);
        });
    }
}
