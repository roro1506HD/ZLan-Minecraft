package fr.roro.zlan.manager.loot.entity;

import net.minecraft.server.v1_8_R3.EntityArmorStand;
import net.minecraft.server.v1_8_R3.World;

/**
 * This file is a part of ZLAN project.
 *
 * @author roro1506_HD
 */
public class GameLootArmorStand extends EntityArmorStand {

    private final Runnable callback;

    public GameLootArmorStand(World world, Runnable callback) {
        super(world);
        this.callback = callback;
    }

    public GameLootArmorStand(World world) {
        super(world);
        this.callback = null;
    }

    @Override
    public void m() {
        super.m();

        if (!this.onGround && this.motY < 0.0D)
            this.motY *= 0.6;

        if (this.onGround) {
            this.die();

            if (this.callback != null)
                this.callback.run();
        }
    }
}
