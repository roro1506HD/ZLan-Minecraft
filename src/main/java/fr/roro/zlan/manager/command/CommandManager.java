package fr.roro.zlan.manager.command;

import fr.roro.zlan.ZLan;
import fr.roro.zlan.manager.Manager;
import fr.roro.zlan.manager.command.defaults.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.bukkit.Bukkit;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;

/**
 * This file is a part of ZLAN project.
 *
 * @author roro1506_HD
 */
public class CommandManager implements Manager {

    @Override
    public void onEnable() {
        try {
            Field commandMapField = CraftServer.class.getDeclaredField("commandMap");
            commandMapField.setAccessible(true);

            Field modifiers = Field.class.getDeclaredField("modifiers");
            modifiers.setAccessible(true);
            modifiers.set(commandMapField, commandMapField.getModifiers() & ~Modifier.FINAL);

            commandMapField.set(Bukkit.getServer(), new ZLanCommandMap(Bukkit.getServer()));
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        SimpleCommandMap commandMap = ((CraftServer) Bukkit.getServer()).getCommandMap();
        String prefix = ZLan.getInstance().getDescription().getName();

        commandMap.register(prefix, new ChatOnCommand());
        commandMap.register(prefix, new ChatOffCommand());
        commandMap.register(prefix, new MalusCommand());
        commandMap.register(prefix, new SpecCommand());
        commandMap.register(prefix, new StartCommand());
        commandMap.register(prefix, new ZRefreshCommand());
        commandMap.register(prefix, new SetRoundCommand());
        commandMap.register(prefix, new LootCommand());
        commandMap.register(prefix, new ClearStatsCommand());
    }
}
