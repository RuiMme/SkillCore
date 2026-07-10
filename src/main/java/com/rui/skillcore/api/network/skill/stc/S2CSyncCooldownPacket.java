package com.rui.skillcore.api.network.skill.stc;

import com.rui.skillcore.api.capability.skill.SkillProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class S2CSyncCooldownPacket {
    // 我们的嵌套 Map 数据
    private final Map<String, Map<ResourceLocation, Long>> cooldown;

    public S2CSyncCooldownPacket(Map<String, Map<ResourceLocation, Long>> cooldown) {
        this.cooldown = cooldown;
    }


    // --- 解码：从缓冲区读回 Map ---
    public S2CSyncCooldownPacket(PacketBuffer buf) {
        this.cooldown = new HashMap<>();
        int categorySize = buf.readVarInt();
        for (int i = 0; i < categorySize; i++) {
            String category = buf.readUtf();
            int skillSize = buf.readVarInt();
            Map<ResourceLocation, Long> skillMap = new HashMap<>();
            for (int j = 0; j < skillSize; j++) {
                ResourceLocation id = buf.readResourceLocation();
                long time = buf.readLong();
                skillMap.put(id, time);
            }
            this.cooldown.put(category, skillMap);
        }
    }

    // --- 编码：将 Map 写入缓冲区 ---
    public void encode(PacketBuffer buf) {
        buf.writeVarInt(this.cooldown.size()); // 写入分类数量
        this.cooldown.forEach((category, skillMap) -> {
            buf.writeUtf(category); // 写入 String 分类
            buf.writeVarInt(skillMap.size()); // 写入该分类下的技能数量
            skillMap.forEach((id, time) -> {
                buf.writeResourceLocation(id); // 写入技能 ID
                buf.writeLong(time); // 写入 Long 时间戳
            });
        });
    }

    // --- 执行：数据到达客户端后的逻辑 ---
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 注意：这里必须在主线程执行
            // 获取客户端玩家并更新其 Capability
            ClientPlayerEntity player = Minecraft.getInstance().player;
            if (player != null) {
                player.getCapability(SkillProvider.SKILL_CAP).ifPresent(cap -> {
                    cap.clearCooldown();
                    // 假设你的 Capability 里有一个专门设置整个 Map 的方法
                    this.cooldown.forEach((key, set) -> {
                        set.forEach((key1, set1) -> {
                            cap.setSkillCooldown(key, key1, set1);
                        });
                    });
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
