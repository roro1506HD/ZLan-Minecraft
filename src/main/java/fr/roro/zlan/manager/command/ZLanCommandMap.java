package fr.roro.zlan.manager.command;

import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.craftbukkit.v1_8_R3.command.VanillaCommandWrapper;

/**
 * This file is a part of ZLAN project.
 *
 * @author roro1506_HD
 */
class ZLanCommandMap extends SimpleCommandMap {

    ZLanCommandMap(Server server) {
        super(server);
    }

    @Override
    public boolean register(String fallbackPrefix, Command command) {
        if ("minecraft".equals(fallbackPrefix) && command instanceof VanillaCommandWrapper) {
            String commandName = command.getName();
            switch (commandName.toLowerCase()) {
                case "tell":
                case "me":
                    return false;
                default:
                    break;
            }
        }
        return super.register(fallbackPrefix, command);
    }
}
