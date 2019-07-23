package fr.roro.zlan.game.player;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;
import fr.roro.zlan.ZLan;
import fr.roro.zlan.game.GameManager;
import fr.roro.zlan.game.GameState;
import fr.roro.zlan.game.player.statistic.Statistic;
import fr.roro.zlan.game.player.statistic.StatisticType;
import fr.roro.zlan.manager.scoreboard.ScoreboardManager;
import fr.roro.zlan.manager.team.GameTeam;
import fr.roro.zlan.manager.team.TeamManager;
import fr.roro.zlan.manager.world.WorldManager;
import fr.roro.zlan.util.ScoreboardSign;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.server.v1_8_R3.*;
import net.minecraft.server.v1_8_R3.PacketPlayOutTitle.EnumTitleAction;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

/**
 * This file is a part of ZLAN project.
 *
 * @author roro1506_HD
 */
public class GamePlayer {

    private static final Gson   GSON         = new GsonBuilder().setPrettyPrinting().create();
    private static final Random RANDOM       = new Random();
    private static final int[]  PLACE_SCORES = {500, 450, 445, 435, 425, 415, 405, 395, 385, 375, 365, 360, 350, 340,
            330, 320, 310, 300, 290, 280, 270, 260, 250, 240, 230, 220, 210, 200, 190, 180, 170, 160, 150, 149, 148,
            147, 146, 145, 144, 143, 142, 141, 140, 140, 140, 140, 140, 140, 140, 139, 138, 137, 136, 135, 130, 129,
            128, 127, 126, 125, 124, 123, 122, 121, 120, 110, 110, 110, 110, 100, 95, 90, 85, 80, 75, 70, 65, 60, 55,
            50, 49, 48, 47, 46, 45, 44, 43, 42, 41, 40};

    private final CraftPlayer                   player;
    private final Map<StatisticType, Statistic> statistics;
    private final ScoreboardSign                scoreboard;
    private final ScoreboardObjective           scoreboardObjective;
    private final ScoreboardScore               healthScore;
    private final File                          statsFile;
    private final String                        name;

    private boolean               alive;
    private boolean               spectator;
    private boolean               scoreboardInitialized;
    private Map<Integer, Integer> places;
    private Map<Integer, Integer> kills;
    private int                   minLives;
    private int                   lives;
    private int                   invulnerabilityTimer;
    private int                   invulnerability;

    public GamePlayer(Player player) {
        Scoreboard mcScoreboard = MinecraftServer.getServer().getWorld().getScoreboard();

        this.player = (CraftPlayer) player;
        this.statistics = new HashMap<>();
        this.scoreboard = new ScoreboardSign(player, "§2§lZ§f§lLAN");
        this.scoreboardObjective = new ScoreboardObjective(mcScoreboard, "spec_health",
                IScoreboardCriteria.criteria.get("dummy"));
        this.healthScore = new ScoreboardScore(mcScoreboard, this.scoreboardObjective, player.getName());
        this.statsFile = new File(ZLan.getInstance().getDataFolder().getPath().replace('\\', '/') + "/players/",
                player.getUniqueId().toString() + ".json");
        this.name = player.getName();

        this.scoreboardInitialized = false;
        this.healthScore.setScore(100);
        this.scoreboardObjective.setDisplayName("%");

        this.lives = 3;
        this.alive = GameManager.getInstance().getState() == GameState.WAITING;
        this.places = new HashMap<>();
        this.kills = new HashMap<>();

        if (this.statsFile.exists()) {
            try (FileInputStream inputStream = new FileInputStream(this.statsFile);
                    InputStreamReader reader = new InputStreamReader(inputStream)) {
                JsonObject object = new JsonParser().parse(reader).getAsJsonObject();

                if (object.has("spectator"))
                    this.spectator = object.get("spectator").getAsBoolean();

                if (object.has("minLives"))
                    this.minLives = object.get("minLives").getAsInt();

                if (object.has("places"))
                    this.places = GSON.fromJson(object.get("places"), new TypeToken<Map<Integer, Integer>>() {
                    }.getType());

                if (object.has("kills"))
                    this.kills = GSON.fromJson(object.get("kills"), new TypeToken<Map<Integer, Integer>>() {
                    }.getType());

                if (object.has("totalKills"))
                    this.getStatistic(StatisticType.TOTAL_KILLS).setValue(object.get("totalKills").getAsInt());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        ZLan.getInstance().getManager(ScoreboardManager.class).initialize(this);
    }

    public void initialize() {
        this.player.getInventory().clear();
        this.player.getInventory().setArmorContents(null);
        this.player.setMaxHealth(20.0D);
        this.player.setHealth(20.0D);
        this.player.setFoodLevel(20);
        this.player.setSaturation(20.0F);
        this.player.setExhaustion(20.0F);
        this.player.setWalkSpeed(0.2F);
        this.player.setLevel(0);
        this.player.setExp(0.0F);
        this.player.setGameMode(
                this.spectator && GameManager.getInstance().getState() != GameState.WAITING ? GameMode.SPECTATOR
                        : GameMode.ADVENTURE);
        this.player.teleport(ZLan.getInstance().getSpawnLocation());
        this.player.getActivePotionEffects().stream()
                .map(PotionEffect::getType)
                .forEach(this.player::removePotionEffect);

        if (!this.spectator && GameManager.getInstance().getState() != GameState.WAITING)
            GameManager.getInstance().getSpectators()
                    .forEach(gamePlayer -> gamePlayer.getPlayer().hidePlayer(this.player));

        this.setReducedDebugInfo(false);
    }

    public void save() {
        try {
            if (!this.statsFile.exists()) {
                this.statsFile.getParentFile().mkdirs();
                this.statsFile.createNewFile();
            }

            JsonObject object = new JsonObject();

            object.add("name", new JsonPrimitive(this.name));
            object.add("spectator", new JsonPrimitive(this.spectator));
            object.add("minLives", new JsonPrimitive(this.minLives));
            object.add("kills", GSON.toJsonTree(this.kills));
            object.add("places", GSON.toJsonTree(this.places));
            object.add("totalKills", new JsonPrimitive(this.getStatistic(StatisticType.TOTAL_KILLS).getValue()));

            PrintWriter writer = new PrintWriter(this.statsFile);
            GSON.toJson(object, writer);
            writer.flush();
            writer.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void clearStats() {
        this.kills.clear();
        this.places.clear();
        this.getStatistic(StatisticType.TOTAL_KILLS).setValue(0);

        this.save();
    }

    public void reset() {
        this.alive = true;
        this.lives = 3;
        this.invulnerability = 0;
        this.invulnerabilityTimer = 0;

        this.scoreboard.destroy();
        this.scoreboard.clearLines();

        this.scoreboardInitialized = false;
        this.sendPacket(new PacketPlayOutScoreboardObjective(this.scoreboardObjective, 1));

        ZLan.getInstance().getManager(ScoreboardManager.class).initialize(this);

        GameManager.getInstance().getAllPlayers().forEach(gamePlayer -> gamePlayer.getPlayer().showPlayer(this.player));

        this.initialize();
    }

    public void giveArmor() {
        ItemStack leatherHelmet = new ItemStack(Material.LEATHER_HELMET);
        LeatherArmorMeta itemMeta = (LeatherArmorMeta) leatherHelmet.getItemMeta();
        itemMeta.setColor(Color.GREEN);
        leatherHelmet.setItemMeta(itemMeta);

        ItemStack leatherChestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        itemMeta = (LeatherArmorMeta) leatherChestplate.getItemMeta();
        itemMeta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 1, true);
        itemMeta.setColor(Color.GREEN);
        leatherChestplate.setItemMeta(itemMeta);

        ItemStack leatherLeggings = new ItemStack(Material.LEATHER_LEGGINGS);
        itemMeta = (LeatherArmorMeta) leatherLeggings.getItemMeta();
        itemMeta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 1, true);
        itemMeta.setColor(Color.WHITE);
        leatherLeggings.setItemMeta(itemMeta);

        ItemStack leatherBoots = new ItemStack(Material.LEATHER_BOOTS);
        itemMeta = (LeatherArmorMeta) leatherBoots.getItemMeta();
        itemMeta.setColor(Color.WHITE);
        leatherBoots.setItemMeta(itemMeta);

        this.player.getInventory().setBoots(this.lives == 1 ? leatherBoots : new ItemStack(Material.IRON_BOOTS));
        this.player.getInventory()
                .setLeggings(this.lives == 3 ? new ItemStack(Material.IRON_LEGGINGS) : leatherLeggings);
        this.player.getInventory()
                .setChestplate(this.lives == 1 ? leatherChestplate : new ItemStack(Material.IRON_CHESTPLATE));
        this.player.getInventory().setHelmet(this.lives == 3 ? new ItemStack(Material.IRON_HELMET) : leatherHelmet);
    }

    public void giveStuff() {
        ItemStack stoneAxe = new ItemStack(Material.STONE_SWORD);
        ItemMeta stoneAxeMeta = stoneAxe.getItemMeta();
        stoneAxeMeta.spigot().setUnbreakable(true);
        stoneAxe.setItemMeta(stoneAxeMeta);

        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta bowMeta = bow.getItemMeta();
        bowMeta.addEnchant(Enchantment.ARROW_INFINITE, 1, false);
        bowMeta.spigot().setUnbreakable(true);
        bow.setItemMeta(bowMeta);

        this.player.getInventory().clear();

        this.player.getInventory().setItem(0, stoneAxe);
        this.player.getInventory().setItem(1, bow);
        this.player.getInventory().setItem(2, new ItemStack(Material.SAND, 32));
        this.player.getInventory().setItem(9, new ItemStack(Material.ARROW));

        this.player.getInventory().setHeldItemSlot(0);
    }

    public boolean decrementLife() {
        if (--this.lives - this.minLives != 0)
            return true;

        this.alive = false;

        int alivePlayers = GameManager.getInstance().getAlivePlayers().size();
        int place = alivePlayers + 1;
        int round = GameManager.getInstance().getRound();

        this.sendTitle("§cVous êtes éliminé !", "", 10, 80, 10);
        this.player.sendMessage("§aVous êtes §6#" + place + " §aavec §b" +
                this.kills.computeIfAbsent(GameManager.getInstance().getRound(), unused -> 0) + " §akill(s) !");

        this.setReducedDebugInfo(true);

        this.player.setGameMode(GameMode.ADVENTURE);
        this.player.getInventory().clear();
        this.player.getInventory().setArmorContents(null);
        this.player.teleport(ZLan.getInstance().getManager(WorldManager.class).getFinalZoneSpawn());

        this.places.put(round, place);

        this.save();

        GameManager.getInstance().getAllPlayers()
                .forEach(ZLan.getInstance().getManager(ScoreboardManager.class)::updatePlayers);

        GameManager.getInstance().getSpectators().forEach(gamePlayer -> gamePlayer.getPlayer().hidePlayer(this.player));

        if (alivePlayers == 1)
            GameManager.getInstance().finishGame();
        else if (alivePlayers == 2) {
            GameTeam firstTeam = ZLan.getInstance().getManager(TeamManager.class)
                    .getTeam(GameManager.getInstance().getAlivePlayers().get(0).getPlayer());
            GameTeam secondTeam = ZLan.getInstance().getManager(TeamManager.class)
                    .getTeam(GameManager.getInstance().getAlivePlayers().get(1).getPlayer());

            if (isSameTeam(firstTeam, secondTeam))
                GameManager.getInstance().finishGame();
        }

        return false;
    }

    public void randomTeleport() {
        World world = Bukkit.getWorld("gameworld");
        double size = world.getWorldBorder().getSize() / 3;

        Location teleportLocation;
        do {
            int x = -(int) size + RANDOM.nextInt((int) size * 2) + (int) world.getWorldBorder().getCenter().getX();
            int y = 255;
            int z = -(int) size + RANDOM.nextInt((int) size * 2) + (int) world.getWorldBorder().getCenter().getZ();

            Material blockType;
            while ((!(blockType = world.getBlockAt(x, y, z).getType()).isSolid() || blockType == Material.BARRIER) &&
                    y > 0)
                y--;

            teleportLocation = new Location(world, x + 0.5, y + 1, z + 0.5);
        } while (teleportLocation.getY() > 150);

        teleportLocation
                .setDirection(
                        GameManager.getInstance().getCenter().clone().subtract(teleportLocation.clone()).toVector()
                                .normalize());
        this.player.getPlayer().teleport(teleportLocation);
        this.player.getPlayer().setVelocity(new Vector());
    }

    public void borderTeleport() {
        World world = Bukkit.getWorld("gameworld");
        Vector direction = world.getWorldBorder().getCenter().clone().subtract(this.player.getLocation()).toVector()
                .normalize();
        Location playerLocation = this.player.getLocation().clone();

        Location teleportLocation = playerLocation.add(direction);
        int x = teleportLocation.getBlockX();
        int y = 255;
        int z = teleportLocation.getBlockZ();

        Material blockType;
        while ((!(blockType = world.getBlockAt(x, y, z).getType()).isSolid() || blockType == Material.BARRIER) &&
                y > 0)
            y--;

        teleportLocation.setY(y + 1);

        this.player.getPlayer().teleport(teleportLocation);
    }

    public void tick(int timeElapsed) {
        if (this.invulnerabilityTimer > 0) {
            this.player.setLevel(--this.invulnerabilityTimer);
            this.player.setExp(this.invulnerabilityTimer * 1.0F / (float) this.invulnerability);

            if (this.invulnerabilityTimer == 0)
                this.player.sendMessage("§cLa période d'invulnérabilité est terminée.");
        }

        this.sendActionBar(
                String.format("%1$s»%2$s»%1$s»%2$s» %3$s %2$s«%1$s«%2$s«%1$s«", timeElapsed % 2 == 0 ? "§4§l" : "§c§l",
                        timeElapsed % 2 == 0 ? "§c§l" : "§4§l",
                        String.format("§6§l%d BARRE%2$s DE VIE RESTANTE%2$s", this.lives - this.minLives,
                                this.lives - this.minLives == 1 ? "" : "S")));
    }

    public void updateHealth() {
        this.healthScore.setScore(
                (int) Math.round(100 * (this.player.getHealth() + this.player.getHandle().getAbsorptionHearts()) / 20));
    }

    public void updateShownHealth() {
        if (!this.scoreboardInitialized) {
            this.sendPacket(new PacketPlayOutScoreboardObjective(this.scoreboardObjective, 0));
            this.sendPacket(new PacketPlayOutScoreboardDisplayObjective(2, this.scoreboardObjective));
            this.scoreboardInitialized = true;
        }

        for (GamePlayer gamePlayer : GameManager.getInstance().getAlivePlayers())
            this.sendPacket(new PacketPlayOutScoreboardScore(gamePlayer.healthScore));
    }

    public void sendTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        this.sendPacket(new PacketPlayOutTitle(EnumTitleAction.TIMES, null, fadeIn, stay, fadeOut));
        this.sendPacket(new PacketPlayOutTitle(EnumTitleAction.SUBTITLE, new ChatComponentText(subtitle)));
        this.sendPacket(new PacketPlayOutTitle(EnumTitleAction.TITLE, new ChatComponentText(title)));
    }

    public void sendActionBar(String message) {
        this.sendPacket(new PacketPlayOutChat(new ChatComponentText(message), (byte) 2));
    }

    public void setReducedDebugInfo(boolean reduced) {
        this.sendPacket(new PacketPlayOutEntityStatus(this.player.getHandle(), reduced ? (byte) 22 : (byte) 23));
    }

    public void sendPacket(Packet<?> packet) {
        this.player.getHandle().playerConnection.sendPacket(packet);
    }

    public void sendMessage(String message) {
        this.player.sendMessage(message);
    }

    private boolean isSameTeam(GameTeam teamOne, GameTeam teamTwo) {
        return (teamOne == null && teamTwo == null) || (teamOne != null && teamOne.equals(teamTwo));
    }

    public <T, R extends Statistic<T>> R getStatistic(StatisticType<T, R> statisticType) {
        //noinspection unchecked
        return (R) this.statistics.computeIfAbsent(statisticType, StatisticType::getDefaultStatistic);
    }

    public boolean isInvulnerable() {
        return this.invulnerabilityTimer > 0;
    }

    public void setInvulnerable(int seconds) {
        this.invulnerabilityTimer = seconds;
        this.invulnerability = seconds;
    }

    public ScoreboardScore getHealthScore() {
        return this.healthScore;
    }

    public boolean isSpectator() {
        return this.spectator;
    }

    public void setSpectator(boolean spectator) {
        this.spectator = spectator;
    }

    public boolean isAlive() {
        return this.alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public ScoreboardSign getScoreboard() {
        return this.scoreboard;
    }

    public int getPlace(int round) {
        return this.places.getOrDefault(round, 0);
    }

    public void setPlace(int place) {
        this.places.put(GameManager.getInstance().getRound(), place);
    }

    public int getKills(int round) {
        return this.kills.getOrDefault(round, 0);
    }

    public void incrementKills() {
        int round = GameManager.getInstance().getRound();
        this.kills.put(round, this.kills.getOrDefault(round, 0) + 1);
    }

    public int getMinLives() {
        return this.minLives;
    }

    public void setMinLives(int minLives) {
        this.minLives = minLives;
    }

    public CraftPlayer getPlayer() {
        return this.player;
    }
}
