package fr.roro.zlan.manager.command.defaults;

import fr.roro.zlan.ZLan;
import fr.roro.zlan.manager.chat.ChatManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * This file is a part of ZLAN project.
 *
 * @author roro1506_HD
 */
public class ChatOffCommand extends Command {

    public ChatOffCommand() {
        super("chatoff");
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

        if (!ZLan.getInstance().getManager(ChatManager.class).isChatEnabled()) {
            sender.sendMessage("§cLe chat est déjà retreint");
            return false;
        }

        ZLan.getInstance().getManager(ChatManager.class).setChatEnabled(false);
        Bukkit.broadcastMessage("§cLe chat est désormais désactivé !");
        return true;
    }
}
