package com.rui.skillcore.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.rui.skillcore.api.capability.skill.SkillProvider;
import com.rui.skillcore.api.network.PacketHandler;
import com.rui.skillcore.api.network.skill.stc.S2CSyncSkillsPacket;
import com.rui.skillcore.client.screen.skill.manager.SkillManager;
import com.rui.skillcore.data.SkillData;
import com.rui.skillcore.data.SkillDataLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SkillCommand {
    // 获取所有分类 (改为从双端通用的 CATEGORY_DATA 获取)
    private static final SuggestionProvider<CommandSourceStack> CATEGORY_SUGGESTIONS = (ctx, builder) -> {
        return SharedSuggestionProvider.suggest(SkillDataLoader.CATEGORY_DATA.keySet(), builder);
    };

    // 获取指定分类下的所有技能ID (改为从双端通用的 RAW_DATA 过滤)
    private static final SuggestionProvider<CommandSourceStack> SKILL_ID_SUGGESTIONS = (ctx, builder) -> {
        String category = StringArgumentType.getString(ctx, "category");

        // 过滤出该分类下的所有技能 ID 字符串
        List<String> skillIds = SkillDataLoader.RAW_DATA.values().stream()
                .filter(data -> category.equals(data.category))
                .map(data -> data.id)
                .collect(Collectors.toList());

        return SharedSuggestionProvider.suggest(skillIds, builder);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
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
    private static int unlockAll(CommandSourceStack source, Collection<ServerPlayer> targets) {
        for (ServerPlayer target : targets) {
            target.getCapability(SkillProvider.SKILL_CAP).ifPresent(cap -> {
                // 改为遍历 RAW_DATA 来进行解锁
                SkillDataLoader.RAW_DATA.values().forEach(data -> {
                    cap.unlockSkill(data.category, new ResourceLocation(data.id));
                });

                // 解锁完必须向该玩家发送网络包更新 UI！
                PacketHandler.sendToClient(target, new S2CSyncSkillsPacket(cap.getUnlockedSkills()));
                source.sendSuccess(Component.literal("已解锁玩家 " + target.getScoreboardName() + " 的所有技能！").withStyle(ChatFormatting.GREEN), true);
            });
        }
        return targets.size(); // 返回成功影响的实体数量
    }

    /**
     * 解锁指定玩家的特定技能
     */
    private static int unlockSpecific(CommandSourceStack source, Collection<ServerPlayer> targets, String category, ResourceLocation skillId) {
        for (ServerPlayer target : targets) {
            target.getCapability(SkillProvider.SKILL_CAP).ifPresent(cap -> {
                // 改为从 RAW_DATA 获取数据进行校验
                SkillData data = SkillDataLoader.RAW_DATA.get(skillId);

                // 容错：检查该技能是否存在，并且传入的 category 是否匹配
                if (data == null || !category.equals(data.category)) {
                    source.sendFailure(Component.literal("未找到指定技能: " + category + " - " + skillId).withStyle(ChatFormatting.RED));
                    return; // 跳过当前玩家
                }

                cap.unlockSkill(category, skillId);
                // 同步数据给客户端
                PacketHandler.sendToClient(target, new S2CSyncSkillsPacket(cap.getUnlockedSkills()));

                source.sendSuccess(Component.literal("已为玩家 " + target.getScoreboardName() + " 解锁技能: " + skillId.toString()).withStyle(ChatFormatting.GREEN), true);
            });
        }
        return targets.size();
    }
}