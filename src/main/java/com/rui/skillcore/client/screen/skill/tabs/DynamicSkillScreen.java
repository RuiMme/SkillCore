package com.rui.skillcore.client.screen.skill.tabs;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.rui.skillcore.api.capability.skill.ISkillData;
import com.rui.skillcore.api.capability.skill.SkillProvider;
import com.rui.skillcore.api.network.PacketHandler;
import com.rui.skillcore.api.network.skill.cts.C2SToggleSkillPacket;
import com.rui.skillcore.api.network.skill.cts.C2SUnlockSkillPacket;
import com.rui.skillcore.client.keys.KeyBindings;
import com.rui.skillcore.client.screen.skill.manager.SkillManager;
import com.rui.skillcore.client.screen.skill.manager.SkillPlaceholderManager;
import com.rui.skillcore.client.screen.skill.nodes.SkillNode;
import com.rui.skillcore.libs.util.InventoryUtil;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DynamicSkillScreen extends AbstractSkillScreen {
    private final String category;
    private final Map<ResourceLocation, SkillNode> categoryNodes;
    private int guiLeft;
    private int guiTop;
    private int xSize = 252;
    private int ySize = 140;

    // 拖拽平移相关
    private double scrollX = 0;
    private double scrollY = 0;
    private double dragStartX = 0; // 记录按下时的绝对 X
    private double dragStartY = 0; // 记录按下时的绝对 Y

    private SkillNode clickedNode = null;
    private boolean preventClickDueToDrag = false;

    // 缩放相关变量
    private float zoom = 0.8F;
    private static final float MIN_ZOOM = 0.5F;  // 最小缩小到 50%
    private static final float MAX_ZOOM = 2.0F;  // 最大放大到 200%
    private static final float ZOOM_STEP = 0.15F; // 每次滚轮的缩放步长

    public DynamicSkillScreen(String category, ITextComponent title) {
        super(title);
        this.category = category;
        this.categoryNodes = SkillManager.getNodesByCategory(category);
    }

    public void initTab(int width, int height, Minecraft mc, int xSize, int ySize, int guiLeft, int guiTop) {
        this.width = width;
        this.height = height;
        this.minecraft = mc;
        this.font = mc.font;
        this.xSize = xSize;
        this.ySize = ySize;
        this.guiLeft = guiLeft;
        this.guiTop = guiTop;
        // 原本 init() 里的逻辑移到这里
//        this.guiLeft = (this.width - this.xSize) / 2;
//        this.guiTop = (this.height - this.ySize) / 2;
    }

    // --- 核心工具方法：将屏幕鼠标坐标映射回缩放后的世界坐标 ---
    private double getRelativeX(double mouseX) {
        return (mouseX - (guiLeft + xSize / 2f)) / zoom - scrollX;
    }

    private double getRelativeY(double mouseY) {
        return (mouseY - (guiTop + ySize / 2f)) / zoom - scrollY;
    }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
//        this.renderBackground(ms);
        if(this.minecraft == null) return;
        MainWindow window = this.minecraft.getWindow();
        ClientPlayerEntity player = this.minecraft.player;
        ISkillData cap = player.getCapability(SkillProvider.SKILL_CAP).orElse(null);
        double scale = window.getGuiScale();

        // 设置 OpenGL 物理裁剪区，防止超出窗口
        int clipX = guiLeft + 9;
        int clipY = guiTop + 18;
        int clipW = xSize - 18;
        int clipH = ySize - 27;

        int scissorX = (int) (clipX * scale);
        int scissorY = (int) (window.getScreenHeight() - ((clipY + clipH) * scale));
        int scissorW = (int) (clipW * scale);
        int scissorH = (int) (clipH * scale);

        RenderSystem.pushMatrix();
        RenderSystem.enableScissor(scissorX, scissorY, scissorW, scissorH);

        ms.pushPose();
        // 核心渲染矩阵：移到中心 -> 缩放 -> 应用拖拽位移
        ms.translate(guiLeft + xSize / 2f, guiTop + ySize / 2f, 0);
        ms.scale(zoom, zoom, 1.0F);
        ms.translate(scrollX, scrollY, 0);

        renderScrollingBackground(ms);

        for (SkillNode node : this.categoryNodes.values()) {
            if (cap != null && !node.isVisible(cap, SkillManager.CATEGORIZED_NODES)) {
                continue; // 不画图标、不画背景、不画连线、无法被鼠标悬停
            }
            for(SkillNode parent : node.getParent()) {
                if (node.getParent() != null) {
                    drawConnector(ms, player, parent, node);
                }
            }
        }

        // 渲染节点并检测悬停（使用逆向映射的坐标）
        SkillNode hoveredNode = null;
        double relMouseX = getRelativeX(mouseX);
        double relMouseY = getRelativeY(mouseY);
        boolean mouseInViewport = isMouseInViewport(mouseX, mouseY);

        for (SkillNode node : this.categoryNodes.values()) {
            if (cap != null && !node.isVisible(cap, SkillManager.CATEGORIZED_NODES)) {
                continue; // 不画图标、不画背景、不画连线、无法被鼠标悬停
            }
            node.render(ms);
            if (mouseInViewport && node.isMouseOver((int)relMouseX, (int)relMouseY)) {
                hoveredNode = node;
            }
        }

        ms.popPose();
        RenderSystem.disableScissor();
        RenderSystem.popMatrix();

        // 渲染 UI 外壳边框
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.font.draw(ms, this.title, guiLeft + 8, guiTop + 6, 4210752);

        // 渲染 Tooltip (必须在缩放矩阵外渲染，防止文字模糊或变大)
        renderTooltip(ms, player, hoveredNode, mouseX, mouseY);

        super.render(ms, mouseX, mouseY, partialTicks);
    }

    private void renderTooltip(MatrixStack ms, ClientPlayerEntity player, SkillNode hoveredNode, int mouseX, int mouseY) {
        if (hoveredNode != null) {
            List<ITextComponent> tooltip = new ArrayList<>();

            player.getCapability(SkillProvider.SKILL_CAP).ifPresent(cap -> {
                boolean isParentUnlocked = hoveredNode.getParent().isEmpty();
                for (SkillNode parent : hoveredNode.getParent()) {
                    isParentUnlocked = cap.hasSkill(parent.getCategory(), parent.getId());;
                    if(!isParentUnlocked) break;
                }

                if(isParentUnlocked) {
                    tooltip.add(new StringTextComponent("§e§l" + hoveredNode.getName()));
                    String rawDescription = hoveredNode.getDescription();

                    // 把解析工作全权丢给 Manager，UI 压根不需要知道 {days} 是什么意思
                    String parsedDescription = SkillPlaceholderManager.parse(rawDescription, player);
                    // 支持 \n 换行渲染
                    String[] descLines = parsedDescription.split("\n");
                    for (String line : descLines) {
                        tooltip.add(new StringTextComponent(line));
                    }

//                    tooltip.add(new StringTextComponent(rawDescription));
                    Prerequisites(hoveredNode, tooltip);
                    for (SkillNode parent : hoveredNode.getParent()) {
                        boolean unlocked = cap.hasSkill(parent.getCategory(), parent.getId());
                        String icon = unlocked ? "§a✔" : "§c✘";
                        tooltip.add(new StringTextComponent("  " + icon + " §8" + parent.getName()));
                    }

                    if (!cap.hasSkill(hoveredNode.getCategory(), hoveredNode.getId())) {
                        int lvl = hoveredNode.getInfo().getLevelRequirement();
                        String lvlColor = (minecraft.player.experienceLevel >= lvl) ? "§a" : "§c";
                        tooltip.add(new StringTextComponent(lvlColor + "• 需要等级: " + lvl));

                        hoveredNode.getInfo().getCost().forEach((item, count) -> {
                            int amount = InventoryUtil.getPlayerItemCount(minecraft.player, item);
                            String itemColor = (amount >= count) ? "§a" : "§c";
                            tooltip.add(new StringTextComponent(itemColor + "• 需要 " + item.getName(ItemStack.EMPTY).getString() + " x" + count));
                        });
                    } else {
                        tooltip.add(new StringTextComponent("§b§o[已激活]"));
                        if (cap.isActive(hoveredNode.getCategory(), hoveredNode.getId())) {
                            tooltip.add(new StringTextComponent("§a状态: 已开启"));
                        } else {
                            tooltip.add(new StringTextComponent("§c状态: 已关闭"));
                        }
                        if(hoveredNode.getCooldownTime(cap, player.level.getGameTime()) != 16L) {
                            tooltip.add(new StringTextComponent("§c技能冷却中"));
                        }
                    }
                } else {
                    tooltip.add(new StringTextComponent("§e§l???"));
                    Prerequisites(hoveredNode, tooltip);
                    for (SkillNode parent : hoveredNode.getParent()) {
                        boolean unlocked = cap.hasSkill(parent.getCategory(), parent.getId());
                        String icon = unlocked ? "§a✔" : "§c✘";
                        if(!unlocked) {
                            tooltip.add(new StringTextComponent("  " + icon + " §8???"));
                        } else {
                            tooltip.add(new StringTextComponent("  " + icon + " §8" + parent.getName()));
                        }
                    }
                }
            });
            this.renderComponentTooltip(ms, tooltip, mouseX, mouseY);
        }
    }

    private void Prerequisites(SkillNode hoveredNode, List<ITextComponent> tooltip) {
        tooltip.add(new StringTextComponent("§8" + "--------------------"));

        if (!hoveredNode.getParent().isEmpty()) {
            tooltip.add(new StringTextComponent("§7前置要求: "));
        }
    }

    // --- 动态视口背景填充算法 ---
    private void renderScrollingBackground(MatrixStack ms) {
        this.minecraft.getTextureManager().bind(new ResourceLocation(SkillManager.getCategoryData(this.category).background));

        // 计算当前缩放级别下，窗口所需的"世界尺寸"
        float bgWidth = xSize / zoom;
        float bgHeight = ySize / zoom;

        if (!SkillManager.getCategoryData(this.category).flat) {
            // 启用酷炫的“深邃星空视差漂移”特效
            long time = this.minecraft.level != null ? this.minecraft.level.getGameTime() : 0;
            float driftX = time * 0.05F;
            float driftY = time * 0.02F;
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            // 底层 (慢速拖拽，无漂移)
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            drawLayer(ms, bgWidth, bgHeight, scrollX, scrollY, 256.0F);
            // 中层 (中速拖拽，附加漂移)
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 0.6F);
            drawLayer(ms, bgWidth, bgHeight, scrollX * 0.5F + driftX, scrollY * 0.5F - driftY, 128.0F);
            // 表层 (快速拖拽，反向漂移)
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 0.3F);
            drawLayer(ms, bgWidth, bgHeight, scrollX * 0.2F - driftX, scrollY * 0.2F + driftY, 64.0F);
            RenderSystem.disableBlend();
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        } else {
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            // 强制将 UV 缩放系数锁死在 16.0F，完全跟随鼠标拖拽，没有漂移
            drawLayer(ms, bgWidth, bgHeight, scrollX, scrollY, 16.0F);
        }
    }

    private void drawLayer(MatrixStack ms, float bgWidth, float bgHeight, double offsetX, double offsetY, float textureScale) {
        float viewLeft = -bgWidth / 2f - (float)offsetX;
        float viewTop = -bgHeight / 2f - (float)offsetY;
        float viewRight = bgWidth / 2f - (float)offsetX;
        float viewBottom = bgHeight / 2f - (float)offsetY;

        // 【关键修复】将原本写死的 16.0F 替换为动态的 textureScale
        float u1 = viewLeft / textureScale;
        float v1 = viewTop / textureScale;
        float u2 = viewRight / textureScale;
        float v2 = viewBottom / textureScale;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuilder();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.vertex(ms.last().pose(), viewLeft, viewBottom, 0.0F).uv(u1, v2).endVertex();
        bufferbuilder.vertex(ms.last().pose(), viewRight, viewBottom, 0.0F).uv(u2, v2).endVertex();
        bufferbuilder.vertex(ms.last().pose(), viewRight, viewTop, 0.0F).uv(u2, v1).endVertex();
        bufferbuilder.vertex(ms.last().pose(), viewLeft, viewTop, 0.0F).uv(u1, v1).endVertex();
        tessellator.end();
    }

    // 连线
    private void drawConnector(MatrixStack ms, ClientPlayerEntity player, SkillNode parent, SkillNode child) {
//        int x1 = parent.getX();
//        int y1 = parent.getY();
//        int x2 = child.getX();
//        int y2 = child.getY();
//
//        if(!parent.getCategory().equals(child.getCategory())) return;
//
//        player.getCapability(SkillProvider.SKILL_CAP).ifPresent(cap -> {
//            int color = cap.hasSkill(child.getCategory(), child.getId()) ? 0xFFFFFFFF : 0xFF555555;
//
//            AbstractGui.fill(ms, x1, y1 - 1, x2, y1 + 1, color);
//            if (y2 > y1) {
//                AbstractGui.fill(ms, x2 - 1, y1, x2 + 1, y2, color);
//            } else {
//                AbstractGui.fill(ms, x2 - 1, y2, x2 + 1, y1, color);
//            }
//        });
        float x1 = parent.getX();
        float y1 = parent.getY();
        float x2 = child.getX();
        float y2 = child.getY();

        if(!parent.getCategory().equals(child.getCategory())) return;

        player.getCapability(SkillProvider.SKILL_CAP).ifPresent(cap -> {
            // 判定子节点是否已解锁（已解锁则连线亮起，未解锁则为暗灰色）
            boolean isUnlocked = cap.hasSkill(child.getCategory(), child.getId());
            // 调用高级连线渲染
            renderSkillConnection(ms, x1, y1, x2, y2, isUnlocked);
        });
    }

    /**
     * 高级技能连线渲染
     * @param x1 起点 X
     * @param y1 起点 Y
     * @param x2 终点 X
     * @param y2 终点 Y
     * @param isUnlocked 技能是否已解锁（用于变色和发光）
     */
    private void renderSkillConnection(MatrixStack matrixStack, float x1, float y1, float x2, float y2, boolean isUnlocked) {
        // 计算两点间的向量与总距离
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = MathHelper.sqrt(dx * dx + dy * dy);
        float angle = (float) Math.atan2(dy, dx);

        // 颜色配置 (摒弃导致模糊的半透明发光，使用 100% 纯色实心线)
        int borderColor = 0xFF000000; // 纯黑边框
        int innerColor = isUnlocked ? 0xFF00E5FF : 0xFF444444; // 解锁亮青色，未解锁暗灰色

        matrixStack.pushPose();

        // 将原点移动到起点，并旋转画板对准终点
        matrixStack.translate(x1, y1, 0);
        matrixStack.mulPose(Vector3f.ZP.rotation(angle));

        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuilder();

        // 画底层的黑色描边 (从 0 一直画到 length，也就是中心到中心)
        // borderWidth 为 1.5F，意味着线条总宽度为 3 像素
        float borderWidth = 1.5F;
        fillQuad(matrixStack, bufferbuilder, 0, -borderWidth, length, borderWidth, borderColor);

        // 画表层的高亮内芯
        // innerWidth 为 0.75F，稍微比黑边细一点，完美露出两边的黑线边缘
        float innerWidth = 0.75F;
        fillQuad(matrixStack, bufferbuilder, 0, -innerWidth, length, innerWidth, innerColor);

        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
        matrixStack.popPose();
    }

    /**
     * 辅助方法：使用 BufferBuilder 绘制自定义四边形
     */
    private void fillQuad(MatrixStack matrixStack, BufferBuilder bufferbuilder, float x1, float y1, float x2, float y2, int color) {
        Matrix4f matrix = matrixStack.last().pose();
        float a = (float)(color >> 24 & 255) / 255.0F;
        float r = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >> 8 & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;

        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR); // 7 代表 GL_QUADS
        bufferbuilder.vertex(matrix, x1, y2, 0.0F).color(r, g, b, a).endVertex();
        bufferbuilder.vertex(matrix, x2, y2, 0.0F).color(r, g, b, a).endVertex();
        bufferbuilder.vertex(matrix, x2, y1, 0.0F).color(r, g, b, a).endVertex();
        bufferbuilder.vertex(matrix, x1, y1, 0.0F).color(r, g, b, a).endVertex();
        Tessellator.getInstance().end();
    }

    // --- 交互：限制与调整 ---
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0) {
            double totalDist = Math.hypot(mouseX - this.dragStartX, mouseY - this.dragStartY);
            if (totalDist > 5.0) {
                this.preventClickDueToDrag = true;
            }

            // 当缩小时，鼠标移动 1 像素，世界需要移动更多才能跟手；放大时同理。
            this.scrollX += dragX / zoom;
            this.scrollY += dragY / zoom;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(this.minecraft == null) return super.mouseClicked(mouseX, mouseY, button);
        ClientPlayerEntity player = this.minecraft.player;
        if (button == 0 && isMouseInViewport(mouseX, mouseY)) {
            this.preventClickDueToDrag = false; // 按下时，重置拖拽标记
            this.dragStartX = mouseX; // 记录按下那一刻的屏幕绝对坐标
            this.dragStartY = mouseY;

            // 使用映射后的坐标判断点击
            double relX = getRelativeX(mouseX);
            double relY = getRelativeY(mouseY);

            for (SkillNode node : this.categoryNodes.values()) {
                if (node.isMouseOver((int)relX, (int)relY)) {
                    clickedNode = node;
                    return true;
                }
            }
            clickedNode = null;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if(this.minecraft == null) return super.mouseReleased(mouseX, mouseY, button);
        if (button == 0) {
            ClientPlayerEntity player = this.minecraft.player;

            // 只有当按下了有效节点、且期间没有发生任何屏幕拖拽
            if (player != null && clickedNode != null && !this.preventClickDueToDrag) {
                // 确保松开鼠标时，鼠标仍然在这个节点上（符合标准 UI 体验）
                if (isMouseInViewport(mouseX, mouseY)) {
                    double relX = getRelativeX(mouseX);
                    double relY = getRelativeY(mouseY);

                    if (this.clickedNode.isMouseOver((int) relX, (int) relY)) {
                        SkillNode node = this.clickedNode; // 局部变量赋引用
                        player.getCapability(SkillProvider.SKILL_CAP).ifPresent(cap -> {
                            if (!cap.hasSkill(node.getCategory(), node.getId())) {
                                PacketHandler.sendToServer(new C2SUnlockSkillPacket(node.getCategory(), node.getId()));
                            } else {
                                PacketHandler.sendToServer(new C2SToggleSkillPacket(node.getCategory(), node.getId()));
                            }
                        });
                    }
                }
            }
            // 操作结束，彻底清空状态
            this.clickedNode = null;
            this.preventClickDueToDrag = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    // --- 交互：滚轮缩放 ---
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta > 0) {
            this.zoom = Math.min(MAX_ZOOM, this.zoom + ZOOM_STEP);
        } else if (delta < 0) {
            this.zoom = Math.max(MIN_ZOOM, this.zoom - ZOOM_STEP);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 允许原版的 ESC 键正常关闭屏幕
        if (keyCode == 256) { // 256 是 GLFW.GLFW_KEY_ESCAPE
            this.onClose();
            return true;
        }

        // 检测自定义按键：替换为你的 Mod 实际的 KeyBinding 变量名
        if (keyCode == KeyBindings.OPEN_SKILL_SCREEN.getKey().getValue()) {
            this.onClose(); // 调用原版有关闭/清理功能的 onClose 方法
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public ResourceLocation getIcon() {
        return new ResourceLocation(SkillManager.getCategoryData(this.category).icon);
    }

    private boolean isMouseInViewport(double mouseX, double mouseY) {
        int clipX = this.guiLeft + 9;
        int clipY = this.guiTop + 18;
        int clipW = this.xSize - 18;
        int clipH = this.ySize - 27;

        return mouseX >= clipX && mouseX < (clipX + clipW) && mouseY >= clipY && mouseY < (clipY + clipH);
    }
}
