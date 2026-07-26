package com.rui.skillcore.client.screen.skillconfig;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;

import java.util.ArrayList;
import java.util.List;

public class SkillSettingsScreen extends Screen {
    private static final ResourceLocation GUI_TEXTURE = new ResourceLocation("skillcore", "textures/gui/skill_settings_bg.png");

    // 注册表：将所有实现了接口的面板放入这里，告别硬编码！
    public static final List<ISkillConfigPanel> REGISTERED_PANELS = new ArrayList<>();

    private ISkillConfigPanel selectedPanel = null;
    private int guiLeft, guiTop, guiWidth, guiHeight;
    private int leftWidth;

    public SkillSettingsScreen() {
        super(new StringTextComponent("Skill Settings"));
        if (!REGISTERED_PANELS.isEmpty()) {
            selectedPanel = REGISTERED_PANELS.get(0);
        }
    }

    @Override
    protected void init() {
        super.init();
        guiWidth = 340;
        guiHeight = 220;
        guiLeft = (this.width - guiWidth) / 2;
        guiTop = (this.height - guiHeight) / 2;
        leftWidth = 160;

        rebuildRightWidgets();
    }

    private void rebuildRightWidgets() {
        this.buttons.clear();
        this.children.clear();

        if (selectedPanel != null) {
            selectedPanel.clear();
        }

        // 渲染左侧列表
        int startY = guiTop + 12;
        for (int i = 0; i < REGISTERED_PANELS.size(); i++) {
            ISkillConfigPanel panel = REGISTERED_PANELS.get(i);
            this.addButton(new SkillTabButton(guiLeft + 11, startY + i * 24, leftWidth - 1, 20, panel, panel == selectedPanel, b -> {
                selectedPanel = panel;
                rebuildRightWidgets();
            }));
        }

        // 通知选中的 Panel 进行初始化，传入右侧操作区的坐标范围
        if (selectedPanel != null) {
            selectedPanel.init(this, guiLeft + leftWidth, guiTop, guiWidth - leftWidth, guiHeight);
        }
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);

        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.minecraft.getTextureManager().bind(GUI_TEXTURE);
        blit(matrixStack, guiLeft, guiTop, 0, 0, guiWidth, guiHeight, 512, 512);

        if (selectedPanel != null) {
            drawString(matrixStack, this.font, "设置: " + selectedPanel.getDisplayName(),
                    guiLeft + leftWidth + 15, guiTop + 14, 0xFFDA70D6);

            // 委托选中的面板进行渲染
            selectedPanel.render(matrixStack, mouseX, mouseY, partialTicks);
        }

        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    public void renderItemTooltip(MatrixStack matrixStack, ItemStack stack, int mouseX, int mouseY) {
        this.renderTooltip(matrixStack, stack, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (selectedPanel != null && selectedPanel.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (selectedPanel != null && selectedPanel.charTyped(codePoint, modifiers)) return true;
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (selectedPanel != null && selectedPanel.keyPressed(keyCode, scanCode, modifiers)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // 内部类：列表左侧的美化按钮
    private class SkillTabButton extends Button {
        private final ISkillConfigPanel panel;
        private final boolean isSelected;

        public SkillTabButton(int x, int y, int width, int height, ISkillConfigPanel panel, boolean isSelected, IPressable onPress) {
            super(x, y, width, height, StringTextComponent.EMPTY, onPress);
            this.panel = panel;
            this.isSelected = isSelected;
        }

        @Override
        public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            if (isSelected) {
                fill(matrixStack, this.x, this.y, this.x + this.width, this.y + this.height, 0x44FFFFFF);
                fill(matrixStack, this.x, this.y, this.x + 2, this.y + this.height, 0xFFEEEEEE);
            } else if (isHovered()) {
                fill(matrixStack, this.x, this.y, this.x + this.width, this.y + this.height, 0x22FFFFFF);
            }

            if (panel.getIcon() != null && !panel.getIcon().isEmpty()) {
                itemRenderer.renderGuiItem(panel.getIcon(), this.x + 6, this.y + 2);
            }
            int textColor = isSelected ? 0xFFFFFF : (isHovered() ? 0xDDDDDD : 0x888888);
            drawString(matrixStack, font, panel.getDisplayName(), this.x + 28, this.y + 6, textColor);
        }
    }
}
