package com.rui.skillcore.common.event;

import com.rui.skillcore.api.network.PacketHandler;
import com.rui.skillcore.api.network.skill.stc.S2CSyncSkillDataPacket;
import com.rui.skillcore.data.SkillDataLoader;
import com.rui.skillcore.libs.LibMisc;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import static com.rui.skillcore.common.event.CapabilityEvent.syncDataToClient;

@Mod.EventBusSubscriber(modid = LibMisc.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DataSyncHandler {
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        // 确保只在服务端处理逻辑
        if (!(event.getEntity() instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer player = (ServerPlayer) event.getEntity();
        syncDataToClient(player);
        // 创建同步数据包
        S2CSyncSkillDataPacket packet = new S2CSyncSkillDataPacket(SkillDataLoader.RAW_DATA, SkillDataLoader.CATEGORY_DATA);
        // 将数据精准发送给刚上线的玩家
        PacketHandler.HANDLER.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
