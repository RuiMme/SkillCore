package com.rui.skillcore.api.network.skill.stc;

import com.rui.skillcore.api.capability.skill.SkillProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class S2CSyncSkillsPacket {
    private final Map<String, Set<ResourceLocation>> unlockedSkills;

    public S2CSyncSkillsPacket(Map<String, Set<ResourceLocation>> unlockedSkills) {
        this.unlockedSkills = unlockedSkills;
    }

    public static S2CSyncSkillsPacket decode(PacketBuffer buf) {
        Map<String, Set<ResourceLocation>> map = new HashMap<>();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            String key = buf.readUtf();
            int setSize = buf.readVarInt();
            Set<ResourceLocation> set = new HashSet<>();
            for (int j = 0; j < setSize; j++) {
                set.add(buf.readResourceLocation());
            }
            map.put(key, set);
        }
        return new S2CSyncSkillsPacket(map);
    }

    public static void toBytes(S2CSyncSkillsPacket msg, PacketBuffer buf) {
        buf.writeInt(msg.unlockedSkills.size());
        msg.unlockedSkills.forEach((key, set) -> {
            buf.writeUtf(key);
            buf.writeVarInt(set.size());
            for(ResourceLocation res : set) {
                buf.writeResourceLocation(res);
            }
        });
    }

    public static void handle(S2CSyncSkillsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientPlayerEntity player = Minecraft.getInstance().player;
            if (player != null) {
                msg.unlockedSkills.values();
                player.getCapability(SkillProvider.SKILL_CAP).ifPresent(cap -> {
                    cap.clearUnlockedSkills();
                    msg.unlockedSkills.forEach((key, set) -> {
                        set.forEach(resource -> cap.unlockSkill(key, resource));
                    });
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
