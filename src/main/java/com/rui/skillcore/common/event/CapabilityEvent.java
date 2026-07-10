package com.rui.skillcore.common.event;

import com.rui.skillcore.api.capability.skill.SkillProvider;
import com.rui.skillcore.api.network.PacketHandler;
import com.rui.skillcore.api.network.skill.stc.S2CSyncActiveSkillsPacket;
import com.rui.skillcore.api.network.skill.stc.S2CSyncCooldownGameTimePacket;
import com.rui.skillcore.api.network.skill.stc.S2CSyncCooldownPacket;
import com.rui.skillcore.api.network.skill.stc.S2CSyncSkillsPacket;
import com.rui.skillcore.libs.LibMisc;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LibMisc.MOD_ID)
public class CapabilityEvent {
    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof PlayerEntity) {
            event.addCapability(new ResourceLocation(LibMisc.MOD_ID, "skill_data"), new SkillProvider());
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) event.getPlayer();
            syncDataToClient(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        PlayerEntity original = event.getOriginal();
        PlayerEntity clone = event.getPlayer();

        if(!clone.level.isClientSide) {
            original.getCapability(SkillProvider.SKILL_CAP).ifPresent(oldData -> {
                clone.getCapability(SkillProvider.SKILL_CAP).ifPresent(newData -> {
                    oldData.getUnlockedSkills().forEach((key, set) -> set.forEach(resourceLocation -> newData.unlockSkill(key, resourceLocation)));
                    oldData.getActiveSkills().forEach((key, set) -> set.forEach(resourceLocation -> newData.activeSkill(key, resourceLocation)));
                    oldData.getSkillCooldownGameTime().forEach((key, set) -> set.forEach((key1, set1) -> newData.setSkillCooldownGameTime(key, key1, set1)));
                    oldData.getSkillCooldown().forEach((key, set) -> set.forEach((key1, set1) -> newData.setSkillCooldown(key, key1, set1)));
                });
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        syncDataToClient(event.getPlayer());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        syncDataToClient(event.getPlayer());
    }

    private static void syncDataToClient(PlayerEntity player) {
        // 只有服务端才能给客户端发包
        if (!player.level.isClientSide && player instanceof ServerPlayerEntity) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            // 获取玩家身上的技能 Capability
            // ⚠️ 这里假设你的 Capability 获取方法叫 getCapability，请替换为你自己 Mod 中的实际获取方式
            serverPlayer.getCapability(SkillProvider.SKILL_CAP).ifPresent(data -> {
                PacketHandler.sendToClient(serverPlayer, new S2CSyncSkillsPacket(data.getUnlockedSkills()));
                PacketHandler.sendToClient(serverPlayer, new S2CSyncActiveSkillsPacket(data.getActiveSkills()));
                PacketHandler.sendToClient(serverPlayer, new S2CSyncCooldownPacket(data.getSkillCooldown()));
                PacketHandler.sendToClient(serverPlayer, new S2CSyncCooldownGameTimePacket(data.getSkillCooldownGameTime()));
            });
        }
    }
}
