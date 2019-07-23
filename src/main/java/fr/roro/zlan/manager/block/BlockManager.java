package fr.roro.zlan.manager.block;

import fr.roro.zlan.ZLan;
import fr.roro.zlan.manager.Manager;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.metadata.FixedMetadataValue;

/**
 * This file is a part of ZLAN project.
 *
 * @author roro1506_HD
 */
public class BlockManager implements Manager {

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!event.getBlock().hasMetadata("placed")) {
            event.setCancelled(true);
            return;
        }

        event.getBlock().removeMetadata("placed", ZLan.getInstance());
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        event.getBlock().setMetadata("placed", new FixedMetadataValue(ZLan.getInstance(), true));
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.getTo() == Material.AIR)
            event.getBlock().removeMetadata("placed", ZLan.getInstance());
        else
            event.getBlock().setMetadata("placed", new FixedMetadataValue(ZLan.getInstance(), true));
    }
}
