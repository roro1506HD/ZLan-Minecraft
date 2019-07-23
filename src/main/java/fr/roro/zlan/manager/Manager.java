package fr.roro.zlan.manager;

import org.bukkit.event.Listener;

/**
 * This file is a part of ZLAN project.
 *
 * @author roro1506_HD
 */
public interface Manager extends Listener {

    default void onEnable() {
    }

    default void onDisable() {
    }
}
