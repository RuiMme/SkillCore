package com.rui.skillcore.api.network.skill.cts;

import com.rui.skillcore.api.capability.skill.SkillProvider;
import com.rui.skillcore.api.network.PacketHandler;
import com.rui.skillcore.api.network.skill.stc.S2CSyncActiveSkillsPacket;
import com.rui.skillcore.api.network.skill.stc.S2CSyncCooldownPacket;
import com.rui.skillcore.api.network.skill.stc.S2CSyncSkillsPacket;
import com.rui.skillcore.data.SkillData;
import com.rui.skillcore.data.SkillDataLoader;
import com.rui.skillcore.libs.util.InventoryUtil;
import com.rui.skillcore.libs.util.LevelUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class C2SUnlockSkillPacket {
    private final String category;
    private final ResourceLocation skillId;

    public C2SUnlockSkillPacket(String id, ResourceLocation skillId) {
        this.category = id;
        this.skillId = skillId;
    }

    public C2SUnlockSkillPacket(FriendlyByteBuf buf) {
        this.category = buf.readUtf();
        this.skillId = buf.readResourceLocation();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.category);
        buf.writeResourceLocation(this.skillId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                SkillData nodeData = SkillDataLoader.RAW_DATA.get(this.skillId);
                if(nodeData == null) {
                    System.out.println("[Skill Network] 服务端未找到技能数据: " + this.skillId);
                    return;
                }

                // 在服务端给玩家解锁技能
                player.getCapability(SkillProvider.SKILL_CAP).ifPresent(data -> {
                    Map<Item, Integer> map = new HashMap<>();

                    if (nodeData.exclusives != null) {
                        for (String exclusiveStr : nodeData.exclusives) {
                            ResourceLocation exclusiveId = new ResourceLocation(exclusiveStr);
                            // 如果玩家尝试解锁的技能与他已拥有的技能互斥，则直接拦截
                            if (data.hasSkill(nodeData.category, exclusiveId)) {
                                System.out.println("[Skill Network] 玩家尝试非法解锁被互斥封禁的技能: " + this.skillId);
                                return;
                            }
                        }
                    }

//                    SkillNode targetNode = SkillManager.CATEGORIZED_NODES.get(category).get(skillId);
//                    if(targetNode != null) {
//                        // 服务端防作弊安全网: 复用相同的逻辑校验
//                        if (!targetNode.isVisible(data, SkillManager.CATEGORIZED_NODES)) {
//                            // 玩家企图非法激活一条已经被互斥封禁的整条分枝,直接无视并拦截
//                            return;
//                        }
//                    }
                    // 这里可以加入前置条件判定、扣除经验或点数逻辑
                    if (data.hasSkill(nodeData.category, this.skillId)) return;
                    if (nodeData.parents != null) {
                        for (String parentStr : nodeData.parents) {
                            ResourceLocation parentId = new ResourceLocation(parentStr);
                            SkillData parentData = SkillDataLoader.RAW_DATA.get(parentId);

                            // 获取父级分类并判断是否解锁
                            if (parentData != null && !data.hasSkill(parentData.category, parentId)) {
                                player.sendMessage(new TextComponent("§c你必须先解锁前置技能！"), player.getUUID());
                                return;
                            }
                        }
                    }
                    if(!player.gameMode.isCreative()) {
                        if (player.experienceLevel < nodeData.levelRequirement) {
                            player.sendMessage(new TextComponent("§c你的等级不足！需要 " + nodeData.levelRequirement + " 级"), player.getUUID());
                            return;
                        }

                        if (nodeData.cost != null) {
                            for (Map.Entry<String, Integer> entry : nodeData.cost.entrySet()) {
                                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(entry.getKey()));
                                int count = entry.getValue();
                                int amount = InventoryUtil.getPlayerItemCount(player, item);

                                if (amount < count) {
                                    player.sendMessage(new TextComponent("§c材料不足！需要 " + count + " 个 " + item.getName(ItemStack.EMPTY).getString()), player.getUUID());
                                    return; // 物品不足直接中断
                                } else {
                                    map.put(item, count);
                                }
                            }
                        }

                        LevelUtil.consumePlayerLevel(player, nodeData.levelRequirement);
                        map.forEach((item, count) -> {
                            InventoryUtil.consumePlayerItem(player, item, count);
                        });
                    }
                    data.unlockSkill(nodeData.category, this.skillId);
                    data.activeSkill(nodeData.category, this.skillId);
                    data.setSkillCooldown(nodeData.category, this.skillId, nodeData.cooldown);
                    // 解锁成功后，同步最新数据给客户端
                    PacketHandler.sendToClient(player, new S2CSyncSkillsPacket(data.getUnlockedSkills()));
                    PacketHandler.sendToClient(player, new S2CSyncActiveSkillsPacket(data.getActiveSkills()));
                    PacketHandler.sendToClient(player, new S2CSyncCooldownPacket(data.getSkillCooldown()));
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
