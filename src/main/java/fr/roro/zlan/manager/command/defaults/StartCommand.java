package fr.roro.zlan.manager.command.defaults;

import fr.roro.zlan.game.GameManager;
import fr.roro.zlan.game.GameState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * This file is a part of ZLAN project.
 *
 * @author roro1506_HD
 */
public class StartCommand extends Command {

    public StartCommand() {
        super("start");
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
            sender.sendMessage("§cUne partie est déjà en cours !");
            return true;
        }

        sender.sendMessage("§aTentative de démarrage de la partie...");
        GameManager.getInstance().startGame();
        sender.sendMessage("§aVous avez démarré la partie !");
        return true;
    }
}
