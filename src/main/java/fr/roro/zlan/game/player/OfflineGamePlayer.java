package fr.roro.zlan.game.player;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import fr.roro.zlan.ZLan;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * This file is a part of ZLAN project.
 *
 * @author roro1506_HD
 */
public class OfflineGamePlayer {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private Map<Integer, Integer> places;
    private Map<Integer, Integer> kills;

    public OfflineGamePlayer(UUID uuid) {
        this.places = new HashMap<>();
        this.kills = new HashMap<>();

        File statsFile = new File(ZLan.getInstance().getDataFolder().getPath().replace('\\', '/') + "/players/",
                uuid.toString() + ".json");

        if (statsFile.exists()) {
            try (FileInputStream inputStream = new FileInputStream(statsFile);
                    InputStreamReader reader = new InputStreamReader(inputStream)) {
                JsonObject object = new JsonParser().parse(reader).getAsJsonObject();

                if (object.has("places"))
                    this.places = GSON.fromJson(object.get("places"), new TypeToken<Map<Integer, Integer>>() {
                    }.getType());

                if (object.has("kills"))
                    this.kills = GSON.fromJson(object.get("kills"), new TypeToken<Map<Integer, Integer>>() {
                    }.getType());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public int getKills(int round) {
        return this.kills.getOrDefault(round, 0);
    }

    public int getPlace(int round) {
        return this.places.getOrDefault(round, -1);
    }
}
