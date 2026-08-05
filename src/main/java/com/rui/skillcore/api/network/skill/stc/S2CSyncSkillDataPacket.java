package com.rui.skillcore.api.network.skill.stc;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rui.skillcore.data.SkillCategoryInfoData;
import com.rui.skillcore.data.SkillData;
import com.rui.skillcore.data.SkillDataLoader;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.Map;
import java.util.function.Supplier;

public class S2CSyncSkillDataPacket {
    private static final Gson GSON = new Gson();
    private final String rawDataJson;
    private final String categoryDataJson;

    public S2CSyncSkillDataPacket(Map<?, ?> rawData, Map<?, ?> categoryData) {
        this.rawDataJson = GSON.toJson(rawData);
        this.categoryDataJson = GSON.toJson(categoryData);
    }

    public S2CSyncSkillDataPacket(FriendlyByteBuf buffer) {
        this.rawDataJson = buffer.readUtf(262144);
        this.categoryDataJson = buffer.readUtf(262144);
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.rawDataJson, 262144);
        buffer.writeUtf(this.categoryDataJson, 262144);
    }

    public static void handle(S2CSyncSkillDataPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 确保在客户端处理
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                // 1. 反序列化服务端发来的 JSON 数据
                Map<String, SkillData> rawMap = GSON.fromJson(packet.rawDataJson,
                        new TypeToken<Map<String, SkillData>>(){}.getType());
                Map<String, SkillCategoryInfoData> catMap = GSON.fromJson(packet.categoryDataJson,
                        new TypeToken<Map<String, SkillCategoryInfoData>>(){}.getType());

                // 2. 填充到客户端的 SkillDataLoader 内存中
                SkillDataLoader.RAW_DATA.clear();
                rawMap.forEach((k, v) -> SkillDataLoader.RAW_DATA.put(new ResourceLocation(k), v));

                SkillDataLoader.CATEGORY_DATA.clear();
                SkillDataLoader.CATEGORY_DATA.putAll(catMap);

                // 3. 此时客户端有了数据，立即构建 UI 节点！
                SkillDataLoader.buildNodesOnClient();
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
