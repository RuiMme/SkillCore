package com.rui.skillcore.client.screen.skill.tabs;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

public abstract class AbstractSkillScreen extends Screen {
    protected AbstractSkillScreen(ITextComponent p_i51108_1_) {
        super(p_i51108_1_);
    }
    abstract public ResourceLocation getIcon();
}
