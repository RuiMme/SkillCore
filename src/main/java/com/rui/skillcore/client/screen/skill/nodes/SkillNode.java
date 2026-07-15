package com.rui.skillcore.client.screen.skill.nodes;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.rui.skillcore.api.capability.skill.ISkillData;
import com.rui.skillcore.api.capability.skill.SkillProvider;
import com.rui.skillcore.libs.LibMisc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

public class SkillNode {
    private final String category;
    private final SkillInfo info;
    private final ResourceLocation icon;
    private final String nbt;
    private final String name;
    private final String description;
    private final int x; // 在树中的相对 X 坐标
    private final int y; // 在树中的相对 Y 坐标
    private List<SkillNode> parent = new ArrayList<>();
    private final List<SkillNode> children = new ArrayList<>();
    private static final ResourceLocation WIDGETS = new ResourceLocation(LibMisc.MOD_ID, "textures/gui/icon.png");

    private ItemStack cachedItemIcon = ItemStack.EMPTY;
    private boolean isItem = false;

    public SkillNode(String category, SkillInfo info, List<SkillNode> parent, String name, String description, ResourceLocation icon, int x, int y) {
        this(category, info, parent, name, description, icon, "", x, y);
    }

    public SkillNode(String category, SkillInfo info, List<SkillNode> parent, String name, String description, ResourceLocation icon, String nbt, int x, int y) {
        this.category = category;
        this.info = info;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.nbt = nbt;
        this.x = x;
        this.y = y;
        this.setParent(parent);

        this.initIconCache();
    }

    /**
     * 初始化物品和 NBT 缓存，防止运行时每帧查询注册表和动态解析字符串
     */
    private void initIconCache() {
        Item item = ForgeRegistries.ITEMS.getValue(this.icon);
        if (item != null && !item.equals(Items.AIR)) {
            this.isItem = true;
            this.cachedItemIcon = new ItemStack(item);

            // 如果传入了 NBT 字符串，尝试解析并注入到 ItemStack 中
            if (this.nbt != null && !this.nbt.trim().isEmpty()) {
                try {
                    CompoundTag parsedTag = TagParser.parseTag(this.nbt);
                    this.cachedItemIcon.setTag(parsedTag);
                } catch (Exception e) {
                    // 防止非法的 NBT 导致游戏崩溃，解析失败时打印错误并保留无 NBT 的原生状态
                    System.err.println("[SkillCore] 无法为节点 " + this.name + " 解析 NBT 字符串: " + this.nbt);
                    e.printStackTrace();
                }
            }
        } else {
            this.isItem = false;
            this.cachedItemIcon = ItemStack.EMPTY;
        }
    }

    public void render(PoseStack ms) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null) return;

        player.getCapability(SkillProvider.SKILL_CAP).ifPresent(cap -> {
            boolean isUnlocked = cap.hasSkill(this.getCategory(), this.getId());
            boolean isActive = cap.isActive(this.getCategory(), this.getId());
            boolean isParentUnlocked = this.getParent().isEmpty();
            for (SkillNode parent : this.getParent()) {
                isParentUnlocked = cap.hasSkill(parent.getCategory(), parent.getId());
                if (!isParentUnlocked) break;
            }
            if(isUnlocked && isActive) {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            } else if (!isParentUnlocked) {
                RenderSystem.setShaderColor(0.1F, 0.1F, 0.1F, 1.0F);
            } else if (!isActive) {
                RenderSystem.setShaderColor(0.4F, 0.4F, 0.4F, 1.0F);
            } else {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }

            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            // 渲染背景框 (使用原版进度的方形框)
            RenderSystem.setShaderTexture(0, WIDGETS);
            // 如果已解锁使用高亮框(u=0,v=26)，未解锁使用暗色框(u=0,v=0)
            int vOffset = isUnlocked ? 155 : 181;
            GuiComponent.blit(ms, this.x - 13, this.y - 13, 0, vOffset, 26, 26, 256, 256);

            long cooldown = getCooldownTime(cap, player.level.getGameTime());
            if (this.isItem) {
                PoseStack modelViewStack = RenderSystem.getModelViewStack();
                modelViewStack.pushPose();
                modelViewStack.mulPoseMatrix(ms.last().pose());
                RenderSystem.applyModelViewMatrix();
                mc.getItemRenderer().renderGuiItem(this.cachedItemIcon, this.x - 8, this.y - 8);
                modelViewStack.popPose();
                RenderSystem.applyModelViewMatrix();

                ms.pushPose();
                ms.translate(0, 0, 250.0F);
                RenderSystem.disableDepthTest(); // 禁用深度测试，无视任何模型凸起
                RenderSystem.enableBlend();      // 确保透明混合开启
                RenderSystem.defaultBlendFunc();

                // 遮罩补全：物品渲染器不受 RenderSystem.color4f 影响，我们用 fill 覆盖一层半透明来表现未解锁的灰度
                if (!isParentUnlocked) {
                    RenderSystem.blendFunc(GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.SRC_COLOR);
                    GuiComponent.fill(ms, this.x - 8, this.y - 8, this.x + 8, this.y + 8, 0xD5101010); // 深黑
                } else if (!isUnlocked || !isActive) {
                    RenderSystem.blendFunc(GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.SRC_COLOR);
                    GuiComponent.fill(ms, this.x - 8, this.y - 8, this.x + 8, this.y + 8, 0x85252525); // 暗灰
                }

                RenderSystem.defaultBlendFunc();

                // 动态冷却覆膜表现
                if (cooldown < 16L) {
                    GuiComponent.fill(ms, this.x - 8, (int) (this.y - 8 + cooldown), this.x + 8, this.y + 8, 0x99000000);
                }

                RenderSystem.enableDepthTest(); // 恢复深度测试
                ms.popPose();
            } else {
                if(isUnlocked && isActive) {
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                } else if (!isParentUnlocked) {
                    RenderSystem.setShaderColor(0.1F, 0.1F, 0.1F, 1.0F);
                } else if (!isActive) {
                    RenderSystem.setShaderColor(0.4F, 0.4F, 0.4F, 1.0F);
                } else {
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                }

                RenderSystem.setShaderTexture(0, icon);
                if (cooldown < 16L) {
                    GuiComponent.blit(ms, this.x - 8, this.y - 8, 0, 0, 16, (int) cooldown, 16, 16);
                } else {
                    GuiComponent.blit(ms, this.x - 8, this.y - 8, 0, 0, 16, 16, 16, 16);
                }
            }


            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        });
    }

    // 检查鼠标是否悬停在节点上
    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= this.x - 13 && mouseX <= this.x + 13 &&
                mouseY >= this.y - 13 && mouseY <= this.y + 13;
    }

    public void setParent(List<SkillNode> parent) {
        if (parent == null) return;
        this.parent = parent;
        for (SkillNode node : parent) {
            if (!node.getChildren().contains(this)) {
                node.addChild(this);
            }
        }
    }

    public void addChild(SkillNode child) {
        this.children.add(child);
    }

    // --- Getters ---
    public String getCategory() {
        return this.category;
    }

    public SkillInfo getInfo() {
        return this.info;
    }

    public ResourceLocation getId() {
        return this.info.getId();
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public List<SkillNode> getParent() {
        return parent;
    }

    public List<SkillNode> getChildren() {
        return children;
    }

    public long getCooldown() {
        return info.getCooldown();
    }

    public long getCooldownTime(ISkillData cap, long currentGameTime) {
        long gameTime = cap.getSkillCooldownGameTime().computeIfAbsent(this.category, k -> new HashMap<>()).getOrDefault(this.getId(), 0L);
        long cd = getCooldown();
        if (gameTime <= 0L || cd <= 0L) return 16L;

        if (currentGameTime - gameTime < cd) {
            return (long) ((16F / (float) cd) * (currentGameTime - gameTime));
        }
        return 16L;
    }

    /**
     * 动态推导当前技能节点在 UI 或逻辑中是否可见/可用
     * @param cap 玩家的技能数据 Capability 实例 (用于检查玩家已解锁了什么)
     * @param categorizedNodes 全局注册缓存 (传入 SkillManager.CATEGORIZED_NODES)
     * @return true: 可见且能交互; false: 被直接互斥或因前置坍塌而隐藏,需在渲染列表中直接蒸发
     */
    public boolean isVisible(ISkillData cap, Map<String, Map<ResourceLocation, SkillNode>> categorizedNodes) {
        // 遍历该玩家当前在所有大类（Category）下已经解锁的所有技能
        for (Map.Entry<String, Set<ResourceLocation>> entry : cap.getUnlockedSkills().entrySet()) {
            String cat = entry.getKey();
            Map<ResourceLocation, SkillNode> nodeMap = categorizedNodes.get(cat);
            if (nodeMap == null) continue;

            for (ResourceLocation unlockedId : entry.getValue()) {
                SkillNode unlockedNode = nodeMap.get(unlockedId);
                // 如果玩家已经解锁的某个技能，其配置的互斥黑名单里包含了当前技能的 ID
                if (unlockedNode != null && unlockedNode.getInfo().getExclusiveIds().contains(this.getId())) {
                    return false; // 触发直接互斥拦截，立即隐藏！
                }
            }
        }
        // 如果本技能拥有前置父节点，只要任意一个父节点处于隐藏状态，子技能一并连带隐藏！
        if (this.parent != null && !this.parent.isEmpty()) {
            for (SkillNode parentNode : this.parent) {
                // 向上溯源：如果父节点在当前上下文里已经不可见了
                if (!parentNode.isVisible(cap, categorizedNodes)) {
                    return false; // 父分支断裂，子技能株连隐藏！
                }
            }
        }
        return true; // 既没被直接拉黑,前置技能树也完好,放行显示
    }
}
