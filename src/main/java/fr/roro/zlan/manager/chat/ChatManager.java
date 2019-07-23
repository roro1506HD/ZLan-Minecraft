package fr.roro.zlan.manager.chat;

import com.google.common.collect.ImmutableList;
import fr.roro.zlan.manager.Manager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * This file is a part of ZLAN project.
 *
 * @author roro1506_HD
 */
public class ChatManager implements Manager {

    private final ImmutableList<String> highlightedPlayers;

    private boolean chatEnabled = true;

    public ChatManager() {
        this.highlightedPlayers = ImmutableList.<String>builder()
                .add("ZeratoR")
                .add("Libe_")
                .build();
    }

    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        if (this.highlightedPlayers.contains(event.getPlayer().getName())) {
            event.setFormat("§c§l%s: %s");
            return;
        }

        if (!this.chatEnabled) {
            event.getPlayer().sendMessage("§cLe chat est actuellement désactivé. Veuillez réessayer ultérieurement.");
            event.setCancelled(true);
            return;
        }

        event.setFormat("§7%s: %s");
    }

    public void setChatEnabled(boolean chatEnabled) {
        this.chatEnabled = chatEnabled;
    }

    public boolean isChatEnabled() {
        return this.chatEnabled;
    }
}
