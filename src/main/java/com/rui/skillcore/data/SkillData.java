package com.rui.skillcore.data;

import java.util.HashMap;
import java.util.List;

public class SkillData {
    public String category;
    public String id;               // 技能唯一标识
    public List<String> parents;    // 前置技能 ID 列表
    public String name;
    public String description;
    public String icon;
    public String nbt;
    public int x;
    public int y;
    public int levelRequirement;
    public HashMap<String, Integer> cost;
    public long cooldown;
    public List<String> dependencies;       // 这个技能要生效，必须依赖哪些技能也处于生效状态.留空则不需要依赖.
    public List<String> overrides;          // 如果这个技能生效了，它会屏蔽掉哪些低阶技能
    public List<String> exclusives;
}
