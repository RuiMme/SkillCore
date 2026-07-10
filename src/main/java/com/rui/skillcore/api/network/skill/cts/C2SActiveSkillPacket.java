package com.rui.skillcore.api.network.skill.cts;

import com.rui.skillcore.api.capability.skill.SkillProvider;
import com.rui.skillcore.api.network.PacketHandler;
import com.rui.skillcore.api.network.skill.stc.S2CSyncActiveSkillsPacket;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SActiveSkillPacket {
    private final String id;
    private final ResourceLocation skillId;

    public C2SActiveSkillPacket(String id, ResourceLocation skillId) {
        this.id = id;
        this.skillId = skillId;
    }

    public C2SActiveSkillPacket(PacketBuffer buf) {
        this.id = buf.readUtf();
        this.skillId = buf.readResourceLocation();
    }

    public void encode(PacketBuffer buf) {
        buf.writeUtf(this.id);
        buf.writeResourceLocation(this.skillId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player != null) {
                player.getCapability(SkillProvider.SKILL_CAP).ifPresent(cap -> {
                    cap.activeSkill(this.id, this.skillId);
                    PacketHandler.sendToClient(player, new S2CSyncActiveSkillsPacket(cap.getActiveSkills()));
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
