package com.rui.skillcore.libs.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

public class SkillUtil {
    public static boolean hasItem(Item handItem, String itemId) {
        ResourceLocation resource = ForgeRegistries.ITEMS.getKey(handItem);
        return resource != null && resource.toString().equals(itemId);
    }

    public static void thunder(LivingEntity attacker, LivingEntity victim, float rand) {
        if(attacker.level().isClientSide) return;
        ServerLevel world = (ServerLevel) attacker.level();
        if(attacker.getRandom().nextFloat() < rand) {
            LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(world);
            if(lightning != null) {
                lightning.moveTo(Vec3.atBottomCenterOf(victim.blockPosition()));
                lightning.setVisualOnly(false);
                world.addFreshEntity(lightning);
            }
        }
    }
}
