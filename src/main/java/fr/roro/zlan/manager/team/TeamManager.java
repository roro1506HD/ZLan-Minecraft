package fr.roro.zlan.manager.team;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.authlib.properties.PropertyMap;
import fr.roro.zlan.ZLan;
import fr.roro.zlan.manager.Manager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.entity.Player;

/**
 * This file is a part of ZLAN project.
 *
 * @author roro1506_HD
 */
public class TeamManager implements Manager {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(PropertyMap.class, new PropertyMap.Serializer())
            .setPrettyPrinting()
            .create();

    private static final Type TEAMS_TOKEN = new TypeToken<List<GameTeam>>() {
    }.getType();

    private List<GameTeam> teams;

    @Override
    public void onEnable() {
        try {
            File file = new File(ZLan.getInstance().getDataFolder(), "teams.json");

            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
                this.teams = new ArrayList<>();
                return;
            }

            try (FileInputStream inputStream = new FileInputStream(file);
                    InputStreamReader reader = new InputStreamReader(inputStream)) {
                this.teams = GSON.fromJson(reader, TEAMS_TOKEN);
            } catch (IllegalStateException ex) {
                this.teams = new ArrayList<>();
                ZLan.getInstance().getLogger().warning("Invalid teams.json");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        try {
            File file = new File(ZLan.getInstance().getDataFolder(), "teams.json");

            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }

            try (FileOutputStream outputStream = new FileOutputStream(file);
                    OutputStreamWriter writer = new OutputStreamWriter(outputStream)) {
                GSON.toJson(this.teams, TEAMS_TOKEN, writer);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void clearTeams() {
        this.teams.clear();
    }

    public void registerTeam(GameTeam team) {
        this.teams.add(team);
    }

    public List<GameTeam> getTeams() {
        return Collections.unmodifiableList(this.teams);
    }

    public GameTeam getTeam(Player player) {
        return this.teams.stream()
                .filter(team -> team.contains(player.getUniqueId()))
                .findAny()
                .orElse(null);
    }
}
