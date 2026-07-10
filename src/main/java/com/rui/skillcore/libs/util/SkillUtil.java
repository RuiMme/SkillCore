package com.rui.skillcore.libs.util;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;

public class SkillUtil {
    public static boolean hasItem(Item handItem, String itemId) {
        ResourceLocation resource = handItem.getRegistryName();
        return resource != null && resource.toString().equals(itemId);
    }

    public static void thunder(LivingEntity attacker, LivingEntity victim, float rand) {
        if(attacker.level.isClientSide) return;
        ServerWorld world = (ServerWorld) attacker.level;
        if(attacker.getRandom().nextFloat() < rand) {
            LightningBoltEntity lightning = EntityType.LIGHTNING_BOLT.create(world);
            if(lightning != null) {
                lightning.moveTo(Vector3d.atBottomCenterOf(victim.blockPosition()));
                lightning.setVisualOnly(false);
                world.addFreshEntity(lightning);
            }
        }
    }
}
