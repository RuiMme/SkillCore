package com.rui.skillcore.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.rui.skillcore.client.screen.skill.manager.SkillManager;
import com.rui.skillcore.client.screen.skill.nodes.SkillInfo;
import com.rui.skillcore.client.screen.skill.nodes.SkillNode;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SkillDataLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();
    public static final Map<ResourceLocation, SkillData> RAW_DATA = new HashMap<>();
    public static final Map<String, SkillCategoryInfoData> CATEGORY_DATA = new HashMap<>();

    public SkillDataLoader() {
        super(GSON, "skills");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objectIn, ResourceManager resourceManagerIn, ProfilerFiller profilerIn) {
        RAW_DATA.clear();
        CATEGORY_DATA.clear();
        objectIn.forEach((loc, json) -> {
            String path = loc.getPath();
            String[] pathParts = path.split("/");
            String category = pathParts.length > 1 ? pathParts[0] : path;
            if(pathParts.length == 2 && pathParts[1].equals("root")) {
                SkillCategoryInfoData categoryInfoData = GSON.fromJson(json, SkillCategoryInfoData.class);
                if(ModList.get().isLoaded(categoryInfoData.modid) && categoryInfoData.show) {
                    System.out.println("Load Mod: " + categoryInfoData.modid);
                    categoryInfoData.category = category;
                    CATEGORY_DATA.put(category, categoryInfoData);
                }
            }
        });
        objectIn.forEach((loc, json) -> {
            String path = loc.getPath();
            String[] pathParts = path.split("/");
            String category = pathParts.length > 1 ? pathParts[0] : path;
            if(!pathParts[1].equals("root")) {
                if(CATEGORY_DATA.containsKey(category)) {
                    SkillData model = GSON.fromJson(json, SkillData.class);
                    model.category = category; // 强制覆盖，由文件夹层级决定
                    RAW_DATA.put(new ResourceLocation(model.id), model);
                }
            }
        });
    }

    public static void buildNodesOnClient() {
        Map<ResourceLocation, SkillNode> nodeCache = new HashMap<>();

        RAW_DATA.forEach((id, model) -> {
            List<ResourceLocation> pIds = model.parents != null ?
                    model.parents.stream().map(ResourceLocation::new).collect(Collectors.toList()) : null;
            List<ResourceLocation> exclusiveIds = new java.util.ArrayList<>();
            Map<Item, Integer> cost = new HashMap<>();

            if(model.cost != null) {
                model.cost.forEach((itemId, count) -> {
                    cost.put(ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId)), count);
                });
            }

            if (model.exclusives != null) {
                for (String exId : model.exclusives) {
                    exclusiveIds.add(new ResourceLocation(exId));
                }
            }

            SkillInfo info = new SkillInfo(
                    id, pIds,
                    model.levelRequirement,
                    cost,
                    model.cooldown,
                    exclusiveIds
            );

            // 注意：此时 parent 传空，我们第二遍再连线
            SkillNode node = new SkillNode(
                    model.category, info, new ArrayList<>(), model.name, model.description,
                    new ResourceLocation(model.icon), model.nbt, model.x, model.y
            );
            nodeCache.put(id, node);
        });

        nodeCache.forEach((id, node) -> {
            List<ResourceLocation> pIds = node.getInfo().getParentIds();
            if (pIds != null) {
                List<SkillNode> parents = new ArrayList<>();
                for (ResourceLocation pId : pIds) {
                    if (nodeCache.containsKey(pId)) {
                        parents.add(nodeCache.get(pId));
                    }
                }
                node.setParent(parents); // 这里会触发你代码里的 node.addChild(this)
            }
            SkillManager.CATEGORIZED_NODES.computeIfAbsent(node.getCategory(), k-> new HashMap<>()).put(id, node);
        });

        SkillManager.CATEGORY_DATA.putAll(CATEGORY_DATA);


//        SkillManager.CATEGORIZED_NODES = nodeCache;
    }
}
