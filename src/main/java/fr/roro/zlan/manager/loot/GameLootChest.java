package fr.roro.zlan.manager.loot;

import fr.roro.zlan.ZLan;
import fr.roro.zlan.game.GameManager;
import fr.roro.zlan.manager.entity.EntityManager;
import fr.roro.zlan.manager.loot.entity.GameLootArmorStand;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.server.v1_8_R3.EnumParticle;
import net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

/**
 * This file is a part of ZLAN project.
 *
 * @author roro1506_HD
 */
public class GameLootChest implements Runnable {

    private static final Random     RANDOM = new Random();
    private static final GameLoot[] LOOTS;

    private final Location                location;
    private final BukkitTask              task;
    private final long                    startTimestamp;
    private final CompletableFuture<Void> future;

    private double theta;

    GameLootChest(Location location) {
        this.location = location.clone();
        this.task = Bukkit.getScheduler().runTaskTimer(ZLan.getInstance(), this, 1L, 1L);
        this.startTimestamp = System.currentTimeMillis();
        this.future = new CompletableFuture<>();

        location.setY(location.getY() + 50);

        ZLan.getInstance().getManager(EntityManager.class).setSpawnRestricted(false);

        CraftWorld world = (CraftWorld) location.getWorld();
        Slime leashHolder = world.spawn(location.clone().add(-0.5, 1, 0), Slime.class);
        List<Chicken> parachute = new ArrayList<>();
        GameLootArmorStand leashHolderVehicle = new GameLootArmorStand(world.getHandle());
        GameLootArmorStand holder = new GameLootArmorStand(world.getHandle(), () -> this.future.complete(null));

        this.future.whenComplete((aVoid, throwable) -> {
            this.task.cancel();
            leashHolder.remove();
            parachute.forEach(Entity::remove);

            Block chestBlock = this.location.getBlock();
            chestBlock.setType(Material.CHEST);

            ZLan.getInstance().getManager(LootManager.class).registerLootChest(chestBlock);

            this.getRandomLoot().placeItems(((Chest) chestBlock.getState()).getBlockInventory());

            for (int i = 0; i < 40; i++) {
                double angle = 2 * Math.PI * i / 40;
                double x = Math.cos(angle) * 1.5;
                double z = Math.sin(angle) * 1.5;
                Location particleLocation = this.location.clone().add(x, 0, z);

                this.sendParticle(particleLocation, new Vector(0, 1, 0));
                this.sendParticle(particleLocation,
                        particleLocation.clone().subtract(this.location).toVector().normalize());
            }
        });

        holder.setLocation(location.getX(), location.getY(), location.getZ(), 0, 0);
        leashHolderVehicle.setLocation(location.getX() + 0.75, location.getY() + 0.75, location.getZ() - 0.5, 0, 0);

        ArmorStand chest = world.addEntity(holder, SpawnReason.CUSTOM);
        chest.setMetadata("invincible", new FixedMetadataValue(ZLan.getInstance(), true));
        chest.setHelmet(new ItemStack(Material.CHEST));
        chest.setVisible(false);

        ArmorStand holderVehicle = world.addEntity(leashHolderVehicle, SpawnReason.CUSTOM);
        holderVehicle.setPassenger(leashHolder);
        holderVehicle.setVisible(false);
        holderVehicle.setMetadata("invincible", new FixedMetadataValue(ZLan.getInstance(), true));

        leashHolder.setSize(1);
        leashHolder.setMetadata("invincible", new FixedMetadataValue(ZLan.getInstance(), true));
        leashHolder.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 666666, 255, true, false));

        for (int i = 0; i < 15; i++) {
            Chicken chicken = world.spawn(location.clone().add(randomDouble(), 5, randomDouble()), Chicken.class);
            chicken.setLeashHolder(leashHolder);
            chicken.setMetadata("invincible", new FixedMetadataValue(ZLan.getInstance(), true));
            parachute.add(chicken);
        }

        ZLan.getInstance().getManager(EntityManager.class).setSpawnRestricted(true);

        TextComponent component = new TextComponent(TextComponent.fromLegacyText("§eUn poulet stratégique est apparu en x=§6" + location.getBlockX() + "§e, z=§6" + location.getBlockZ() + "§e !"));

        component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ()));
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("§eCliquez pour vous téléporter à ce drop !")));

        Bukkit.getOnlinePlayers().stream()
                .map(GameManager.getInstance()::getPlayer)
                .forEach(tempPlayer -> {
                    tempPlayer.getPlayer().playSound(location, Sound.FIREWORK_BLAST, 1000.0F, 0.5F);
                    if (tempPlayer.isSpectator())
                        tempPlayer.getPlayer().spigot().sendMessage(component);
                    else
                        tempPlayer.getPlayer().sendMessage(component.toLegacyText());
                });
    }

    @Override
    public void run() {
        if(this.location.getBlock().getType().isSolid())
            this.location.add(0, 1, 0);

        for (double d = 0.0D; d <= 2 * Math.PI; d += Math.PI / 2) {
            Location particleLocation = this.location.clone()
                    .add(Math.cos(this.theta + d) * 0.5, 0.1, Math.sin(this.theta + d) * 0.5);
            Location reversedParticleLocation = this.location.clone()
                    .add(Math.cos(-this.theta + d) * 0.5, 0.1, Math.sin(-this.theta + d) * 0.5);

            this.sendParticle(particleLocation,
                    particleLocation.clone().subtract(this.location).toVector().normalize());
            this.sendParticle(reversedParticleLocation,
                    reversedParticleLocation.clone().subtract(this.location).toVector().normalize());
        }

        this.theta += Math.PI / 40.0D;

        if (System.currentTimeMillis() - this.startTimestamp > 30000)
            this.future.complete(null);
    }

    private GameLoot getRandomLoot() {
        double random = RANDOM.nextDouble();

        if (random <= 0.16)
            return LOOTS[0];
        else if (random <= 0.32)
            return LOOTS[1];
        else if (random <= 0.48)
            return LOOTS[2];
        else if (random <= 0.64)
            return LOOTS[3];
        else if (random <= 0.80)
            return LOOTS[4];
        else
            return LOOTS[5];
    }

    private double randomDouble() {
        return Math.random() < 0.5D ? ((1.0D - Math.random()) * 3.0D - 1.5D) : (Math.random() * 3.0D - 1.5D);
    }

    private void sendParticle(Location location, Vector vector) {
        location.getWorld().getNearbyEntities(location, 64.0D, 64.0D, 64.0D).stream()
                .filter(Player.class::isInstance)
                .map(CraftPlayer.class::cast)
                .forEach(player -> player.getHandle().playerConnection.sendPacket(
                        new PacketPlayOutWorldParticles(EnumParticle.FLAME, true, (float) location.getX(),
                                (float) location.getY(), (float) location.getZ(), (float) vector.getX(),
                                (float) vector.getY(), (float) vector.getZ(), 0.5F, 0)));
    }

    static {
        LOOTS = new GameLoot[6];

        // Potion de Speed 2 (1:30) + Golden Apple
        LOOTS[0] = new GameLoot(new ItemStack(Material.POTION, 1, (short) 8226), new ItemStack(Material.GOLDEN_APPLE));

        // Potion de Jump Boost (3:00) + Golden Apple
        LOOTS[1] = new GameLoot(new ItemStack(Material.POTION, 1, (short) 8235), new ItemStack(Material.GOLDEN_APPLE));

        ItemStack item = new ItemStack(Material.POTION, 4, (short) 8227);
        ItemMeta itemMeta = item.getItemMeta();
        ((PotionMeta) itemMeta).clearCustomEffects();
        ((PotionMeta) itemMeta).addCustomEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 0), true);
        item.setItemMeta(itemMeta);

        // Potions d'absorption (1:00)
        LOOTS[2] = new GameLoot(new ItemStack(Material.POTION, 1, (short) 8235));

        item = new ItemStack(Material.POTION, 4, (short) 16420);
        itemMeta = item.getItemMeta();
        ((PotionMeta) itemMeta).clearCustomEffects();
        ((PotionMeta) itemMeta).addCustomEffect(new PotionEffect(PotionEffectType.POISON, 2400, 0), true);
        item.setItemMeta(itemMeta);

        // Potions de poison (0:15)
        LOOTS[3] = new GameLoot(item);

        // Plastron en diamant
        LOOTS[4] = new GameLoot(new ItemStack(Material.DIAMOND_CHESTPLATE));

        item = new ItemStack(Material.BONE);
        itemMeta = item.getItemMeta();
        itemMeta.setDisplayName("§c§lBON TOUTOU ! :)");
        item.setItemMeta(itemMeta);

        // Os renommé "Bon toutou"
        LOOTS[5] = new GameLoot(item);
    }
}