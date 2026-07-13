package com.rui.skillcore.api.network.skill.stc;

import com.rui.skillcore.api.capability.skill.SkillProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class S2CSyncActiveSkillsPacket {
    private final Map<String, Set<ResourceLocation>> activeSkills;

    public S2CSyncActiveSkillsPacket(Map<String, Set<ResourceLocation>> activeSkills) {
        this.activeSkills = activeSkills;
    }

    public S2CSyncActiveSkillsPacket(FriendlyByteBuf buf) {
        this.activeSkills = new HashMap<>();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            String key = buf.readUtf();
            int setSize = buf.readVarInt();
            Set<ResourceLocation> set = new HashSet<>();

            // 4. 循环读取 ResourceLocation 并存入 Set
            for (int j = 0; j < setSize; j++) {
                set.add(buf.readResourceLocation());
            }

            // 5. 将解析好的 Set 放入 Map
            this.activeSkills.put(key, set);
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.activeSkills.size());
        this.activeSkills.forEach((key, set) -> {
            buf.writeUtf(key);
            buf.writeVarInt(set.size());
            for (ResourceLocation res : set) {
                buf.writeResourceLocation(res);
            }
        });
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                player.getCapability(SkillProvider.SKILL_CAP).ifPresent(cap -> {
                    cap.clearActiveSkills();
                    this.activeSkills.forEach((key, set) -> {
                        set.forEach(resource -> {
                            cap.activeSkill(key, resource);
                        });
                    });
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
