package com.rui.skillcore.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.rui.skillcore.api.capability.skill.SkillProvider;
import com.rui.skillcore.api.network.PacketHandler;
import com.rui.skillcore.api.network.skill.stc.S2CSyncSkillsPacket;
import com.rui.skillcore.client.screen.skill.manager.SkillManager;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.command.arguments.ResourceLocationArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

import java.util.Collection;
import java.util.Collections;

public class SkillCommand {
    // 获取所有分类
    private static final SuggestionProvider<CommandSource> CATEGORY_SUGGESTIONS = (ctx, builder) -> {
        return net.minecraft.command.ISuggestionProvider.suggest(SkillManager.CATEGORIZED_NODES.keySet(), builder);
    };
    // 获取指定分类下的所有技能ID
    private static final SuggestionProvider<CommandSource> SKILL_ID_SUGGESTIONS = (ctx, builder) -> {
        String category = StringArgumentType.getString(ctx, "category");
        if (SkillManager.CATEGORIZED_NODES.containsKey(category)) {
            // 将 ResourceLocation 转为 String 字符串供补全使用
            return net.minecraft.command.ISuggestionProvider.suggest(
                    SkillManager.CATEGORIZED_NODES.get(category).keySet().stream().map(ResourceLocation::toString),
                    builder
            );
        }
        return builder.buildFuture();
    };


    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(
                Commands.literal("skill")
                        .requires(source -> source.hasPermission(2)) // 要求 OP 权限 (权限等级 2)
                        // 子命令：/skill unlock
                        .then(Commands.literal("unlock")
                                // ==========================================
                                // 分支 A：/skill unlock all [<targets>]
                                // ==========================================
                                .then(Commands.literal("all")
                                        // 不带玩家参数（默认给执行者自己解锁）
                                        .executes(context -> unlockAll(
                                                context.getSource(),
                                                Collections.singleton(context.getSource().getPlayerOrException())
                                        ))
                                        // 带玩家参数（可以选中多个玩家，例如 @a）
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(context -> unlockAll(
                                                        context.getSource(),
                                                        EntityArgument.getPlayers(context, "targets")
                                                ))
                                        )
                                )

                                // ==========================================
                                // 分支 B：/skill unlock specific <category> <skillId> [<targets>]
                                // ==========================================
                                .then(Commands.literal("specific")
                                        .then(Commands.argument("category", StringArgumentType.word())
                                                .suggests(CATEGORY_SUGGESTIONS)
                                                .then(Commands.argument("skillId", ResourceLocationArgument.id())
                                                        .suggests(SKILL_ID_SUGGESTIONS)
                                                        // 不带玩家参数
                                                        .executes(context -> unlockSpecific(
                                                                context.getSource(),
                                                                Collections.singleton(context.getSource().getPlayerOrException()),
                                                                StringArgumentType.getString(context, "category"),
                                                                ResourceLocationArgument.getId(context, "skillId")
                                                        ))
                                                        // 带玩家参数
                                                        .then(Commands.argument("targets", EntityArgument.players())
                                                                .executes(context -> unlockSpecific(
                                                                        context.getSource(),
                                                                        EntityArgument.getPlayers(context, "targets"),
                                                                        StringArgumentType.getString(context, "category"),
                                                                        ResourceLocationArgument.getId(context, "skillId")
                                                                ))
                                                        )
                                                )
                                        )
                                )
                        )
        );
    }

    /**
     * 解锁指定玩家的所有技能
     */
    private static int unlockAll(CommandSource source, Collection<ServerPlayerEntity> targets) {
        for (ServerPlayerEntity target : targets) {
            target.getCapability(SkillProvider.SKILL_CAP).ifPresent(cap -> {
                // 遍历你缓存的所有技能类别和 ID 并解锁
                SkillManager.CATEGORIZED_NODES.forEach((category, nodeMap) -> {
                    for (ResourceLocation skillId : nodeMap.keySet()) {
                        cap.unlockSkill(category, skillId);
                    }
                });

                // 解锁完必须向该玩家发送网络包更新 UI！
                PacketHandler.sendToClient(target, new S2CSyncSkillsPacket(cap.getUnlockedSkills()));
                source.sendSuccess(new StringTextComponent("已解锁玩家 " + target.getScoreboardName() + " 的所有技能！").withStyle(TextFormatting.GREEN), true);
            });
        }
        return targets.size(); // 返回成功影响的实体数量
    }

    /**
     * 解锁指定玩家的特定技能
     */
    private static int unlockSpecific(CommandSource source, Collection<ServerPlayerEntity> targets, String category, ResourceLocation skillId) {
        for (ServerPlayerEntity target : targets) {
            target.getCapability(SkillProvider.SKILL_CAP).ifPresent(cap -> {
                // 容错：检查该技能是否真的存在
                if (!SkillManager.CATEGORIZED_NODES.containsKey(category) || !SkillManager.CATEGORIZED_NODES.get(category).containsKey(skillId)) {
                    source.sendFailure(new StringTextComponent("未找到指定技能: " + category + " - " + skillId).withStyle(TextFormatting.RED));
                    return; // 跳过当前玩家
                }
                cap.unlockSkill(category, skillId);
                // 同步数据给客户端
                PacketHandler.sendToClient(target, new S2CSyncSkillsPacket(cap.getUnlockedSkills()));

                source.sendSuccess(new StringTextComponent("已为玩家 " + target.getScoreboardName() + " 解锁技能: " + skillId.toString()).withStyle(TextFormatting.GREEN), true);
            });
        }
        return targets.size();
    }
}