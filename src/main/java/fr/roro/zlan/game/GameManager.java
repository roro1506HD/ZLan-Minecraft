package fr.roro.zlan.game;

import fr.roro.zlan.ZLan;
import fr.roro.zlan.game.player.GamePlayer;
import fr.roro.zlan.game.player.OfflineGamePlayer;
import fr.roro.zlan.game.runnable.TeleportRunnable;
import fr.roro.zlan.manager.config.ConfigManager;
import fr.roro.zlan.manager.doc.DocManager;
import fr.roro.zlan.manager.loot.LootManager;
import fr.roro.zlan.manager.scoreboard.ScoreboardManager;
import fr.roro.zlan.manager.world.WorldManager;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.server.v1_8_R3.Tuple;
import org.apache.commons.io.FileUtils;
import org.bukkit.*;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * This file is a part of FightForSub project.
 *
 * @author roro1506_HD
 */
public class GameManager {

    private static GameManager instance;

    private final Map<UUID, GamePlayer> playersByUuid = new HashMap<>();
    private final Random                random;

    private GameLoop  gameLoop;
    private GameState state;
    private int       timeElapsed;
    private int       round;
    private int       startPlayers;
    private Location  center;

    public GameManager() {
        instance = this;

        this.random = new Random();
        this.gameLoop = new GameLoop();
        this.state = GameState.WAITING;
        this.round = 1;
    }

    public void startGame() {
        File map = new File(ZLan.getInstance().getDataFolder(), "world");
        World gameWorld;

        Bukkit.broadcastMessage("§3Préparation du terrain...");

        try {
            FileUtils.copyDirectory(map, new File("gameworld"));
            gameWorld = new WorldCreator("gameworld").createWorld();

            gameWorld.setGameRuleValue("doDaylightCycle", "true");
            gameWorld.setGameRuleValue("doMobLoot", "false");
            gameWorld.setGameRuleValue("doMobSpawning", "true");
            gameWorld.setGameRuleValue("doTileDrops", "false");
            gameWorld.setGameRuleValue("keepInventory", "true");
            gameWorld.setGameRuleValue("randomTickSpeed", "0");
            gameWorld.setGameRuleValue("doFireTick", "false");
            gameWorld.setGameRuleValue("naturalRegeneration", "false");
            gameWorld.setGameRuleValue("sendCommandFeedback", "false");

            gameWorld.setDifficulty(Difficulty.PEACEFUL);
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }

        Bukkit.broadcastMessage("§3Téléportation...");

        this.state = GameState.TELEPORTING;
        this.center = new Location(gameWorld, 0, 150, 0);

        List<GamePlayer> gamePlayers = new ArrayList<>(this.playersByUuid.values());
        List<Location> locations = new ArrayList<>();

        gamePlayers.removeIf(GamePlayer::isSpectator);

        int bigCircleEntries;
        double bigCircleSize;
        int smallCircleEntries;
        double smallCircleSize;

        {
            double size = ZLan.getInstance().getManager(ConfigManager.class).getBorderSize();

            gameWorld.getWorldBorder().setSize(size);

            bigCircleSize = size / 2.0D * 0.7D;
            smallCircleSize = size / 2.0D * 0.3D;

            double bigPerimeter = bigCircleSize * 2.0D * Math.PI;
            double smallPerimeter = smallCircleSize * 2.0D * Math.PI;
            double perimeter = bigPerimeter + smallPerimeter;

            bigCircleEntries = (int) ((this.startPlayers = gamePlayers.size()) * (bigPerimeter / perimeter));
            smallCircleEntries = gamePlayers.size() - bigCircleEntries;
        }

        gamePlayers.forEach(GamePlayer::updateHealth);

        this.getSpectators().forEach(player -> {
            player.getPlayer().setGameMode(GameMode.SPECTATOR);
            player.getPlayer().teleport(this.center);
            ZLan.getInstance().getManager(ScoreboardManager.class).updateRound(player);
            player.updateShownHealth();
        });

        this.findLocation(gameWorld, locations, bigCircleEntries, bigCircleSize);
        this.findLocation(gameWorld, locations, smallCircleEntries, smallCircleSize);

        new TeleportRunnable(locations, gamePlayers, () -> {
            gamePlayers.forEach(player -> {
                player.giveStuff();
                player.giveArmor();
                player.getPlayer().setHealth(20.0D);
                player.getPlayer().setGameMode(GameMode.SURVIVAL);
                player.setInvulnerable(30);

                player.getPlayer().setWalkSpeed(0.2F);
                player.getPlayer().setFoodLevel(20);
                player.getPlayer().removePotionEffect(PotionEffectType.JUMP);
                player.getPlayer().removePotionEffect(PotionEffectType.BLINDNESS);
            });

            Bukkit.broadcastMessage("");
            Bukkit.broadcastMessage("§c§lQue le combat commence !");

            gameWorld.setDifficulty(Difficulty.NORMAL);
            gameWorld.setTime(12300L);

            ZLan.getInstance().getManager(WorldManager.class)
                    .setFinalZoneCoordinates(this.random.nextInt((int) gameWorld.getWorldBorder().getSize() / 2) + 0.5,
                            this.random.nextInt((int) gameWorld.getWorldBorder().getSize() / 2) + 0.5);

            for (Player player : Bukkit.getOnlinePlayers())
                player.playSound(player.getLocation(), Sound.ENDERDRAGON_GROWL, 50.0F, 1.0F);

            this.state = GameState.IN_GAME;
            Bukkit.getScheduler().runTaskTimer(ZLan.getInstance(), this.gameLoop, 5L, 5L);
        });
    }

    public void finishGame() {
        this.state = GameState.FINISHED;

        Bukkit.getScheduler().cancelTasks(ZLan.getInstance());

        List<GamePlayer> winners = new ArrayList<>(this.getAlivePlayers());
        int kills = winners.stream()
                .mapToInt(winner -> winner.getKills(this.round))
                .sum();

        int place = winners.size();

        for (GamePlayer winner : winners) {
            winner.setPlace(place--);
            winner.sendTitle("§6Victoire !", "", 10, 80, 10);
            winner.getPlayer().sendMessage("§aVous avez remporté cette manche avec §6" + kills + " §akills !");
            winner.getPlayer().setGameMode(GameMode.ADVENTURE);

            winner.save();
        }

        if (winners.size() == 1)
            Bukkit.broadcastMessage(
                    "§a" + winners.get(0).getPlayer().getName() + " §7vient de remporter la manche §6" + this.round +
                            " §7avec §b" + kills + " §7kills !");
        else {
            try {
                StringBuilder message = new StringBuilder();

                for (int i = 0; i < winners.size(); i++) {
                    message.append("§a").append(winners.get(i).getPlayer().getName());

                    if (i < winners.size() - 1)
                        message.append("§7, ");
                    else if (i == winners.size() - 1)
                        message.append("§7 et ");
                }

                message.append("§7 viennent de remporter la manche §6").append(this.round).append("§7 avec un total de §b")
                        .append(kills).append("§7 kills !");

                Bukkit.broadcastMessage(message.toString());
            } catch (Exception ex) {
                ex.printStackTrace();
                Bukkit.broadcastMessage("§a" +
                        winners.stream().map(GamePlayer::getPlayer).map(CraftPlayer::getName).map(""::concat)
                                .collect(Collectors.joining("§7, ")) + " §7viennent de remporter la manche §6" +
                        this.round + " §7avec un total de §b" + kills + " §7kills !");
            }
        }

        ZLan.getInstance().getManager(LootManager.class).clearLoots();
        ZLan.getInstance().getManager(DocManager.class).pushRoundStats();

        new BukkitRunnable() {
            int timer = 10;
            boolean offline = false;

            @Override
            public void run() {
                if (this.timer-- == 0 || this.offline) {
                    cancel();
                    GameManager.this.resetGame();
                    return;
                }

                for (GamePlayer winner : winners) {
                    Firework firework = winner.getPlayer().getWorld()
                            .spawn(winner.getPlayer().getLocation(), Firework.class);
                    FireworkEffect effect = FireworkEffect.builder()
                            .with(Type.values()[GameManager.this.random.nextInt(Type.values().length)]).withColor(
                                    Color.fromRGB(GameManager.this.random.nextInt(256),
                                            GameManager.this.random.nextInt(256), GameManager.this.random.nextInt(256)))
                            .flicker(GameManager.this.random.nextBoolean()).trail(GameManager.this.random.nextBoolean())
                            .build();
                    FireworkMeta meta = firework.getFireworkMeta();

                    meta.addEffect(effect);
                    firework.setFireworkMeta(meta);
                }

                this.offline = winners.stream().map(GamePlayer::getPlayer)
                        .allMatch(((Predicate<CraftPlayer>) CraftPlayer::isOnline).negate());
            }
        }.runTaskTimer(ZLan.getInstance(), 20L, 20L);
    }

    private void resetGame() {
        this.state = GameState.WAITING;
        this.round++;
        this.timeElapsed = 0;

        this.playersByUuid.values().forEach(GamePlayer::reset);
        this.playersByUuid.values().forEach(player -> {
            ZLan.getInstance().getManager(ScoreboardManager.class).updateTimer(player);
            ZLan.getInstance().getManager(ScoreboardManager.class).updatePlayers(player);
            ZLan.getInstance().getManager(ScoreboardManager.class).updateRound(player);
            ZLan.getInstance().getManager(ScoreboardManager.class).updateStats(player);
        });

        Bukkit.unloadWorld("gameworld", false);

        try {
            FileUtils.deleteDirectory(new File("gameworld"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void findLocation(World world, List<Location> locations, int circleEntries, double circleSize) {
        for (int i = 0; i < circleEntries; i++) {
            double angle = Math.toRadians(360.0D / circleEntries * i);
            double x = Math.cos(angle) * circleSize;
            double y = 255;
            double z = Math.sin(angle) * circleSize;

            Location location = this.center.clone().add(x, 0, z);

            Material blockType;
            while ((!(blockType = world.getBlockAt((int) x, (int) y, (int) z).getType()).isSolid() ||
                    blockType == Material.BARRIER) &&
                    y > 0)
                y--;

            location.setY(y + 1);
            location.setDirection(this.center.clone().subtract(location).toVector().normalize());
            locations.add(location);
        }
    }

    public GamePlayer addPlayer(Player player) {
        GamePlayer gamePlayer = this.playersByUuid
                .computeIfAbsent(player.getUniqueId(), unused -> new GamePlayer(player));

        this.playersByUuid.values().forEach(ZLan.getInstance().getManager(ScoreboardManager.class)::updatePlayers);

        return gamePlayer;
    }

    public Tuple<Boolean, GamePlayer> removePlayer(Player player) {
        if (!this.playersByUuid.containsKey(player.getUniqueId()))
            return new Tuple<>(false, null);

        GamePlayer gamePlayer = this.playersByUuid.remove(player.getUniqueId());

        gamePlayer.save();

        this.playersByUuid.values().forEach(ZLan.getInstance().getManager(ScoreboardManager.class)::updatePlayers);

        return new Tuple<>(this.state == GameState.IN_GAME && gamePlayer.isAlive(), gamePlayer);
    }

    public List<GamePlayer> getAlivePlayers() {
        return this.playersByUuid.values().stream()
                .filter(((Predicate<GamePlayer>) GamePlayer::isSpectator).negate())
                .filter(GamePlayer::isAlive)
                .collect(Collectors.toList());
    }

    public List<GamePlayer> getSpectators() {
        return this.playersByUuid.values().stream()
                .filter(GamePlayer::isSpectator)
                .collect(Collectors.toList());
    }

    public List<GamePlayer> getAllPlayers() {
        return new ArrayList<>(this.playersByUuid.values());
    }

    public GamePlayer getPlayer(Player player) {
        return this.playersByUuid.get(player.getUniqueId());
    }

    public GamePlayer getPlayer(UUID uuid) {
        return this.playersByUuid.get(uuid);
    }

    public OfflineGamePlayer getOfflinePlayer(UUID uuid) {
        return new OfflineGamePlayer(uuid);
    }

    int increaseTimeElapsed() {
        return ++this.timeElapsed;
    }

    public int getTimeElapsed() {
        return this.timeElapsed;
    }

    public GameState getState() {
        return this.state;
    }

    public int getRound() {
        return this.round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public Location getCenter() {
        return this.center;
    }

    public int getStartPlayers() {
        return this.startPlayers;
    }

    public static GameManager getInstance() {
        return instance;
    }
}
