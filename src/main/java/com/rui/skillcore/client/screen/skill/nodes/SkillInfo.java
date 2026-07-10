package com.rui.skillcore.client.screen.skill.nodes;

import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkillInfo {
    private final ResourceLocation id;
    private final List<ResourceLocation> parentIds; // 前置技能ID,没有则为null
    private final int levelRequirement;
    private final Map<Item, Integer> cost = new HashMap<>();
    private final long cooldown;
    private final List<ResourceLocation> exclusiveIds; // 互斥技能的ID列表

    public SkillInfo(ResourceLocation id, List<ResourceLocation> parentIds, int levelRequirement, Map<Item, Integer> cost, long cooldown, List<ResourceLocation> exclusiveIds) {
        this.id = id;
        this.parentIds = parentIds;
        this.levelRequirement = levelRequirement;
        this.cost.putAll(cost);
        this.cooldown = cooldown;
        this.exclusiveIds = exclusiveIds;
    }

    public ResourceLocation getId() { return id; }

    public List<ResourceLocation> getParentIds() {
        return parentIds;
    }

    public int getLevelRequirement() { return levelRequirement; }

    public Map<Item, Integer> getCost() {
        return cost;
    }

    public long getCooldown() {
        return cooldown;
    }

    public List<ResourceLocation> getExclusiveIds() {
        return exclusiveIds;
    }
}
