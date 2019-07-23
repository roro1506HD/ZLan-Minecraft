package fr.roro.zlan.manager.command.defaults;

import fr.roro.zlan.ZLan;
import fr.roro.zlan.manager.doc.DocManager;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * This file is a part of ZLAN project.
 *
 * @author roro1506_HD
 */
public class ZRefreshCommand extends Command {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    public ZRefreshCommand() {
        super("zrefresh");
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

        sender.sendMessage("§aRafraîchissement de la whitelist...");
        EXECUTOR.execute(ZLan.getInstance().getManager(DocManager.class)::refresh);
        return true;
    }
}
