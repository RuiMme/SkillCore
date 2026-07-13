package com.rui.skillcore.libs.util;

import net.minecraft.world.entity.player.Player;

public class LevelUtil {
    public static void consumePlayerLevel(Player player, int level) {
        player.giveExperienceLevels(-level);
    }
}
