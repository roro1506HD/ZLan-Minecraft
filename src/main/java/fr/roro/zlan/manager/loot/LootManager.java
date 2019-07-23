package fr.roro.zlan.manager.loot;

import fr.roro.zlan.game.GameManager;
import fr.roro.zlan.manager.Manager;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * This file is a part of ZLAN project.
 *
 * @author roro1506_HD
 */
public class LootManager implements Manager {

    private final Random      random     = new Random();
    private final List<Block> lootChests = new ArrayList<>();

    void registerLootChest(Block block) {
        this.lootChests.add(block);
    }

    public void clearLoots() {
        this.lootChests.clear();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.CHEST)
            return;

        if (GameManager.getInstance().getPlayer(event.getPlayer()).isSpectator())
            return;

        if (!this.lootChests.remove(event.getClickedBlock()))
            return;

        Location location = event.getClickedBlock().getLocation();
        TextComponent component = new TextComponent(TextComponent.fromLegacyText(
                "§3§lSpec §8» §eLe poulet stratégique en x=§6" + location.getBlockX() + "§e, z=§6" +
                        location.getBlockZ() + " §ea été ouvert par §6" + event.getPlayer().getName()));

        component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                "/tp " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ()));
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                TextComponent.fromLegacyText("§eCliquez pour vous téléporter à ce drop !")));

        Bukkit.getOnlinePlayers().stream()
                .map(GameManager.getInstance()::getPlayer)
                .forEach(tempPlayer -> {
                    if (tempPlayer.isSpectator())
                        tempPlayer.getPlayer().spigot().sendMessage(component);
                });
    }

    public void dropLootChest() {
        World world = Bukkit.getWorld("gameworld");
        double size = world.getWorldBorder().getSize() / 2 - 1;

        Location location;
        do {
            int x = -(int) size + this.random.nextInt((int) size * 2) + (int) world.getWorldBorder().getCenter().getX();
            int y = 255;
            int z = -(int) size + this.random.nextInt((int) size * 2) + (int) world.getWorldBorder().getCenter().getZ();

            Material blockType;
            while ((!(blockType = world.getBlockAt(x, y, z).getType()).isSolid() || blockType == Material.BARRIER) &&
                    y > 0)
                y--;

            location = new Location(world, x + 0.5, y + 1, z + 0.5);
        } while (location.getY() > 150);

        new GameLootChest(location);
    }
}
