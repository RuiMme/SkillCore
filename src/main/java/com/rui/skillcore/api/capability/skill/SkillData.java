package com.rui.skillcore.api.capability.skill;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SkillData implements ISkillData {
    private final Map<String, Set<ResourceLocation>> unlockedSkills = new HashMap<>();
    private final Map<String, Set<ResourceLocation>> activeSkills = new HashMap<>();
    private final Map<String, Map<ResourceLocation, Long>> skillCooldownGameTime = new HashMap<>();
    private final Map<String, Map<ResourceLocation, Long>> skillCooldown = new HashMap<>();

    @Override
    public Map<String, Set<ResourceLocation>> getUnlockedSkills() {
        return this.unlockedSkills;
    }

    @Override
    public void unlockSkill(String id, ResourceLocation skillId) {
        if(!this.unlockedSkills.containsKey(id)) this.unlockedSkills.put(id, new HashSet<>());
        this.unlockedSkills.get(id).add(skillId);
    }

    @Override
    public boolean hasSkill(String id, ResourceLocation skillId) {
        if(!this.unlockedSkills.containsKey(id)) return false;
        return this.unlockedSkills.get(id).contains(skillId);
    }

    @Override
    public Map<String, Set<ResourceLocation>> getActiveSkills() {
        return this.activeSkills;
    }

    @Override
    public void activeSkill(String id, ResourceLocation skillId) {
        if(!this.activeSkills.containsKey(id)) this.activeSkills.put(id, new HashSet<>());
        this.activeSkills.get(id).add(skillId);
    }

    @Override
    public void offSkill(String id, ResourceLocation skillId) {
        if(this.activeSkills.containsKey(id)) {
            this.activeSkills.get(id).remove(skillId);
        }
    }

    @Override
    public boolean isActive(String id, ResourceLocation skillId) {
        if(!activeSkills.containsKey(id)) return false;
        return hasSkill(id, skillId) && activeSkills.get(id).contains(skillId);
    }

    @Override
    public Map<String, Map<ResourceLocation, Long>> getSkillCooldownGameTime() {
        return this.skillCooldownGameTime;
    }

    @Override
    public Map<String, Map<ResourceLocation, Long>> getSkillCooldown() {
        return this.skillCooldown;
    }

    @Override
    public void setSkillCooldownGameTime(String id, ResourceLocation skillId, Long gameTime) {
        this.skillCooldownGameTime.computeIfAbsent(id, k -> new HashMap<>()).put(skillId, gameTime);
    }

    @Override
    public void setSkillCooldown(String id, ResourceLocation skillId, Long cooldown) {
        this.skillCooldown.computeIfAbsent(id, k -> new HashMap<>()).put(skillId, cooldown);
    }

    @Override
    public boolean isCooldown(String id, ResourceLocation skillId, long currentTime, long cooldowns) {
        if(!this.skillCooldownGameTime.containsKey(id)) return false;
        Map<ResourceLocation, Long> skillCooldownGameTimeMap = this.skillCooldownGameTime.get(id);
        Map<ResourceLocation, Long> skillCooldownMap = this.skillCooldown.get(id);
        if(!skillCooldownGameTimeMap.containsKey(skillId) || !skillCooldownMap.containsKey(skillId)) return false;

        return currentTime - skillCooldownGameTimeMap.get(skillId) < skillCooldownMap.get(skillId);
    }

    @Override
    public void clear() {
        this.unlockedSkills.clear();
        this.activeSkills.clear();
        this.skillCooldownGameTime.clear();
        this.skillCooldown.clear();
    }

    @Override
    public void clearUnlockedSkills() {
        this.unlockedSkills.clear();
    }

    @Override
    public void clearActiveSkills() {
        this.activeSkills.clear();
    }

    @Override
    public void clearCooldownGameTime() {
        this.skillCooldownGameTime.clear();
    }

    @Override
    public void clearCooldown() {
        this.skillCooldown.clear();
    }

    // --- 1.18 全新 NBT 读写逻辑 (顶替了原本的 IStorage) ---
    public CompoundTag saveNBT() {
        CompoundTag tag = new CompoundTag();
        CompoundTag skillTag = new CompoundTag();
        CompoundTag activeTag = new CompoundTag();
        CompoundTag cooldownGameTimeTag = new CompoundTag();
        CompoundTag cooldownTag = new CompoundTag();

        for (Map.Entry<String, Set<ResourceLocation>> entry : this.getUnlockedSkills().entrySet()) {
            String id = entry.getKey();
            Set<ResourceLocation> skillIds = entry.getValue();
            ListTag skillList = new ListTag();
            for(ResourceLocation skillId : skillIds) {
                skillList.add(StringTag.valueOf(skillId.toString()));
            }
            skillTag.put(id, skillList);
        }

        for(Map.Entry<String, Set<ResourceLocation>> entry : this.getActiveSkills().entrySet()) {
            String id = entry.getKey();
            Set<ResourceLocation> activeIds = entry.getValue();
            ListTag activeList = new ListTag();
            for(ResourceLocation activeId : activeIds) {
                activeList.add(StringTag.valueOf(activeId.toString()));
            }
            activeTag.put(id, activeList);
        }

        this.getSkillCooldownGameTime().forEach((category, skillMap) -> {
            CompoundTag skillMapNbt = new CompoundTag();
            skillMap.forEach((id, time) -> {
                skillMapNbt.putLong(id.toString(), time);
            });
            cooldownGameTimeTag.put(category, skillMapNbt);
        });

        this.getSkillCooldown().forEach((category, skillMap) -> {
            CompoundTag skillMapNbt = new CompoundTag();
            skillMap.forEach((id, time) -> {
                skillMapNbt.putLong(id.toString(), time);
            });
            cooldownTag.put(category, skillMapNbt);
        });

        tag.put("UnlockedSkills", skillTag);
        tag.put("ActiveSkills", activeTag);
        tag.put("SkillCooldownGameTime", cooldownGameTimeTag);
        tag.put("SkillCooldown", cooldownTag);

        return tag;
    }

    public void loadNBT(CompoundTag tag) {
        this.clear();

        if(tag.contains("UnlockedSkills", Tag.TAG_COMPOUND)) {
            CompoundTag skillTag = tag.getCompound("UnlockedSkills");
            for(String id : skillTag.getAllKeys()) {
                ListTag list = skillTag.getList(id, Tag.TAG_STRING);
                for (int i = 0; i < list.size(); i++) {
                    this.unlockSkill(id, new ResourceLocation(list.getString(i)));
                }
            }
        }

        if(tag.contains("ActiveSkills", Tag.TAG_COMPOUND)) {
            CompoundTag activeTag = tag.getCompound("ActiveSkills");
            for(String id : activeTag.getAllKeys()) {
                ListTag list = activeTag.getList(id, Tag.TAG_STRING);
                for (int i = 0; i < list.size(); i++) {
                    this.activeSkill(id, new ResourceLocation(list.getString(i)));
                }
            }
        }

        if (tag.contains("SkillCooldownGameTime", Tag.TAG_COMPOUND)) {
            CompoundTag rootCdNbt = tag.getCompound("SkillCooldownGameTime");
            for (String category : rootCdNbt.getAllKeys()) {
                CompoundTag skillMapNbt = rootCdNbt.getCompound(category);
                for (String skillIdStr : skillMapNbt.getAllKeys()) {
                    this.setSkillCooldownGameTime(category, new ResourceLocation(skillIdStr), skillMapNbt.getLong(skillIdStr));
                }
            }
        }

        if (tag.contains("SkillCooldown", Tag.TAG_COMPOUND)) {
            CompoundTag rootCdNbt = tag.getCompound("SkillCooldown");
            for (String category : rootCdNbt.getAllKeys()) {
                CompoundTag skillMapNbt = rootCdNbt.getCompound(category);
                for (String skillIdStr : skillMapNbt.getAllKeys()) {
                    this.setSkillCooldown(category, new ResourceLocation(skillIdStr), skillMapNbt.getLong(skillIdStr));
                }
            }
        }
    }

//
//    // --- NBT 存储逻辑 (IStorage) ---
//    public static class Storage implements Capability.IStorage<ISkillData> {
//        @Nullable
//        @Override
//        public INBT writeNBT(Capability<ISkillData> capability, ISkillData instance, Direction side) {
//            ListNBT skillList = new ListNBT();
//            CompoundNBT tag = new CompoundNBT();
//            CompoundNBT skillTag = new CompoundNBT();
//            CompoundNBT activeTag = new CompoundNBT();
//            CompoundNBT cooldownGameTimeTag = new CompoundNBT();
//            CompoundNBT cooldownTag = new CompoundNBT();
//
//            for (Map.Entry<String, Set<ResourceLocation>> entry : instance.getUnlockedSkills().entrySet()) {
//                String id = entry.getKey();
//                Set<ResourceLocation> skillIds = entry.getValue();
//
//                for(ResourceLocation skillId : skillIds) {
//                    skillList.add(StringNBT.valueOf(skillId.toString()));
//                }
//
//                skillTag.put(id, skillList);
//            }
//
//            for(String key : instance.getActiveSkills().keySet()) {
//                ListNBT activeList = new ListNBT();
//                Set<ResourceLocation> activeIds = instance.getActiveSkills().get(key);
//                for(ResourceLocation activeId : activeIds) {
//                    activeList.add(StringNBT.valueOf(activeId.toString()));
//                }
//                activeTag.put(key, activeList);
//            }
//
//            instance.getSkillCooldownGameTime().forEach((category, skillMap) -> {
//                CompoundNBT skillMapNbt = new CompoundNBT();
//                skillMap.forEach((id, time) -> {
//                    skillMapNbt.putLong(id.toString(), time);
//                });
//                cooldownGameTimeTag.put(category, skillMapNbt);
//            });
//
//            instance.getSkillCooldown().forEach((category, skillMap) -> {
//                CompoundNBT skillMapNbt = new CompoundNBT();
//                skillMap.forEach((id, time) -> {
//                    skillMapNbt.putLong(id.toString(), time);
//                });
//                cooldownTag.put(category, skillMapNbt);
//            });
//
//            tag.put("UnlockedSkills", skillTag);
//            tag.put("ActiveSkills", activeTag);
//            tag.put("SkillCooldownGameTime", cooldownGameTimeTag);
//            tag.put("SkillCooldown", cooldownTag);
//
//            return tag;
//        }
//
//        @Override
//        public void readNBT(Capability<ISkillData> capability, ISkillData instance, Direction side, INBT nbt) {
//            if (nbt instanceof CompoundNBT) {
//                CompoundNBT tag = (CompoundNBT) nbt;
//                instance.clear();
//
//                if(tag.contains("UnlockedSkills", 10)) {
//                    CompoundNBT skillTag = tag.getCompound("UnlockedSkills");
//                    for(String id : skillTag.getAllKeys()) {
//                        ListNBT list = skillTag.getList(id, 8);
//                        for (int i = 0; i < list.size(); i++) {
//                            instance.unlockSkill(id, new ResourceLocation(list.getString(i)));
//                        }
//                    }
//                }
//
//                if(tag.contains("ActiveSkills", 10)) {
//                    CompoundNBT activeTag = tag.getCompound("ActiveSkills");
//                    for(String id : activeTag.getAllKeys()) {
//                        ListNBT list = activeTag.getList(id, 8);
//                        for (int i = 0; i < list.size(); i++) {
//                            instance.activeSkill(id, new ResourceLocation(list.getString(i)));
//                        }
//                    }
//                }
//
//                if (tag.contains("SkillCooldownGameTime", 10)) {
//                    CompoundNBT rootCdNbt = tag.getCompound("SkillCooldownGameTime");
//                    for (String category : rootCdNbt.getAllKeys()) {
//                        CompoundNBT skillMapNbt = rootCdNbt.getCompound(category);
//                        for (String skillIdStr : skillMapNbt.getAllKeys()) {
//                            instance.setSkillCooldownGameTime(category, new ResourceLocation(skillIdStr), skillMapNbt.getLong(skillIdStr));
//                        }
//                    }
//                }
//
//                if (tag.contains("SkillCooldown", 10)) {
//                    CompoundNBT rootCdNbt = tag.getCompound("SkillCooldown");
//                    for (String category : rootCdNbt.getAllKeys()) {
//                        CompoundNBT skillMapNbt = rootCdNbt.getCompound(category);
//                        for (String skillIdStr : skillMapNbt.getAllKeys()) {
//                            instance.setSkillCooldown(category, new ResourceLocation(skillIdStr), skillMapNbt.getLong(skillIdStr));
//                        }
//                    }
//                }
//            }
//        }
//    }
}
