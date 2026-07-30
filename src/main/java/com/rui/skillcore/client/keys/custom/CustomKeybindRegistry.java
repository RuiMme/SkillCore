package com.rui.skillcore.client.keys.custom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomKeybindRegistry {
    public static final List<CustomKeyBind> SKILL_BINDS = new ArrayList<>();
    public static final List<CustomKeyBind> FUNCTION_BINDS = new ArrayList<>();
    public static final Map<String, Boolean> TOGGLE_STATES = new HashMap<>();

    // 2. 分类注册系统
    public static final List<KeybindCategory> CATEGORIES = new ArrayList<>();

    // ==========================================
    // 静态代码块：打包分类 (在上面的按键全部 new 完之后执行)
    // ==========================================
    static {
        registerCategory("技能按键设置", SKILL_BINDS);
        registerCategory("功能按键设置", FUNCTION_BINDS);
    }

    // --- 注册方法 ---
    public static CustomKeyBind registerSkill(String id, String name, int defaultKey) {
        CustomKeyBind bind = new CustomKeyBind(id, name, defaultKey);
        SKILL_BINDS.add(bind);
        return bind;
    }

    public static CustomKeyBind registerFunction(String id, String name, int defaultKey) {
        CustomKeyBind bind = new CustomKeyBind(id, name, defaultKey);
        FUNCTION_BINDS.add(bind);
        return bind;
    }

    public static void registerCategory(String name, List<CustomKeyBind> binds) {
        CATEGORIES.add(new KeybindCategory(name, binds));
    }

    // --- 开关状态方法 ---
    public static boolean getToggleState(String id, boolean defaultValue) {
        return TOGGLE_STATES.getOrDefault(id, defaultValue);
    }

    public static void setToggleState(String id, boolean value) {
        TOGGLE_STATES.put(id, value);
    }

    // --- 内部数据类 ---
    public static class KeybindCategory {
        public final String name;
        public final List<CustomKeyBind> binds;

        public KeybindCategory(String name, List<CustomKeyBind> binds) {
            this.name = name;
            this.binds = binds;
        }
    }
}
