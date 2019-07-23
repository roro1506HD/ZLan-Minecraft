package fr.roro.zlan.manager.command.defaults;

import fr.roro.zlan.ZLan;
import fr.roro.zlan.game.GameManager;
import fr.roro.zlan.game.GameState;
import fr.roro.zlan.game.player.GamePlayer;
import fr.roro.zlan.manager.chat.ChatManager;
import fr.roro.zlan.manager.scoreboard.ScoreboardManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * This file is a part of ZLAN project.
 *
 * @author roro1506_HD
 */
public class SpecCommand extends Command {

    public SpecCommand() {
        super("spec");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command is restricted to physical players.");
            return false;
        }

        if (!sender.isOp()) {
            sender.sendMessage("§cVous n'avez pas accès à cette commande.");
            return false;
        }

        if (GameManager.getInstance().getState() != GameState.WAITING) {
            sender.sendMessage("§cVous ne pouvez pas changer votre mode de jeu durant une partie.");
            return false;
        }

        GamePlayer gamePlayer = GameManager.getInstance().getPlayer((Player) sender);

        if (gamePlayer.isSpectator())
            sender.sendMessage("§eVous serez compté comme joueur durant la partie.");
        else
            sender.sendMessage("§aVous serez compté comme spectateur durant la partie.");

        gamePlayer.setSpectator(!gamePlayer.isSpectator());
        GameManager.getInstance().getAllPlayers().forEach(ZLan.getInstance().getManager(ScoreboardManager.class)::updatePlayers);
        return true;
    }
}
