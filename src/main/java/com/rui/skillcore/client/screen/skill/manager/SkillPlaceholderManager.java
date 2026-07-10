package com.rui.skillcore.client.screen.skill.manager;

import net.minecraft.client.entity.player.ClientPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class SkillPlaceholderManager {
    // 核心字典：键是占位符(如 "days")，值是计算逻辑(返回字符串)
    private static final Map<String, Function<ClientPlayerEntity, String>> PLACEHOLDERS = new HashMap<>();

    /**
     * 注册一个新的占位符
     * @param key 占位符名称 (不需要带大括号，如 "days")
     * @param resolver 当UI遇到这个占位符时，如何计算出具体数值的函数
     */
    public static void register(String key, Function<ClientPlayerEntity, String> resolver) {
        PLACEHOLDERS.put("{" + key + "}", resolver);
    }

    /**
     * 核心解析器：传入原始文本，替换掉里面所有的占位符
     */
    public static String parse(String rawText, ClientPlayerEntity player) {
        if (rawText == null || rawText.isEmpty()) return rawText;

        String parsedText = rawText;
        // 遍历所有已注册的占位符
        for (Map.Entry<String, Function<ClientPlayerEntity, String>> entry : PLACEHOLDERS.entrySet()) {
            // 如果文本里包含这个占位符，才去执行对应的计算逻辑，节省性能
            if (parsedText.contains(entry.getKey())) {
                String calculatedValue = entry.getValue().apply(player);
                parsedText = parsedText.replace(entry.getKey(), calculatedValue);
            }
        }
        return parsedText;
    }
}
