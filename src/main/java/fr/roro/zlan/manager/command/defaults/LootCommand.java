package fr.roro.zlan.manager.command.defaults;

import fr.roro.zlan.ZLan;
import fr.roro.zlan.game.GameManager;
import fr.roro.zlan.game.GameState;
import fr.roro.zlan.manager.loot.LootManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * This file is a part of ZLAN project.
 *
 * @author roro1506_HD
 */
public class LootCommand extends Command {

    public LootCommand() {
        super("loot");
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

        if (args.length == 0) {
            sender.sendMessage("§cUtilisation : /loot <nombre>");
            return false;
        }

        if (GameManager.getInstance().getState() != GameState.IN_GAME) {
            sender.sendMessage("§cAucun partie n'est en cours !");
            return true;
        }

        try {
            for(int i = 0; i < Math.min(Integer.parseInt(args[0]), 100); i++)
                ZLan.getInstance().getManager(LootManager.class).dropLootChest();
        } catch (Exception ex) {
            sender.sendMessage("§cVous n'avez pas entré un nombre valide.");
        }
        return true;
    }
}
