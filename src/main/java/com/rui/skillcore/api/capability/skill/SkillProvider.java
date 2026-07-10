package com.rui.skillcore.api.capability.skill;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SkillProvider implements ICapabilitySerializable<CompoundNBT> {
    @CapabilityInject(ISkillData.class)
    public static final Capability<ISkillData> SKILL_CAP = null;

    private final ISkillData instance = new SkillData();
    private final LazyOptional<ISkillData> optional = LazyOptional.of(() -> instance);

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return cap == SKILL_CAP ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundNBT serializeNBT() {
        return (CompoundNBT) SKILL_CAP.getStorage().writeNBT(SKILL_CAP, this.instance, null);
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        SKILL_CAP.getStorage().readNBT(SKILL_CAP, this.instance, null, nbt);
    }
}
