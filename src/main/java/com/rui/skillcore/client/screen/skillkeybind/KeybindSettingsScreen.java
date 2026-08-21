package com.rui.skillcore.client.screen.skillkeybind;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.rui.skillcore.client.keys.custom.CustomKeyBind;
import com.rui.skillcore.client.keys.custom.CustomKeybindRegistry;
import com.rui.skillcore.client.keys.custom.KeybindConfigManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class KeybindSettingsScreen extends Screen {
    private final Screen parentScreen;
    public CustomKeyBind activeBind = null;

    private CustomKeybindList rightPanelList;

    // 改为保存当前的“分类对象”，而不仅仅是按键列表，这样能获取到分类名称
    private CustomKeybindRegistry.KeybindCategory currentCategory;

    public KeybindSettingsScreen(Screen parentScreen) {
        super(Component.literal("自定义按键管理"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        if (this.currentCategory == null && !CustomKeybindRegistry.CATEGORIES.isEmpty()) {
            this.currentCategory = CustomKeybindRegistry.CATEGORIES.get(0);
        }

        int leftPanelWidth = 140;
        int navStartY = 45;

        // ==========================================
        // 动态生成左侧导航按钮
        // ==========================================
        for (int i = 0; i < CustomKeybindRegistry.CATEGORIES.size(); i++) {
            CustomKeybindRegistry.KeybindCategory category = CustomKeybindRegistry.CATEGORIES.get(i);
            int btnY = navStartY + (i * 25);

            Button navBtn = this.addRenderableWidget(Button.builder(Component.literal(category.name), (btn) -> {
                this.currentCategory = category;
                this.init(this.minecraft, this.width, this.height);
            }).pos(10, btnY).size(leftPanelWidth - 20, 20).build());

            navBtn.active = (this.currentCategory != category);
        }

        // ==========================================
        // 初始化右侧滚动列表
        // ==========================================
        if (this.currentCategory != null) {
            this.rightPanelList = new CustomKeybindList(this, this.minecraft, leftPanelWidth, this.width - leftPanelWidth, this.currentCategory.binds);

            this.rightPanelList.setLeftPos(leftPanelWidth);
            this.addWidget(this.rightPanelList);
        }

        // ==========================================
        // 底部功能按钮
        // ==========================================
        int bottomY = this.height - 30;
        int rightPanelWidth = this.width - leftPanelWidth;
        int padding = 12;
        int gap = 6;     
        int btnWidth = (rightPanelWidth - (padding * 2) - gap) / 2;
        int btn1X = leftPanelWidth + padding;
        int btn2X = btn1X + btnWidth + gap;

        this.addRenderableWidget(Button.builder(Component.literal("恢复当前页默认"), (button) -> {
            if (this.currentCategory != null) {
                for (CustomKeyBind bind : this.currentCategory.binds) {
                    bind.reset();
                }
            }
            this.activeBind = null;
        }).pos(btn1X, bottomY).size(btnWidth, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("完成"), (button) -> {
            KeybindConfigManager.save();
            this.minecraft.setScreen(this.parentScreen);
        }).pos(btn2X, bottomY).size(btnWidth, 20).build());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.activeBind != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                // ESC 键清空绑定
                this.activeBind.setKey(InputConstants.UNKNOWN, false, false, false);
                this.activeBind = null;
                return true;
            }

            // 判断当前按下的按键本身是否就是修饰键 (Ctrl/Shift/Alt)
            boolean isModifierKey = (keyCode == GLFW.GLFW_KEY_LEFT_CONTROL || keyCode == GLFW.GLFW_KEY_RIGHT_CONTROL
                    || keyCode == GLFW.GLFW_KEY_LEFT_SHIFT || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT
                    || keyCode == GLFW.GLFW_KEY_LEFT_ALT || keyCode == GLFW.GLFW_KEY_RIGHT_ALT);

            // 如果按下的是普通按键（如 M 键），捕获它以及当前正被按住的修饰键状态
            if (!isModifierKey) {
                boolean ctrl = Screen.hasControlDown();
                boolean shift = Screen.hasShiftDown();
                boolean alt = Screen.hasAltDown();

                this.activeBind.setKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode), ctrl, shift, alt);
                this.activeBind = null;
                return true;
            }
            // 如果玩家按住 Ctrl 正在等待按下主键，不拦截输入，继续等待
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.activeBind != null) {
            boolean ctrl = Screen.hasControlDown();
            boolean shift = Screen.hasShiftDown();
            boolean alt = Screen.hasAltDown();

            this.activeBind.setKey(InputConstants.Type.MOUSE.getOrCreate(button), ctrl, shift, alt);
            this.activeBind = null;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderDirtBackground(guiGraphics);

        if (this.rightPanelList != null) {
            this.rightPanelList.render(guiGraphics, mouseX, mouseY, partialTicks);
        }

        int leftPanelWidth = 140;
        guiGraphics.fill(leftPanelWidth - 2, 0, leftPanelWidth, this.height, 0x66000000);

        guiGraphics.drawCenteredString(this.font, this.title, leftPanelWidth / 2, 15, 0xFFFFFF);

        // 动态获取当前分类的名字作为右侧标题
        if (this.currentCategory != null) {
            String categoryTitle = "- " + this.currentCategory.name + " -";
            guiGraphics.drawCenteredString(this.font, Component.literal(categoryTitle), leftPanelWidth + (this.width - leftPanelWidth) / 2, 15, 0xAAAAAA);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void onClose() {
        KeybindConfigManager.save();
        this.minecraft.setScreen(this.parentScreen);
    }
}
