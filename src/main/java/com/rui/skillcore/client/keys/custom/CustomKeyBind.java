package com.rui.skillcore.client.keys.custom;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.client.settings.KeyModifier;

public class CustomKeyBind {
    private final String id;
    private final String description;

    private final InputMappings.Input defaultKey;
    private final boolean defaultCtrl;
    private final boolean defaultShift;
    private final boolean defaultAlt;

    private InputMappings.Input currentKey;
    private boolean currentCtrl;
    private boolean currentShift;
    private boolean currentAlt;

    private int pressTime = 0;

    // 构造函数：无修饰键
    public CustomKeyBind(String id, String description, int defaultKeyCode) {
        this(id, description, defaultKeyCode, false, false, false);
    }

    // 构造函数：支持默认组合键
    public CustomKeyBind(String id, String description, int defaultKeyCode, boolean ctrl, boolean shift, boolean alt) {
        this.id = id;
        this.description = description;
        this.defaultKey = InputMappings.Type.KEYSYM.getOrCreate(defaultKeyCode);
        this.defaultCtrl = ctrl;
        this.defaultShift = shift;
        this.defaultAlt = alt;

        this.currentKey = this.defaultKey;
        this.currentCtrl = ctrl;
        this.currentShift = shift;
        this.currentAlt = alt;
    }

    // 绑定新按键（同时保存修饰键状态）
    public void setKey(InputMappings.Input key, boolean ctrl, boolean shift, boolean alt) {
        this.currentKey = key;
        this.currentCtrl = ctrl;
        this.currentShift = shift;
        this.currentAlt = alt;
    }

    // 重置为默认按键
    public void reset() {
        this.currentKey = this.defaultKey;
        this.currentCtrl = this.defaultCtrl;
        this.currentShift = this.defaultShift;
        this.currentAlt = this.defaultAlt;
    }

    // 是否为默认配置
    public boolean isDefault() {
        return this.currentKey.equals(this.defaultKey)
                && this.currentCtrl == this.defaultCtrl
                && this.currentShift == this.defaultShift
                && this.currentAlt == this.defaultAlt;
    }

    // 格式化输出显示文本 (例如: "Ctrl + Shift + M")
    public ITextComponent getDisplayName() {
        if (this.currentKey == InputMappings.UNKNOWN) {
            return this.currentKey.getDisplayName();
        }
        StringBuilder sb = new StringBuilder();
        if (this.currentCtrl) sb.append("Ctrl + ");
        if (this.currentShift) sb.append("Shift + ");
        if (this.currentAlt) sb.append("Alt + ");
        return new StringTextComponent(sb.toString()).append(this.currentKey.getDisplayName());
    }

    // 匹配按键与修饰键输入
    public boolean matches(int keyCode, boolean ctrl, boolean shift, boolean alt) {
        if (this.currentKey == InputMappings.UNKNOWN) return false;
        return this.currentKey.getValue() == keyCode
                && this.currentCtrl == ctrl
                && this.currentShift == shift
                && this.currentAlt == alt;
    }

    // 冲突检测 (考虑组合键)
    public boolean hasConflict() {
        if (this.currentKey == InputMappings.UNKNOWN) return false;

        // 1. 检测自定义按键之间的冲突
        for (CustomKeybindRegistry.KeybindCategory category : CustomKeybindRegistry.CATEGORIES) {
            for (CustomKeyBind other : category.binds) {
                if (other != this && other.getKey().equals(this.currentKey)
                        && other.isNeedCtrl() == this.currentCtrl
                        && other.isNeedShift() == this.currentShift
                        && other.isNeedAlt() == this.currentAlt) {
                    return true;
                }
            }
        }

        // 2. 检测原版/其他 Mod 的按键冲突
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null && mc.options.keyMappings != null) {
            for (KeyBinding vanillaBind : mc.options.keyMappings) {
                if (vanillaBind.getKey().equals(this.currentKey)) {
                    KeyModifier modifier = vanillaBind.getKeyModifier();
                    boolean vanillaCtrl = (modifier == KeyModifier.CONTROL);
                    boolean vanillaShift = (modifier == KeyModifier.SHIFT);
                    boolean vanillaAlt = (modifier == KeyModifier.ALT);

                    if (this.currentCtrl == vanillaCtrl && this.currentShift == vanillaShift && this.currentAlt == vanillaAlt) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean consumeClick() {
        if (this.pressTime > 0) {
            this.pressTime--;
            return true;
        }
        return false;
    }

    public void handlePress() { this.pressTime++; }

    // Getters
    public String getId() { return id; }
    public String getDescription() { return description; }
    public InputMappings.Input getKey() { return currentKey; }
    public boolean isNeedCtrl() { return currentCtrl; }
    public boolean isNeedShift() { return currentShift; }
    public boolean isNeedAlt() { return currentAlt; }
}
