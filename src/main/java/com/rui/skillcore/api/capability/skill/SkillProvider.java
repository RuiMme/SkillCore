package com.rui.skillcore.api.capability.skill;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SkillProvider implements ICapabilitySerializable<CompoundTag> {
    public static final Capability<ISkillData> SKILL_CAP = CapabilityManager.get(new CapabilityToken<ISkillData>() {});

    private final SkillData instance = new SkillData();
    private final LazyOptional<ISkillData> optional = LazyOptional.of(() -> instance);

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return cap == SKILL_CAP ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return this.instance.saveNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.instance.loadNBT(nbt);
    }
}
