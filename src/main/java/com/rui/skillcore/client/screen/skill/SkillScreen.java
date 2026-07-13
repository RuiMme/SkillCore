package com.rui.skillcore.client.screen.skill;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.rui.skillcore.api.network.PacketHandler;
import com.rui.skillcore.api.network.skill.SyncSkillsPacket;
import com.rui.skillcore.data.SkillDataLoader;
import com.rui.skillcore.client.screen.skill.manager.SkillManager;
import com.rui.skillcore.client.screen.skill.tabs.AbstractSkillScreen;
import com.rui.skillcore.client.screen.skill.tabs.DynamicSkillScreen;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SkillScreen extends Screen {
    private static final ResourceLocation WINDOW = new ResourceLocation("minecraft", "textures/gui/advancements/window.png");
    private static final ResourceLocation TABS = new ResourceLocation("minecraft", "textures/gui/advancements/tabs.png");

    private final List<AbstractSkillScreen> tabs = new ArrayList<>();
    private AbstractSkillScreen activeTab;

    // --- 翻页核心常量与变量 ---
    private static final int TABS_PER_ROW = 11; // 每行最多放 8 个
    private static final int TABS_PER_PAGE = TABS_PER_ROW * 2; // 上下两行，每页最多 16 个
    private int currentPage = 0; // 当前页码（从 0 开始）

    // --- 全局动态坐标与尺寸变量 ---
    private int xSize;
    private int ySize;
    private int guiLeft;
    private int guiTop;

    public SkillScreen() {
        super(Component.nullToEmpty(""));
        SkillDataLoader.buildNodesOnClient();
        PacketHandler.sendToServer(new SyncSkillsPacket());
    }

    @Override
    protected void init() {
        super.init();

        // 动态计算宽高，自适应屏幕
        this.xSize = this.width - 50;
        this.ySize = this.height - 80;
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;

        tabs.clear();
        List<String> categories = new ArrayList<>(SkillManager.getCategories());
        categories.sort(Comparator.comparingInt(cat -> SkillManager.getCategoryData(cat).order));
        // 遍历所有从 JSON 加载出来的分类
        for (String category : categories) {
            TextComponent tabTitle = new TextComponent(SkillManager.getCategoryData(category).title);

            DynamicSkillScreen tab = new DynamicSkillScreen(category, tabTitle);
            tab.initTab(this.width, this.height, this.minecraft, this.xSize, this.ySize, this.guiLeft, this.guiTop);
            tabs.add(tab);
        }

        // 默认打开第一个子面板
        activeTab = tabs.isEmpty() ? null : tabs.get(0);
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack); // 游戏暗色背景

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
//        this.minecraft.getTextureManager().bind(WINDOW);
        RenderSystem.setShaderTexture(0, WINDOW);
        drawNineSliceWindow(matrixStack, guiLeft, guiTop, xSize, ySize);

//        this.blit(matrixStack, guiLeft, guiTop, 0, 0, xSize, ySize);
//        drawTabs(matrixStack, mouseX, mouseY);

        if (activeTab != null) {
            activeTab.render(matrixStack, mouseX, mouseY, partialTicks);
        }

        drawTabsAndPagination(matrixStack, mouseX, mouseY, guiLeft, guiTop, xSize, ySize);

        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    /**
     * 九宫格材质拉伸算法 (完美保留原版 window.png 的边框比例和顶部标题栏)
     */
    private void drawNineSliceWindow(PoseStack ms, int x, int y, int width, int height) {
        int u = 0, v = 0;
        int imgW = 252, imgH = 140;

        int top = 22;
        int bottom = 10;
        int left = 10;
        int right = 10;

        // 画四个角
        this.blit(ms, x, y, u, v, left, top);
        this.blit(ms, x + width - right, y, u + imgW - right, v, right, top);
        this.blit(ms, x, y + height - bottom, u, v + imgH - bottom, left, bottom);
        this.blit(ms, x + width - right, y + height - bottom, u + imgW - right, v + imgH - bottom, right, bottom);

        // 画四条边
        GuiComponent.blit(ms, x + left, y, width - left - right, top, u + left, v, imgW - left - right, top, 256, 256);
        GuiComponent.blit(ms, x + left, y + height - bottom, width - left - right, bottom, u + left, v + imgH - bottom, imgW - left - right, bottom, 256, 256);
        GuiComponent.blit(ms, x, y + top, left, height - top - bottom, u, v + top, left, imgH - top - bottom, 256, 256);
        GuiComponent.blit(ms, x + width - right, y + top, right, height - top - bottom, u + imgW - right, v + top, right, imgH - top - bottom, 256, 256);

        // 画中心区域
        GuiComponent.blit(ms, x + left, y + top, width - left - right, height - top - bottom, u + left, v + top, imgW - left - right, imgH - top - bottom, 256, 256);
    }

    private void drawTabsAndPagination(PoseStack ms, int mouseX, int mouseY, int guiLeft, int guiTop, int xSize, int ySize) {
        int startIndex = currentPage * TABS_PER_PAGE;
        int endIndex = Math.min(startIndex + TABS_PER_PAGE, tabs.size());

        for (int i = startIndex; i < endIndex; i++) {
            AbstractSkillScreen tab = tabs.get(i);
            boolean isActive = (tab == activeTab);

            // 计算这个 Tab 在当前页里是第几个 (0~15)
            int indexInPage = i - startIndex;
            boolean isTopRow = indexInPage < TABS_PER_ROW;
            int col = indexInPage % TABS_PER_ROW;

            int tabX = guiLeft + col * 28;
            int tabY = isTopRow ? (guiTop - 28) : (guiTop + ySize - 4); // 顶部栏在窗口上，底部栏在窗口下

            // 计算材质 UV（利用原版材质：顶部没选中0/0，选中0/32；底部没选中84/0，选中84/32）
            int u = isTopRow ? 0 : 84;
            int v = isActive ? 32 : 0;

//            this.minecraft.getTextureManager().bind(TABS);
            RenderSystem.setShaderTexture(0, TABS);
            this.blit(ms, tabX, tabY, u, v, 28, 32);
            // 图标渲染稍微偏移对齐
            int iconOffset = isTopRow ? 9 : 7;
            Item item = ForgeRegistries.ITEMS.getValue(tab.getIcon());
            if(item != null && !item.equals(Items.AIR)) {
                this.itemRenderer.renderGuiItem(new ItemStack(item), tabX + 6, tabY + iconOffset);
            } else {
                RenderSystem.setShaderTexture(0, tab.getIcon());
//                this.minecraft.getTextureManager().bind(tab.getIcon());
                blit(ms, tabX + 6, tabY + iconOffset, 16, 16, 0.0F, 0.0F, 40, 40, 40, 40);
            }

            // 悬停提示
            if (mouseX >= tabX && mouseX <= tabX + 28 && mouseY >= tabY && mouseY <= tabY + 32) {
                this.renderTooltip(ms, tab.getTitle(), mouseX, mouseY);
            }
        }

        // 如果总标签超过了TABS_PER_PAGE，绘制翻页按钮
        if (tabs.size() > TABS_PER_PAGE) {
            int maxPages = (int) Math.ceil((double) tabs.size() / TABS_PER_PAGE);

            // 按钮位置：放在右上角外面
            int btnX = guiLeft + xSize - 40;
            int btnY = guiTop - 18;

            // 绘制页码文本 "1/2"
            String pageText = (currentPage + 1) + "/" + maxPages;
            this.font.drawShadow(ms, pageText, btnX, btnY, 0xFFFFFF);

            // 绘制左右翻页小三角符号 (可以用字符表示，也可以自己画材质，这里用最简单的字符)
            boolean hoverLeft = mouseX >= btnX - 15 && mouseX <= btnX - 5 && mouseY >= btnY && mouseY <= btnY + 10;
            boolean hoverRight = mouseX >= btnX + 20 && mouseX <= btnX + 30 && mouseY >= btnY && mouseY <= btnY + 10;

            int leftColor = (currentPage > 0) ? (hoverLeft ? 0xFFFF55 : 0xFFFFFF) : 0x555555;
            int rightColor = (currentPage < maxPages - 1) ? (hoverRight ? 0xFFFF55 : 0xFFFFFF) : 0x555555;

            this.font.drawShadow(ms, "<", btnX - 12, btnY, leftColor);
            this.font.drawShadow(ms, ">", btnX + 25, btnY, rightColor);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (activeTab != null) {
            return activeTab.mouseScrolled(mouseX, mouseY, delta);
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (activeTab != null) {
            return activeTab.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // 拦截翻页按钮点击
            if (tabs.size() > TABS_PER_PAGE) {
                int maxPages = (int) Math.ceil((double) tabs.size() / TABS_PER_PAGE);
                int btnX = guiLeft + xSize - 40;
                int btnY = guiTop - 18;

                if (mouseX >= btnX - 15 && mouseX <= btnX - 5 && mouseY >= btnY && mouseY <= btnY + 10) {
                    if (currentPage > 0) {
                        currentPage--;
                        playClickSound();
                        return true;
                    }
                }
                if (mouseX >= btnX + 20 && mouseX <= btnX + 30 && mouseY >= btnY && mouseY <= btnY + 10) {
                    if (currentPage < maxPages - 1) {
                        currentPage++;
                        playClickSound();
                        return true;
                    }
                }
            }

            // 拦截标签页点击
            int startIndex = currentPage * TABS_PER_PAGE;
            int endIndex = Math.min(startIndex + TABS_PER_PAGE, tabs.size());

            for (int i = startIndex; i < endIndex; i++) {
                int indexInPage = i - startIndex;
                boolean isTopRow = indexInPage < TABS_PER_ROW;
                int col = indexInPage % TABS_PER_ROW;

                int tabX = guiLeft + col * 28;
                int tabY = isTopRow ? (guiTop - 28) : (guiTop + ySize - 4);

                if (mouseX >= tabX && mouseX <= tabX + 28 && mouseY >= tabY && mouseY <= tabY + 32) {
                    this.activeTab = tabs.get(i);
                    playClickSound();
                    return true;
                }
            }
        }

        if (activeTab != null) {
            return activeTab.mouseClicked(mouseX, mouseY, button);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void playClickSound() {
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if(activeTab != null) {
            return activeTab.mouseReleased(mouseX, mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if(activeTab != null) {
            return activeTab.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
