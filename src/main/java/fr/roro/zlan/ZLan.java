package fr.roro.zlan;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;
import fr.roro.zlan.game.GameManager;
import fr.roro.zlan.game.player.GamePlayer;
import fr.roro.zlan.manager.Manager;
import fr.roro.zlan.manager.block.BlockManager;
import fr.roro.zlan.manager.chat.ChatManager;
import fr.roro.zlan.manager.command.CommandManager;
import fr.roro.zlan.manager.config.ConfigManager;
import fr.roro.zlan.manager.doc.DocManager;
import fr.roro.zlan.manager.entity.EntityManager;
import fr.roro.zlan.manager.loot.LootManager;
import fr.roro.zlan.manager.player.PlayerManager;
import fr.roro.zlan.manager.scoreboard.ScoreboardManager;
import fr.roro.zlan.manager.team.TeamManager;
import fr.roro.zlan.manager.world.WorldManager;
import fr.roro.zlan.util.ScoreboardSign;
import java.io.File;
import net.minecraft.server.v1_8_R3.CraftingManager;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * This file is a part of ZLAN project.
 *
 * @author roro1506_HD
 */
public class ZLan extends JavaPlugin {

    private static ZLan instance;

    private final ClassToInstanceMap<Manager> managers = MutableClassToInstanceMap.create();

    private Location spawnLocation;

    @Override
    public void onDisable() {
        this.managers.values().forEach(Manager::onDisable);

        GameManager.getInstance().getAllPlayers().stream()
                .map(GamePlayer::getScoreboard)
                .forEach(ScoreboardSign::destroy);

        GameManager.getInstance().getAllPlayers().forEach(GamePlayer::save);
    }

    @Override
    public void onEnable() {
        // Create plugin's instance
        instance = this;

        // Check and/or generate plugin folder
        if (!super.getDataFolder().exists())
            super.getDataFolder().mkdirs();

        // Register game & managers
        new GameManager();

        this.managers.putInstance(ConfigManager.class, new ConfigManager());
        this.managers.putInstance(BlockManager.class, new BlockManager());
        this.managers.putInstance(WorldManager.class, new WorldManager());
        this.managers.putInstance(ChatManager.class, new ChatManager());
        this.managers.putInstance(CommandManager.class, new CommandManager());
        this.managers.putInstance(EntityManager.class, new EntityManager());
        this.managers.putInstance(PlayerManager.class, new PlayerManager());
        this.managers.putInstance(ScoreboardManager.class, new ScoreboardManager());
        this.managers.putInstance(LootManager.class, new LootManager());
        this.managers.putInstance(TeamManager.class, new TeamManager());
        this.managers.putInstance(DocManager.class, new DocManager());

        // Setup spawn
        this.spawnLocation = new Location(Bukkit.getWorlds().get(0), 0.5, 200.5, 0.5, 0, 0);

        // Register online players
        Bukkit.getOnlinePlayers().forEach(player -> GameManager.getInstance().addPlayer(player).initialize());

        // Register listeners
        PluginManager pluginManager = Bukkit.getPluginManager();
        this.managers.values().forEach(manager -> {
            pluginManager.registerEvents(manager, this);
            manager.onEnable();
        });

        // Check and unload world if exists
        if (Bukkit.getWorld("gameworld") != null)
            Bukkit.unloadWorld("gameworld", false);

        // Delete world file, do nothing if not existing
        try {
            FileUtils.deleteDirectory(new File("gameworld"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        CraftingManager.getInstance().getRecipes().clear();
    }

    /**
     * Gets the plugin's instance, as it extends {@link JavaPlugin} it can be used anywhere a {@link JavaPlugin} is
     * required
     *
     * @return the plugin's instance
     */
    public static ZLan getInstance() {
        return instance;
    }

    /**
     * Gets the lobby's spawn location
     *
     * @return the lobby's spawn location
     */
    public Location getSpawnLocation() {
        return this.spawnLocation;
    }

    /**
     * Gets a {@link Manager}'s instance by its {@link Class<T>}.
     *
     * @param clazz The {@link Manager}'s class
     * @return the {@link Manager}'s instance
     */
    public <T extends Manager> T getManager(Class<T> clazz) {
        return this.managers.getInstance(clazz);
    }
}
