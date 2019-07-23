package fr.roro.zlan.manager.command.defaults;

import fr.roro.zlan.ZLan;
import fr.roro.zlan.game.GameManager;
import fr.roro.zlan.game.GameState;
import fr.roro.zlan.game.player.GamePlayer;
import fr.roro.zlan.manager.scoreboard.ScoreboardManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * This file is a part of ZLAN project.
 *
 * @author roro1506_HD
 */
public class SetRoundCommand extends Command {

    public SetRoundCommand() {
        super("setround");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§cVous n'avez pas accès à cette commande.");
            return false;
        }

        if (GameManager.getInstance().getState() != GameState.WAITING) {
            sender.sendMessage("§cVous ne pouvez pas changer la manche durant la partie.");
            return false;
        }

        if (args.length == 0) {
            sender.sendMessage("§cUtilisation : /setround <manche>");
            return false;
        }

        try {
            GameManager.getInstance().setRound(Integer.parseInt(args[0]));
            GameManager.getInstance().getAllPlayers().forEach(ZLan.getInstance().getManager(ScoreboardManager.class)::updateRound);
            GameManager.getInstance().getAllPlayers().forEach(ZLan.getInstance().getManager(ScoreboardManager.class)::updateStats);
        } catch (Exception ex) {
            sender.sendMessage("c pas un nombre valide");
        }
        return true;
    }
}
