package fr.roro.zlan.manager.player;

import fr.roro.zlan.ZLan;
import fr.roro.zlan.game.GameManager;
import fr.roro.zlan.game.GameState;
import fr.roro.zlan.game.player.GamePlayer;
import fr.roro.zlan.game.player.statistic.StatisticType;
import fr.roro.zlan.manager.Manager;
import fr.roro.zlan.manager.scoreboard.ScoreboardManager;
import fr.roro.zlan.manager.team.GameTeam;
import fr.roro.zlan.manager.team.TeamManager;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.server.v1_8_R3.Tuple;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

/**
 * This file is a part of ZLAN project.
 *
 * @author roro1506_HD
 */
public class PlayerManager implements Manager {

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (Bukkit.getOnlinePlayers().size() >= 200 && Bukkit.getWhitelistedPlayers().stream()
                .map(OfflinePlayer::getUniqueId)
                .noneMatch(uuid -> uuid.equals(event.getUniqueId())))
            event.disallow(Result.KICK_WHITELIST, "§cLe serveur est plein, veuillez réessayer ultérieurement.");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        GameManager.getInstance().addPlayer(event.getPlayer()).initialize();
        event.setJoinMessage(null);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Tuple<Boolean, GamePlayer> eliminated = GameManager.getInstance().removePlayer(player);

        event.setQuitMessage(null);

        if (!eliminated.a())
            return;

        GamePlayer gamePlayer = eliminated.b();

        gamePlayer.setAlive(false);

        int alivePlayers = GameManager.getInstance().getAlivePlayers().size();

        gamePlayer.setPlace(alivePlayers + 1);
        gamePlayer.save();

        Bukkit.broadcastMessage("§c" + player.getName() + " §7s'est déconnecté et est par conséquent éliminé.");

        if (alivePlayers == 1)
            GameManager.getInstance().finishGame();
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        event.setFoodLevel(20);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;

        GamePlayer gamePlayer = GameManager.getInstance().getPlayer((Player) event.getEntity());

        if (GameManager.getInstance().getState() != GameState.IN_GAME || gamePlayer.isInvulnerable() ||
                !gamePlayer.isAlive()) {
            event.setCancelled(true);
            return;
        }

        Bukkit.getScheduler().runTask(ZLan.getInstance(), () -> {
            GameManager.getInstance().getPlayer((Player) event.getEntity()).updateHealth();
            GameManager.getInstance().getSpectators().forEach(GamePlayer::updateShownHealth);
        });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) ||
                GameManager.getInstance().getPlayer((Player) event.getEntity()).isInvulnerable())
            return;

        Player damager = null;
        if (event.getDamager() instanceof Player)
            damager = (Player) event.getDamager();
        else if (event.getDamager() instanceof Projectile &&
                ((Projectile) event.getDamager()).getShooter() instanceof Player)
            damager = (Player) ((Projectile) event.getDamager()).getShooter();

        if (damager != null && GameManager.getInstance().getPlayer(damager).isInvulnerable()) {
            GamePlayer player = GameManager.getInstance().getPlayer(damager);

            player.setInvulnerable(0);
            player.getPlayer().setExp(0.0F);
            player.getPlayer().setLevel(0);
            player.sendMessage("§cVotre période d'invulnérabilité est terminée puisque vous avez tapé quelqu'un.");
        }
    }

    @EventHandler
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;

        Bukkit.getScheduler().runTask(ZLan.getInstance(), () -> {
            GamePlayer gamePlayer = GameManager.getInstance().getPlayer((Player) event.getEntity());

            gamePlayer.updateHealth();
            GameManager.getInstance().getSpectators().forEach(GamePlayer::updateShownHealth);
        });
    }

    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        if (event.getItem().getType() == Material.POTION) {
            int slot = event.getPlayer().getInventory().getHeldItemSlot();
            Bukkit.getScheduler()
                    .runTask(ZLan.getInstance(), () -> event.getPlayer().getInventory().setItem(slot, null));
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if ((GameManager.getInstance().getState() != GameState.IN_GAME &&
                GameManager.getInstance().getState() != GameState.FINISHED) ||
                !GameManager.getInstance().getPlayer(event.getPlayer()).isSpectator() ||
                !(event.getRightClicked() instanceof Player))
            return;

        Player player = (Player) event.getRightClicked();

        if (GameManager.getInstance().getPlayer(player).isSpectator())
            return;

        Inventory inventory = Bukkit.createInventory(null, 36, "Inventaire de " + player.getName());

        inventory.setContents(player.getInventory().getContents());

        event.getPlayer().openInventory(inventory);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        GamePlayer gamePlayer = GameManager.getInstance().getPlayer(player);

        event.setDeathMessage(null);

        TextComponent deathMessage = new TextComponent(player.getName());
        deathMessage.setColor(ChatColor.RED);
        deathMessage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("§7Cliquez pour vous téléporter à §c" + player.getName()).create()));
        deathMessage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp " + player.getName()));

        if (player.getKiller() != null && player.getKiller() != player) {
            TextComponent partTwo = new TextComponent(" a été tué par ");
            partTwo.setColor(ChatColor.GRAY);
            partTwo.setHoverEvent(null);
            partTwo.setClickEvent(null);
            deathMessage.addExtra(partTwo);

            TextComponent partThree = new TextComponent(player.getKiller().getName());
            partThree.setColor(ChatColor.GREEN);
            partThree.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder("§7Cliquez pour vous téléporter à §a" + player.getKiller().getName())
                            .create()));
            partThree.setClickEvent(
                    new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp " + player.getKiller().getName()));
            deathMessage.addExtra(partThree);

            GamePlayer killer = GameManager.getInstance().getPlayer(player.getKiller());

            if (!isSameTeam(ZLan.getInstance().getManager(TeamManager.class).getTeam(player),
                    ZLan.getInstance().getManager(TeamManager.class).getTeam(killer.getPlayer()))) {
                player.getKiller()
                        .setHealth(Math.min(player.getKiller().getMaxHealth(), player.getKiller().getHealth() + 6.0D));

                killer.incrementKills();
                killer.getStatistic(StatisticType.TOTAL_KILLS).incrementValue(1);

                killer.updateHealth();
                GameManager.getInstance().getSpectators().forEach(GamePlayer::updateShownHealth);
            }

            ZLan.getInstance().getManager(ScoreboardManager.class).updateStats(killer);
        } else {
            TextComponent partTwo = new TextComponent(" est mort.");
            partTwo.setColor(ChatColor.GRAY);
            partTwo.setHoverEvent(null);
            partTwo.setClickEvent(null);
            deathMessage.addExtra(partTwo);
        }

        Bukkit.getOnlinePlayers().stream()
                .map(GameManager.getInstance()::getPlayer)
                .forEach(tempPlayer -> {
                    if (tempPlayer.isSpectator())
                        tempPlayer.getPlayer().spigot().sendMessage(deathMessage);
                    else
                        tempPlayer.getPlayer().sendMessage(deathMessage.toLegacyText());
                });

        player.setHealth(player.getMaxHealth());
        gamePlayer.setInvulnerable(10);

        boolean gaveSand = false;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack itemStack = player.getInventory().getItem(i);

            if (itemStack == null)
                continue;

            Material itemType = itemStack.getType();
            if (itemType != Material.STONE_SWORD && itemType != Material.BOW && itemType != Material.ARROW &&
                    itemType != Material.IRON_BOOTS && itemType != Material.IRON_LEGGINGS &&
                    itemType != Material.IRON_CHESTPLATE && itemType != Material.IRON_HELMET &&
                    itemType != Material.LEATHER_BOOTS && itemType != Material.LEATHER_LEGGINGS &&
                    itemType != Material.LEATHER_CHESTPLATE && itemType != Material.LEATHER_HELMET &&
                    itemType != Material.DIAMOND_CHESTPLATE) {
                player.getWorld().dropItemNaturally(player.getLocation(), itemStack);
                if (itemStack.getType() == Material.SAND && !gaveSand) {
                    gaveSand = true;
                    player.getInventory().setItem(i, new ItemStack(Material.SAND, 32));
                } else
                    player.getInventory().setItem(i, null);
            }
        }

        if (!gaveSand)
            player.getInventory().addItem(new ItemStack(Material.SAND, 32));

        Bukkit.getScheduler().runTask(ZLan.getInstance(), () -> {
            if (!gamePlayer.decrementLife())
                return;

            player.sendMessage(
                    "§cVous êtes mort, vous venez d'être téléporté dans un nouvel emplacement de la carte. Vous possédez une période d'invincibilité de §610 secondes§c.");
            gamePlayer.randomTeleport();
            gamePlayer.giveArmor();
            player.getActivePotionEffects().stream()
                    .map(PotionEffect::getType)
                    .forEach(player::removePotionEffect);

            gamePlayer.updateHealth();
            GameManager.getInstance().getSpectators().forEach(GamePlayer::updateShownHealth);
        });
    }

    private boolean isSameTeam(GameTeam teamOne, GameTeam teamTwo) {
        return (teamOne == null && teamTwo == null) || (teamOne != null && teamOne.equals(teamTwo));
    }
}
