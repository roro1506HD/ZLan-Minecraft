package fr.roro.zlan.game;

import fr.roro.zlan.ZLan;
import fr.roro.zlan.manager.loot.LootManager;
import fr.roro.zlan.manager.scoreboard.ScoreboardManager;
import fr.roro.zlan.manager.world.WorldManager;

/**
 * This file is a part of ZLAN project.
 *
 * @author roro1506_HD
 */
class GameLoop implements Runnable {

    private byte delta = 1;

    GameLoop() {
    }

    @Override
    public void run() {
        // Shift 1 bit to the left
        this.delta <<= 1;

        // Check for world border
        ZLan.getInstance().getManager(WorldManager.class).checkBorderCollision();

        // Check if delta is equals to 16 (0b10000) which is equivalent to one second as the loop executes every 5 ticks
        if ((this.delta & 0x10) != 0x10)
            return;

        // Reset delta
        this.delta = 1;

        // Increment and get game time
        int timeElapsed = GameManager.getInstance().increaseTimeElapsed();

        // Tick players & update timer
        GameManager.getInstance().getAlivePlayers().forEach(player -> player.tick(timeElapsed));
        GameManager.getInstance().getAllPlayers().forEach(ZLan.getInstance().getManager(ScoreboardManager.class)::updateTimer);

        // Tick world border
        ZLan.getInstance().getManager(WorldManager.class).computeZone();

        // Check for Loot Chest, and drop one if needed
        if (timeElapsed % 60 == 0)
            ZLan.getInstance().getManager(LootManager.class).dropLootChest();
    }
}
