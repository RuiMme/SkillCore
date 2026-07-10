package com.rui.skillcore.api.network.skill.cts;

import com.rui.skillcore.api.capability.skill.SkillProvider;
import com.rui.skillcore.api.network.PacketHandler;
import com.rui.skillcore.api.network.skill.stc.S2CSyncCooldownPacket;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SCooldownPacket {
    private final String id;
    private final ResourceLocation skillId;
    private final long cooldown;

    public C2SCooldownPacket(String id, ResourceLocation skillId, long cooldown) {
        this.id = id;
        this.skillId = skillId;
        this.cooldown = cooldown;
    }

    public C2SCooldownPacket(PacketBuffer buf) {
        this.id = buf.readUtf();
        this.skillId = buf.readResourceLocation();
        this.cooldown = buf.readLong();
    }

    public void encode(PacketBuffer buf) {
        buf.writeUtf(this.id);
        buf.writeResourceLocation(this.skillId);
        buf.writeLong(this.cooldown);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player != null) {
                player.getCapability(SkillProvider.SKILL_CAP).ifPresent(cap -> {
                    cap.setSkillCooldown(this.id, this.skillId, this.cooldown);
                    PacketHandler.sendToClient(player, new S2CSyncCooldownPacket(cap.getSkillCooldown()));
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
