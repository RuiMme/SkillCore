package com.rui.skillcore.api.capability.skill;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Set;

public interface ISkillData {
    Map<String, Set<ResourceLocation>> getUnlockedSkills();

    void unlockSkill(String id, ResourceLocation skillId);

    boolean hasSkill(String id, ResourceLocation skillId);

    Map<String, Set<ResourceLocation>> getActiveSkills();

    boolean isActive(String id, ResourceLocation skillId);

    void activeSkill(String id, ResourceLocation skillId);

    void offSkill(String id, ResourceLocation skillId);

    Map<String, Map<ResourceLocation, Long>> getSkillCooldownGameTime();
    Map<String, Map<ResourceLocation, Long>> getSkillCooldown();

    void setSkillCooldownGameTime(String id, ResourceLocation skillId, Long gameTime);
    void setSkillCooldown(String id, ResourceLocation skillId, Long cooldown);

    boolean isCooldown(String id, ResourceLocation skillId, long currentTime, long cooldowns);

    void clear(); // 用于重置或者死亡惩罚

    void clearUnlockedSkills(); // 用于重置或者死亡惩罚

    void clearActiveSkills(); // 用于重置或者死亡惩罚

    void clearCooldownGameTime(); // 用于重置或者死亡惩罚
    void clearCooldown(); // 用于重置或者死亡惩罚
}
