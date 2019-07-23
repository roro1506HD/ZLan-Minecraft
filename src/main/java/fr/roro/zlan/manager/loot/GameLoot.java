package fr.roro.zlan.manager.loot;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * This file is a part of ZLAN project.
 *
 * @author roro1506_HD
 */
class GameLoot {

    private final ItemStack[] items;

    GameLoot(ItemStack... items) {
        this.items = items;
    }

    void placeItems(Inventory inventory) {
        switch (this.items.length) {
            case 1:
                inventory.setItem(13, this.items[0]);
                break;
            case 2:
                inventory.setItem(12, this.items[0]);
                inventory.setItem(14, this.items[1]);
                break;
            default:
                throw new UnsupportedOperationException(
                        "We do not support " + this.items.length + " items on a single LootChest.");
        }
    }
}
