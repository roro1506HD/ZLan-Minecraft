package fr.roro.zlan.manager.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.roro.zlan.ZLan;
import fr.roro.zlan.manager.Manager;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import org.bukkit.Bukkit;

/**
 * This file is a part of ZLAN project.
 *
 * @author roro1506_HD
 */
public class ConfigManager implements Manager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ConfigEntry configEntry;

    @Override
    public void onEnable() {
        try {
            File configFile = new File(ZLan.getInstance().getDataFolder(), "config.json");

            if (!configFile.exists()) {
                configFile.getParentFile().mkdirs();
                configFile.createNewFile();
                Bukkit.getServer().shutdown();
                return;
            }

            try (FileInputStream inputStream = new FileInputStream(configFile);
                    InputStreamReader reader = new InputStreamReader(inputStream)) {
                this.configEntry = GSON.fromJson(reader, ConfigEntry.class);
            } catch (IllegalStateException ex) {
                Bukkit.shutdown();
                ZLan.getInstance().getLogger().warning("Invalid config.json");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Bukkit.shutdown();
        }
    }

    public String getGoogleDocumentId() {
        return this.configEntry.getGoogleDocumentId();
    }

    public String getGoogleDocumentTeamsRange() {
        return this.configEntry.getGoogleDocumentTeamsRange();
    }

    public String getGoogleDocumentTeamsStatsRange(int round) {
        return this.configEntry.getGoogleDocumentTeamsStatsRange(round);
    }

    public double getBorderSize() {
        return this.configEntry.getBorderSize();
    }

    public int getBorderTime() {
        return this.configEntry.getBorderTime();
    }
}
