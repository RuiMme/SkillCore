package com.rui.skillcore.api.network.skill;

import com.rui.skillcore.api.capability.skill.SkillProvider;
import com.rui.skillcore.api.network.PacketHandler;
import com.rui.skillcore.api.network.skill.stc.S2CSyncActiveSkillsPacket;
import com.rui.skillcore.api.network.skill.stc.S2CSyncCooldownGameTimePacket;
import com.rui.skillcore.api.network.skill.stc.S2CSyncCooldownPacket;
import com.rui.skillcore.api.network.skill.stc.S2CSyncSkillsPacket;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncSkillsPacket {
    public SyncSkillsPacket() {
    }

    public SyncSkillsPacket(PacketBuffer buf) {
    }

    public void encode(PacketBuffer buf) {

    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player != null) {
                player.getCapability(SkillProvider.SKILL_CAP).ifPresent(cap -> {
                    PacketHandler.sendToClient(player, new S2CSyncSkillsPacket(cap.getUnlockedSkills()));
                    PacketHandler.sendToClient(player, new S2CSyncActiveSkillsPacket(cap.getActiveSkills()));
                    PacketHandler.sendToClient(player, new S2CSyncCooldownPacket(cap.getSkillCooldown()));
                    PacketHandler.sendToClient(player, new S2CSyncCooldownGameTimePacket(cap.getSkillCooldownGameTime()));
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
