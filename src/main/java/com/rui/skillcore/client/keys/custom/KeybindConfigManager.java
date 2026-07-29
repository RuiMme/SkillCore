package com.rui.skillcore.client.keys.custom;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.util.InputMappings;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Map;

public class KeybindConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = FMLPaths.CONFIGDIR.get().resolve("skill_keybinds.json").toFile();

    // 存档
    public static void save() {
        JsonObject json = new JsonObject();
        JsonObject keysObj = new JsonObject();
        JsonObject togglesObj = new JsonObject();

        // 1. 保存技能按键 (升级为保存对象，包含主键及 Ctrl/Shift/Alt 状态)
        for (CustomKeyBind bind : CustomKeybindRegistry.SKILL_BINDS) {
            JsonObject bindObj = new JsonObject();
            bindObj.addProperty("key", bind.getKey().getName());
            bindObj.addProperty("ctrl", bind.isNeedCtrl());
            bindObj.addProperty("shift", bind.isNeedShift());
            bindObj.addProperty("alt", bind.isNeedAlt());
            keysObj.add(bind.getId(), bindObj);
        }

        // 2. 保存功能按键 (同上)
        for (CustomKeyBind bind : CustomKeybindRegistry.FUNCTION_BINDS) {
            JsonObject bindObj = new JsonObject();
            bindObj.addProperty("key", bind.getKey().getName());
            bindObj.addProperty("ctrl", bind.isNeedCtrl());
            bindObj.addProperty("shift", bind.isNeedShift());
            bindObj.addProperty("alt", bind.isNeedAlt());
            keysObj.add(bind.getId(), bindObj);
        }

        // 3. 保存功能开关状态 (Toggle States)
        for (Map.Entry<String, Boolean> entry : CustomKeybindRegistry.TOGGLE_STATES.entrySet()) {
            togglesObj.addProperty(entry.getKey(), entry.getValue());
        }

        json.add("keys", keysObj);
        json.add("toggles", togglesObj);

        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(json, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 读档
    public static void load() {
        if (!CONFIG_FILE.exists()) {
            save(); // 文件不存在则生成默认配置
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);

            // 1. 读取按键
            if (json.has("keys")) {
                JsonObject keysObj = json.getAsJsonObject("keys");
                loadKeysToList(keysObj, CustomKeybindRegistry.SKILL_BINDS);
                loadKeysToList(keysObj, CustomKeybindRegistry.FUNCTION_BINDS);
            }

            // 2. 读取开关状态
            if (json.has("toggles")) {
                JsonObject togglesObj = json.getAsJsonObject("toggles");
                for (Map.Entry<String, JsonElement> entry : togglesObj.entrySet()) {
                    CustomKeybindRegistry.TOGGLE_STATES.put(entry.getKey(), entry.getValue().getAsBoolean());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void loadKeysToList(JsonObject keysObj, Iterable<CustomKeyBind> bindList) {
        for (CustomKeyBind bind : bindList) {
            if (keysObj.has(bind.getId())) {
                JsonElement element = keysObj.get(bind.getId());

                // 情况A：新版结构（JsonObject，包含组合键信息）
                if (element.isJsonObject()) {
                    JsonObject bindObj = element.getAsJsonObject();
                    String keyName = bindObj.has("key") ? bindObj.get("key").getAsString() : "key.keyboard.unknown";
                    boolean ctrl = bindObj.has("ctrl") && bindObj.get("ctrl").getAsBoolean();
                    boolean shift = bindObj.has("shift") && bindObj.get("shift").getAsBoolean();
                    boolean alt = bindObj.has("alt") && bindObj.get("alt").getAsBoolean();

                    bind.setKey(InputMappings.getKey(keyName), ctrl, shift, alt);
                }
                // 情况B：旧版兼容（纯字符串，如直接记录了按键名）
                else if (element.isJsonPrimitive()) {
                    String keyName = element.getAsString();
                    bind.setKey(InputMappings.getKey(keyName), false, false, false);
                }
            }
        }
    }
}
