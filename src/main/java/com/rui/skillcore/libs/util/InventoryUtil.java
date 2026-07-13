package com.rui.skillcore.libs.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class InventoryUtil {
    /**
     * 获取玩家背包中是否拥有某种物品
     */
    public static boolean hasPlayerItem(Player player, Item item) {
        // 遍历玩家的主背包物品栏
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() == item) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取玩家背包中某种物品的总数量
     */
    public static int getPlayerItemCount(Player player, Item item) {
        int total = 0;
        // 遍历玩家的主背包物品栏
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() == item) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /**
     * 扣除玩家背包中指定数量的某种物品
     */
    public static void consumePlayerItem(Player player, Item item, int amount) {
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                int toRemove = Math.min(stack.getCount(), amount);
                stack.shrink(toRemove); // 缩小物品堆叠数量
                amount -= toRemove;
                if (amount <= 0) break; // 扣够了就停止
            }
        }
    }
}
