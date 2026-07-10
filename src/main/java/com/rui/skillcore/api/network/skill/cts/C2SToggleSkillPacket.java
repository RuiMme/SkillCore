package com.rui.skillcore.api.network.skill.cts;

import com.rui.skillcore.api.capability.skill.SkillProvider;
import com.rui.skillcore.api.network.PacketHandler;
import com.rui.skillcore.api.network.skill.stc.S2CSyncActiveSkillsPacket;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SToggleSkillPacket {
    private final String id;
    private final ResourceLocation skillId;

    public C2SToggleSkillPacket(String id, ResourceLocation skillId) {
        this.id = id;
        this.skillId = skillId;
    }

    // 解码：将服务器收到的字节流转为对象
    public C2SToggleSkillPacket(PacketBuffer buf) {
        this.id = buf.readUtf();
        this.skillId = buf.readResourceLocation();
    }

    // 编码：将对象转为字节流发给服务器
    public void encode(PacketBuffer buf) {
        buf.writeUtf(this.id);
        buf.writeResourceLocation(this.skillId);
    }


    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player != null) {
                player.getCapability(SkillProvider.SKILL_CAP).ifPresent(cap -> {
                    if(cap.hasSkill(this.id, this.skillId)) {
                        if(cap.isActive(this.id, this.skillId)) {
                            cap.offSkill(this.id, this.skillId);
                        } else {
                            cap.activeSkill(this.id, this.skillId);
                        }
                        PacketHandler.sendToClient(player, new S2CSyncActiveSkillsPacket(cap.getActiveSkills()));
                    }
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
