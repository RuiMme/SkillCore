package com.rui.skillcore.client.screen.skillkeybind;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.rui.skillcore.client.keys.custom.CustomKeyBind;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

import java.util.List;

public class CustomKeybindList extends ContainerObjectSelectionList<CustomKeybindList.Entry> {
    private final KeybindSettingsScreen parent;

    // 构造器新增了 leftPos (左侧起始位置) 和 listWidth (列表宽度)
    public CustomKeybindList(KeybindSettingsScreen parent, Minecraft mc, int leftPos, int listWidth, List<CustomKeyBind> binds) {
        super(mc, listWidth, parent.height, 40, parent.height - 40, 24);
        this.parent = parent;
        this.setLeftPos(leftPos); // 核心：将列表整体向右偏移，留出左侧面板空间

        // 载入当前选中的按键列表
        for (CustomKeyBind bind : binds) {
            this.addEntry(new Entry(bind));
        }
    }

    @Override
    protected int getScrollbarPosition() {
        return this.getLeft() + this.width - 6; // 滚动条贴着右侧边缘
    }

    @Override
    public int getRowWidth() {
        return this.width - 30; // 每一项的宽度自适应列表宽度
    }

    // --- 列表中的每一行 (Entry) ---
    public class Entry extends ContainerObjectSelectionList.Entry<Entry> {
        private final CustomKeyBind bind;
        private final Button changeButton;
        private final Button resetButton;

        public Entry(CustomKeyBind bind) {
            this.bind = bind;

            this.changeButton = new Button(0, 0, 100, 20, TextComponent.EMPTY, (btn) -> {
                CustomKeybindList.this.parent.activeBind = bind;
            });

            this.resetButton = new Button(0, 0, 50, 20, new TextComponent("重置"), (btn) -> {
                bind.reset();
                if (CustomKeybindList.this.parent.activeBind == bind) {
                    CustomKeybindList.this.parent.activeBind = null;
                }
            });
        }

        @Override
        public void render(PoseStack matrixStack, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTicks) {
            // 1. 左侧对齐：渲染描述文字
            Minecraft.getInstance().font.drawShadow(matrixStack, this.bind.getDescription(), left, top + 6, 0xFFFFFF);

            // 2. 右侧对齐：自适应按钮位置
            this.resetButton.x = left + width - 50;
            this.changeButton.x = left + width - 160;

            this.resetButton.y = top;
            this.changeButton.y = top;

            // ★ 核心改动：获取冲突状态 ★
            boolean isConflicting = this.bind.hasConflict();

            // 设置按钮文本
            Component btnText;
            if (CustomKeybindList.this.parent.activeBind == this.bind) {
                // 如果玩家正在等待输入该按键，显示黄色
                btnText = new TextComponent("> ")
                        .append(this.bind.getDisplayName().copy().withStyle(ChatFormatting.YELLOW))
                        .append(" <").withStyle(ChatFormatting.YELLOW);
            } else {
                // 没在等待输入的情况下：
                if (isConflicting) {
                    // 如果存在冲突，字变红！
                    btnText = this.bind.getDisplayName().copy().withStyle(ChatFormatting.RED);
                } else {
                    // 正常状态，显示默认白色字体
                    btnText = this.bind.getDisplayName();
                }
            }

            this.changeButton.setMessage(btnText);

            // 渲染按钮
            this.changeButton.render(matrixStack, mouseX, mouseY, partialTicks);
            this.resetButton.active = !this.bind.isDefault();
            this.resetButton.render(matrixStack, mouseX, mouseY, partialTicks);
        }
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (this.changeButton.mouseClicked(mouseX, mouseY, button)) return true;
            return this.resetButton.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            this.changeButton.mouseReleased(mouseX, mouseY, button);
            this.resetButton.mouseReleased(mouseX, mouseY, button);
            return super.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return ImmutableList.of(this.changeButton, this.resetButton);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return ImmutableList.of(this.changeButton, this.resetButton);
        }
    }
}
