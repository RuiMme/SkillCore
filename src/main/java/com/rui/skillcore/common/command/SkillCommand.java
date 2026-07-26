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
import java.util.List;
import java.util.stream.Collectors;

public class SkillCommand {
    // 获取所有分类
    private static final SuggestionProvider<CommandSource> CATEGORY_SUGGESTIONS = (ctx, builder) -> {
        return net.minecraft.command.ISuggestionProvider.suggest(SkillDataLoader.CATEGORY_DATA.keySet(), builder);
    };
    // 获取指定分类下的所有技能ID
    private static final SuggestionProvider<CommandSource> SKILL_ID_SUGGESTIONS = (ctx, builder) -> {
        String category = StringArgumentType.getString(ctx, "category");

        // 过滤出该分类下的所有技能 ID 字符串
        List<String> skillIds = SkillDataLoader.RAW_DATA.values().stream()
                .filter(data -> category.equals(data.category))
                .map(data -> data.id)
                .collect(Collectors.toList());

        return net.minecraft.command.ISuggestionProvider.suggest(skillIds, builder);
    };


    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(
                Commands.literal("skill")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("unlock")
                                .then(Commands.literal("all")
                                        .executes(context -> unlockAll(
                                                context.getSource(),
                                                Collections.singleton(context.getSource().getPlayerOrException())
                                        ))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(context -> unlockAll(
                                                        context.getSource(),
                                                        EntityArgument.getPlayers(context, "targets")
                                                ))
                                        )
                                )
                                .then(Commands.literal("specific")
                                        .then(Commands.argument("category", StringArgumentType.word())
                                                .suggests(CATEGORY_SUGGESTIONS)
                                                .then(Commands.argument("skillId", ResourceLocationArgument.id())
                                                        .suggests(SKILL_ID_SUGGESTIONS)
                                                        .executes(context -> unlockSpecific(
                                                                context.getSource(),
                                                                Collections.singleton(context.getSource().getPlayerOrException()),
                                                                StringArgumentType.getString(context, "category"),
                                                                ResourceLocationArgument.getId(context, "skillId")
                                                        ))
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
                // 改为遍历 RAW_DATA 来进行解锁
                SkillDataLoader.RAW_DATA.values().forEach(data -> {
                    cap.unlockSkill(data.category, new ResourceLocation(data.id));
                });

                PacketHandler.sendToClient(target, new S2CSyncSkillsPacket(cap.getUnlockedSkills()));
                source.sendSuccess(new StringTextComponent("已解锁玩家 " + target.getScoreboardName() + " 的所有技能！").withStyle(TextFormatting.GREEN), true);
            });
        }
        return targets.size();
    }

    /**
     * 解锁指定玩家的特定技能
     */
    private static int unlockSpecific(CommandSource source, Collection<ServerPlayerEntity> targets, String category, ResourceLocation skillId) {
        for (ServerPlayerEntity target : targets) {
            target.getCapability(SkillProvider.SKILL_CAP).ifPresent(cap -> {
                // 改为从 RAW_DATA 获取数据进行校验
                SkillData data = SkillDataLoader.RAW_DATA.get(skillId);

                // 检查技能是否存在且类别是否匹配
                if (data == null || !category.equals(data.category)) {
                    source.sendFailure(new StringTextComponent("未找到指定技能: " + category + " - " + skillId).withStyle(TextFormatting.RED));
                    return;
                }

                cap.unlockSkill(category, skillId);
                PacketHandler.sendToClient(target, new S2CSyncSkillsPacket(cap.getUnlockedSkills()));

                source.sendSuccess(new StringTextComponent("已为玩家 " + target.getScoreboardName() + " 解锁技能: " + skillId.toString()).withStyle(TextFormatting.GREEN), true);
            });
        }
        return targets.size();
    }
}