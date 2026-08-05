package com.rui.skillcore.api.network;

import com.rui.skillcore.api.network.skill.SyncSkillsPacket;
import com.rui.skillcore.api.network.skill.cts.*;
import com.rui.skillcore.api.network.skill.stc.*;
import com.rui.skillcore.libs.LibMisc;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel HANDLER = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(LibMisc.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );
    public static void init() {
        int id = 0;
        HANDLER.registerMessage(id++, C2SUnlockSkillPacket.class, C2SUnlockSkillPacket::toBytes, C2SUnlockSkillPacket::new, C2SUnlockSkillPacket::handle);
        HANDLER.registerMessage(id++, C2SActiveSkillPacket.class, C2SActiveSkillPacket::encode, C2SActiveSkillPacket::new, C2SActiveSkillPacket::handle);
        HANDLER.registerMessage(id++, C2SToggleSkillPacket.class, C2SToggleSkillPacket::encode, C2SToggleSkillPacket::new, C2SToggleSkillPacket::handle);
        HANDLER.registerMessage(id++, C2SCooldownPacket.class, C2SCooldownPacket::encode, C2SCooldownPacket::new, C2SCooldownPacket::handle);

        HANDLER.registerMessage(id++, S2CSyncSkillsPacket.class, S2CSyncSkillsPacket::toBytes, S2CSyncSkillsPacket::decode, S2CSyncSkillsPacket::handle);
        HANDLER.registerMessage(id++, S2CSyncActiveSkillsPacket.class, S2CSyncActiveSkillsPacket::toBytes, S2CSyncActiveSkillsPacket::new, S2CSyncActiveSkillsPacket::handle);
        HANDLER.registerMessage(id++, S2CSyncCooldownPacket.class, S2CSyncCooldownPacket::encode, S2CSyncCooldownPacket::new, S2CSyncCooldownPacket::handle);
        HANDLER.registerMessage(id++, S2CSyncCooldownGameTimePacket.class, S2CSyncCooldownGameTimePacket::encode, S2CSyncCooldownGameTimePacket::new, S2CSyncCooldownGameTimePacket::handle);
        HANDLER.registerMessage(id++, SyncSkillsPacket.class, SyncSkillsPacket::encode, SyncSkillsPacket::new, SyncSkillsPacket::handle);
        HANDLER.registerMessage(id++, S2CSyncSkillDataPacket.class, S2CSyncSkillDataPacket::toBytes, S2CSyncSkillDataPacket::new, S2CSyncSkillDataPacket::handle);
    }

    public static void sendToServer(Object msg) {
        HANDLER.sendToServer(msg);
    }

    public static void sendToClient(ServerPlayer player, Object msg) {
        HANDLER.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }
}
