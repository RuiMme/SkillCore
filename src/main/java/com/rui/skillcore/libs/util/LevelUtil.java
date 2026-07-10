package com.rui.skillcore.libs.util;

import net.minecraft.entity.player.PlayerEntity;

public class LevelUtil {
    public static void consumePlayerLevel(PlayerEntity player, int level) {
        player.giveExperienceLevels(-level);
    }
}
