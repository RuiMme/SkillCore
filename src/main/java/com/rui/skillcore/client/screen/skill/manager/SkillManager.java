package com.rui.skillcore.client.screen.skill.manager;

import com.rui.skillcore.api.capability.skill.SkillProvider;
import com.rui.skillcore.data.SkillCategoryInfoData;
import com.rui.skillcore.data.SkillData;
import com.rui.skillcore.data.SkillDataLoader;
import com.rui.skillcore.client.screen.skill.nodes.SkillNode;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.*;

public class SkillManager {
    // 外层 Key 是 Category (例如 "basic", "element")
    public static Map<String, Map<ResourceLocation, SkillNode>> CATEGORIZED_NODES = new LinkedHashMap<>();
    public static Map<String, SkillCategoryInfoData> CATEGORY_DATA = new LinkedHashMap<>();

    // 获取特定分类下的所有节点
    public static Map<ResourceLocation, SkillNode> getNodesByCategory(String category) {
        return CATEGORIZED_NODES.getOrDefault(category, Collections.emptyMap());
    }

    // 获取所有分类的名称，用于动态生成 Tab
    public static Set<String> getCategories() {
        return CATEGORIZED_NODES.keySet();
    }

    public static SkillCategoryInfoData getCategoryData(String category) {
        return CATEGORY_DATA.get(category);
    }

    public static boolean isActiveSkill(LivingEntity player, String category, String id) {
        Map<String, Boolean> activeMap = new HashMap<>();
        player.getCapability(SkillProvider.SKILL_CAP).ifPresent(cap -> activeMap.put("active", cap.isActive(category, new ResourceLocation(id))));
        return activeMap.getOrDefault("active", false);
    }

    public static boolean isCooldownSkill(LivingEntity player, String category, String id, long currentTime, long cooldowns) {
        Map<String, Boolean> cooldownMap = new HashMap<>();
        player.getCapability(SkillProvider.SKILL_CAP).ifPresent(cap -> cooldownMap.put("cooldown", cap.isCooldown(category, new ResourceLocation(id), currentTime, cooldowns)));
        return cooldownMap.getOrDefault("cooldown", false);
    }

    /**
     * 【核心通用过滤器】
     * 获取当前玩家身上所有【真正生效】的技能 ID 集合（已处理断桥依赖和高阶屏蔽）
     */
    public static Set<String> getEnabledSkillIds(LivingEntity player) {
        Set<String> validSkillIds = new HashSet<>();

        // ====== 找出所有本身开启且满足依赖的技能 ======
        for (SkillData data : SkillDataLoader.RAW_DATA.values()) {
            if (isEffectValid(player, data.category, data.id)) {
                validSkillIds.add(data.id);
            }
        }

        // ====== 找出所有需要被吞掉的低阶技能 ======
        Set<String> suppressedSkillIds = new HashSet<>();
        for (String validId : validSkillIds) {
            SkillData data = SkillDataLoader.RAW_DATA.get(new ResourceLocation(validId));
            if (data != null && data.overrides != null) {
                suppressedSkillIds.addAll(data.overrides);
            }
        }

        // ====== 直接利用集合操作,移除所有黑名单技能 ======
        validSkillIds.removeAll(suppressedSkillIds);

        return validSkillIds; // 返回的就是绝对干净、可以直接执行的技能ID池
    }

    /**
     * 递归检查战斗效果依赖 (只看 JSON 中的 effectDependencies)
     */
    private static boolean isEffectValid(LivingEntity player, String category, String skillIdStr) {
        if (!isActiveSkill(player, category, skillIdStr)) {
            return false;
        }
        SkillData data = SkillDataLoader.RAW_DATA.get(new ResourceLocation(skillIdStr));
        if (data == null) return false;

        if (data.dependencies != null && !data.dependencies.isEmpty()) {
            for (String depId : data.dependencies) {
                if (!isEffectValid(player, category, depId)) {
                    return false;
                }
            }
        }
        return true;
    }
}
