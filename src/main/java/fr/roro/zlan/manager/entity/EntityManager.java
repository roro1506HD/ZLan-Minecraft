package fr.roro.zlan.manager.entity;

import fr.roro.zlan.manager.Manager;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.hanging.HangingBreakEvent;

/**
 * This file is a part of ZLAN project.
 *
 * @author roro1506_HD
 */
public class EntityManager implements Manager {

    private final List<Function<Entity, Boolean>> entitySpawnRestrictions = new ArrayList<>();

    private boolean spawnRestricted = true;

    public EntityManager() {
        this.entitySpawnRestrictions.add(Creeper.class::isInstance);
        this.entitySpawnRestrictions.add(Spider.class::isInstance);
        this.entitySpawnRestrictions.add(Witch.class::isInstance);
        this.entitySpawnRestrictions.add(Zombie.class::isInstance);

        this.entitySpawnRestrictions
                .add(entity -> entity instanceof Item && ((Item) entity).getItemStack().getType() != Material.LEASH);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Arrow)
            event.getEntity().remove();
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (!this.spawnRestricted)
            return;

        Entity entity = event.getEntity();
        for (Function<Entity, Boolean> spawnRestriction : this.entitySpawnRestrictions)
            if (spawnRestriction.apply(entity)) {
                event.setCancelled(false);
                return;
            }

        event.setCancelled(true);
    }

    @EventHandler
    public void onHangingBreak(HangingBreakEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof ItemFrame || event.getEntity() instanceof LeashHitch ||
                (event.getEntity().hasMetadata("invincible") && event.getCause() != DamageCause.VOID))
            event.setCancelled(true);
    }

    public void setSpawnRestricted(boolean spawnRestricted) {
        this.spawnRestricted = spawnRestricted;
    }
}
