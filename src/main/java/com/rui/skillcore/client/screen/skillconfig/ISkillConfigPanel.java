package com.rui.skillcore.client.screen.skillconfig;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;

public interface ISkillConfigPanel {
    // 技能的唯一标识符 (例如 "void_mining")
    String getSkillId();

    // 技能的显示名称 (例如 "虚空挖掘")
    String getDisplayName();

    // 技能的代表图标
    ItemStack getIcon();

    // 初始化面板 (添加按钮、输入框等)，Screen 会传入右侧面板的边界坐标
    void init(Screen screen, int x, int y, int width, int height);

    // 渲染该面板的专属内容 (图标网格、文字等)
    void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks);

    // 鼠标点击事件
    boolean mouseClicked(double mouseX, double mouseY, int button);

    // 键盘输入事件
    boolean charTyped(char codePoint, int modifiers);
    boolean keyPressed(int keyCode, int scanCode, int modifiers);

    // 当切换到其他技能时，清除本面板的组件
    void clear();
}
