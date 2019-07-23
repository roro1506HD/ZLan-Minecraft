package fr.roro.zlan.manager.doc;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.mojang.authlib.GameProfile;
import fr.roro.zlan.ZLan;
import fr.roro.zlan.game.GameManager;
import fr.roro.zlan.game.player.OfflineGamePlayer;
import fr.roro.zlan.manager.Manager;
import fr.roro.zlan.manager.config.ConfigManager;
import fr.roro.zlan.manager.team.GameTeam;
import fr.roro.zlan.manager.team.TeamManager;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.v1_8_R3.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

/**
 * This file is a part of ZLAN project.
 *
 * @author roro1506_HD
 */
public class DocManager implements Manager {

    //@formatter:off
    private static final String       APP_NAME              = "ZLan's Minecraft Google Sheets Reader/Writer";
    private static final String       TOKENS_DIRECTORY_PATH = "plugins/ZLanMC/tokens";
    private static final String       CREDENTIALS_FILE_PATH = "/credentials.json";
    private static final List<String> SCOPES                = Collections.singletonList(SheetsScopes.SPREADSHEETS);
    private static final JsonFactory  JSON_FACTORY          = JacksonFactory.getDefaultInstance();
    //@formatter:on

    private Sheets service;

    @Override
    public void onEnable() {
        try {
            NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            this.service = new Sheets.Builder(httpTransport, JSON_FACTORY, this.getCredentials(httpTransport))
                    .setApplicationName(APP_NAME)
                    .build();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void refresh() {
        ConfigManager configManager = ZLan.getInstance().getManager(ConfigManager.class);
        String googleDocId = configManager.getGoogleDocumentId();

        if (googleDocId == null) {
            Bukkit.broadcastMessage(
                    "§6§lDoc §8» §cL'identifiant du document est invalide, impossible de rafraîchir la whitelist.");
            return;
        }

        try {
            ZLan.getInstance().getLogger().severe("[DocManager] --- Start of 'Whitelist Refresh' ---");
            Bukkit.broadcastMessage("§6§lDoc §8» §eRafraîchissement de la whitelist...");

            MinecraftServer server = MinecraftServer.getServer();
            ValueRange response = this.service.spreadsheets().values()
                    .get(googleDocId, configManager.getGoogleDocumentTeamsRange()).execute();

            Arrays.stream(server.getPlayerList().getWhitelisted())
                    .map(server.getPlayerList().getWhitelist()::a)
                    .filter(Objects::nonNull)
                    .forEach(server.getPlayerList()::removeWhitelist);

            ZLan.getInstance().getManager(TeamManager.class).clearTeams();

            int line = 2;

            for (List<Object> names : response.getValues()) {
                line++;
                String firstPlayer = names.size() == 0 ? null : (String) names.get(0);
                String secondPlayer = names.size() <= 1 ? null : (String) names.get(1);

                if (firstPlayer != null && firstPlayer.isEmpty())
                    firstPlayer = null;

                if (secondPlayer != null && secondPlayer.isEmpty())
                    secondPlayer = null;

                GameProfile firstProfile = null;
                if (firstPlayer != null) {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(firstPlayer);
                    firstProfile = new GameProfile(offlinePlayer.getUniqueId(), offlinePlayer.getName());
                    // firstProfile = server.getUserCache().getProfile(firstPlayer);
                    if (firstProfile == null)
                        ZLan.getInstance().getLogger().severe("[DocManager] Unknown username : " + firstPlayer);
                    else
                        server.getPlayerList().addWhitelist(firstProfile);
                }

                GameProfile secondProfile = null;
                if (secondPlayer != null) {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(firstPlayer);
                    secondProfile = new GameProfile(offlinePlayer.getUniqueId(), offlinePlayer.getName());
                    // secondProfile = server.getUserCache().getProfile(secondPlayer);
                    if (secondProfile == null)
                        ZLan.getInstance().getLogger().severe("[DocManager] Unknown username : " + secondPlayer);
                    else
                        server.getPlayerList().addWhitelist(secondProfile);
                }

                ZLan.getInstance().getManager(TeamManager.class)
                        .registerTeam(new GameTeam(line, firstProfile, secondProfile));
            }

            Bukkit.broadcastMessage("§6§lDoc §8» §eLa whitelist a été rafraîchie ! §7(" +
                    server.getPlayerList().getWhitelisted().length + " joueurs whitelistés)");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        ZLan.getInstance().getLogger().severe("[DocManager] --- End of 'Whitelist Refresh' ---");
    }

    public void pushRoundStats() {
        ConfigManager configManager = ZLan.getInstance().getManager(ConfigManager.class);
        String googleDocId = configManager.getGoogleDocumentId();

        if (googleDocId == null) {
            Bukkit.broadcastMessage(
                    "§6§lDoc §8» §cL'identifiant du document est invalide, impossible d'écrire les statistiques de la manche.");
            return;
        }

        try {
            ZLan.getInstance().getLogger().severe("[DocManager] --- Start of 'round stats push' ---");
            Bukkit.broadcastMessage("§6§lDoc §8» §eEnvoi des statistiques de la manche en cours...");

            int round = GameManager.getInstance().getRound();
            List<GameTeam> teams = ZLan.getInstance().getManager(TeamManager.class).getTeams();
            Object[][] stats = new Object[teams.size()][4];

            for (GameTeam team : teams) {
                int line = team.getDocLine() - 3;
                GameProfile[] members = team.getRawMembers();

                if (members[0] != null) {
                    OfflineGamePlayer player = GameManager.getInstance().getOfflinePlayer(members[0].getId());
                    stats[line][0] = player.getPlace(round);
                    stats[line][2] = player.getKills(round);
                } else {
                    stats[line][0] = -1;
                    stats[line][2] = -1;
                }

                if (members[1] != null) {
                    OfflineGamePlayer player = GameManager.getInstance().getOfflinePlayer(members[1].getId());
                    stats[line][1] = player.getPlace(round);
                    stats[line][3] = player.getKills(round);
                } else {
                    stats[line][1] = -1;
                    stats[line][3] = -1;
                }
            }

            List<List<Object>> valuesList = new ArrayList<>();

            for (Object[] array : stats)
                valuesList.add(Arrays.asList(array));

            ValueRange values = new ValueRange().setValues(valuesList);

            this.service.spreadsheets().values()
                    .update(googleDocId, configManager.getGoogleDocumentTeamsStatsRange(round), values)
                    .setValueInputOption("RAW").execute();
            Bukkit.broadcastMessage("§6§lDoc §8» §eLes statistiques de la manche ont bien été envoyées.");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        ZLan.getInstance().getLogger().severe("[DocManager] --- End of 'round stats push' ---");
    }

    private Credential getCredentials(NetHttpTransport httpTransport) throws IOException {
        InputStream inputStream = ZLan.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(inputStream));
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY,
                clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver serverReceiver = new LocalServerReceiver.Builder().setPort(8888).build();

        return new AuthorizationCodeInstalledApp(flow, serverReceiver).authorize("user");
    }
}
