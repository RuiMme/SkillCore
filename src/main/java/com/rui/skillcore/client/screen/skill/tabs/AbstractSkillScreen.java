package com.rui.skillcore.client.screen.skill.tabs;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;

public abstract class AbstractSkillScreen extends Screen {
    protected AbstractSkillScreen(TextComponent p_i51108_1_) {
        super(p_i51108_1_);
    }
    abstract public ResourceLocation getIcon();
}
