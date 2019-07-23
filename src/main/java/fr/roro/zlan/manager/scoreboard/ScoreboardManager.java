package fr.roro.zlan.manager.scoreboard;

import fr.roro.zlan.game.GameManager;
import fr.roro.zlan.game.player.GamePlayer;
import fr.roro.zlan.game.player.statistic.StatisticType;
import fr.roro.zlan.manager.Manager;

/**
 * This file is a part of ZLAN project.
 *
 * @author roro1506_HD
 */
public class ScoreboardManager implements Manager {

    public void initialize(GamePlayer player) {
        player.getScoreboard().create();
        player.getScoreboard().setLine(8, "§a");
        player.getScoreboard().setLine(7, "Durée : §600m00s");
        player.getScoreboard().setLine(6, "§a");
        player.getScoreboard().setLine(5, "Joueurs restants : §a" + GameManager.getInstance().getAlivePlayers().size());
        player.getScoreboard().setLine(4, "Manche : §3" + GameManager.getInstance().getRound());
        player.getScoreboard().setLine(3, "§a");
        player.getScoreboard().setLine(2, "Kills (§emanche§r) : §e" + player.getKills(GameManager.getInstance().getRound()));
        player.getScoreboard().setLine(1, "Kills (§ctotal§r) : §c" + player.getStatistic(StatisticType.TOTAL_KILLS).getValue());
        player.getScoreboard().setLine(0, "§a");
    }

    public void updateTimer(GamePlayer player) {
        String timeFormatted = "";
        int timeElapsed = GameManager.getInstance().getTimeElapsed();
        int hours = timeElapsed / 60 / 60;
        int minutes = timeElapsed / 60 % 60;
        int seconds = timeElapsed % 60;

        if (hours > 0)
            timeFormatted += String.format("%02dh", hours);

        timeFormatted += String.format("%02dm", minutes);

        if (hours == 0)
            timeFormatted += String.format("%02ds", seconds);

        player.getScoreboard().setLine(7, "Durée : §6" + timeFormatted);
    }

    public void updatePlayers(GamePlayer player) {
        player.getScoreboard().setLine(5, "Joueurs restants : §a" + GameManager.getInstance().getAlivePlayers().size());
    }

    public void updateRound(GamePlayer player) {
        player.getScoreboard().setLine(4, "Manche : §3" + GameManager.getInstance().getRound());
    }

    public void updateStats(GamePlayer player) {
        player.getScoreboard().setLine(2, "Kills (§emanche§r) : §e" + player.getKills(GameManager.getInstance().getRound()));
        player.getScoreboard().setLine(1, "Kills (§ctotal§r) : §c" + player.getStatistic(StatisticType.TOTAL_KILLS).getValue());
    }
}
