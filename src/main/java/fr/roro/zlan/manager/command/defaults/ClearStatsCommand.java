package fr.roro.zlan.manager.command.defaults;

import fr.roro.zlan.ZLan;
import fr.roro.zlan.game.GameManager;
import fr.roro.zlan.game.player.GamePlayer;
import java.io.File;
import java.util.Arrays;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * This file is a part of ZLAN project.
 *
 * @author roro1506_HD
 */
public class ClearStatsCommand extends Command {

    public ClearStatsCommand() {
        super("clearstats");
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


        File folder = new File(ZLan.getInstance().getDataFolder().getPath().replace('\\', '/') + "/players/");

        Arrays.stream(folder.listFiles()).forEach(File::delete);

        GameManager.getInstance().getAllPlayers().forEach(GamePlayer::clearStats);
        sender.sendMessage("§aLes stats ont été reset");
        return true;
    }
}
