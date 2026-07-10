package com.rui.skillcore.api.network.json;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rui.skillcore.data.SkillCategoryInfoData;
import com.rui.skillcore.data.SkillData;
import com.rui.skillcore.data.SkillDataLoader;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class S2CSyncSkillsPacket {
    private final String rawDataJson;
    private final String categoryDataJson;
    // 构造函数：由服务端调用，传入服务端的真实数据
    public S2CSyncSkillsPacket(Map<ResourceLocation, SkillData> rawData, Map<String, SkillCategoryInfoData> categoryData) {
        Gson gson = new Gson();

        Map<String, SkillData> stringKeyMap = new HashMap<>();
        rawData.forEach((loc, data) -> stringKeyMap.put(loc.toString(), data));
        this.rawDataJson = gson.toJson(stringKeyMap);
        this.categoryDataJson = gson.toJson(categoryData);

    }

    // 从网络缓冲区读取（客户端收到时）
    public S2CSyncSkillsPacket(PacketBuffer buffer) {
        this.rawDataJson = buffer.readUtf(1048576);
        this.categoryDataJson = buffer.readUtf(1048576);
    }

    // 写入网络缓冲区（服务端发送时）
    public void encode(PacketBuffer buffer) {
        buffer.writeUtf(this.rawDataJson, 1048576);
        buffer.writeUtf(this.categoryDataJson, 1048576);
    }

    // 客户端收到数据包后的处理逻辑
    public static void handle(S2CSyncSkillsPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 确保在客户端执行
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                Gson gson = new Gson();
                // 1. 清空客户端旧数据
                SkillDataLoader.RAW_DATA.clear();
                SkillDataLoader.CATEGORY_DATA.clear();

                // 2. 将服务端传过来的 JSON 反序列化到客户端的 Map 中
                Type rawType = new TypeToken<Map<String, SkillData>>(){}.getType();
                Type catType = new TypeToken<Map<String, SkillCategoryInfoData>>(){}.getType();

                Map<String, SkillData> decryptedRaw = gson.fromJson(packet.rawDataJson, rawType);
                Map<String, SkillCategoryInfoData> decryptedCat = gson.fromJson(packet.categoryDataJson, catType);

                if(decryptedRaw != null) {
                    decryptedRaw.forEach((stringLoc, data) -> {
                        SkillDataLoader.RAW_DATA.put(new ResourceLocation(stringLoc), data);
                    });
                }
                if(decryptedCat != null) SkillDataLoader.CATEGORY_DATA.putAll(decryptedCat);

                // 3. 核心：让客户端在拿到数据后，立刻开始构建 GUI 节点！
                // （就是你原本写在 Loader 里的那个方法，现在挪到 ClientUtils 里）
                SkillDataLoader.buildNodesOnClient();
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
