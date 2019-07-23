package fr.roro.zlan.manager.world;

import fr.roro.zlan.ZLan;
import fr.roro.zlan.game.GameManager;
import fr.roro.zlan.manager.Manager;
import fr.roro.zlan.manager.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.util.Vector;

/**
 * This file is a part of ZLAN project.
 *
 * @author roro1506_HD
 */
public class WorldManager implements Manager {

    private double finalX;
    private double finalZ;
    private double interpolation;
    private double interpolationStep;

    @Override
    public void onEnable() {
        Bukkit.getScheduler().runTask(ZLan.getInstance(), () -> this.interpolationStep =
                1.0D / ZLan.getInstance().getManager(ConfigManager.class).getBorderTime());
    }

    public void setFinalZoneCoordinates(double finalX, double finalZ) {
        this.finalX = finalX;
        this.finalZ = finalZ;
        this.interpolation = 0.0D;

        World world = Bukkit.getWorld("gameworld");

        for (int x = -15; x < 16; x++)
            for (int y = 0; y < 4; y++)
                for (int z = -15; z < 16; z++)
                    if (y == 0 || x == -15 || z == -15 || x == 15 || z == 15)
                        world.getBlockAt((int) finalX + x, 240 + y, (int) finalZ + z).setType(Material.BARRIER);
    }

    public Location getFinalZoneSpawn() {
        return new Location(Bukkit.getWorld("gameworld"), this.finalX, 245, this.finalZ, 0, 0);
    }

    public void computeZone() {
        WorldBorder worldBorder = Bukkit.getWorld("gameworld").getWorldBorder();
        double borderSize = ZLan.getInstance().getManager(ConfigManager.class).getBorderSize();

        if (this.interpolation >= 1)
            return;

        this.interpolation += this.interpolationStep;

        worldBorder.setCenter(this.finalX * this.interpolation, this.finalZ * this.interpolation);
        worldBorder.setSize(borderSize + (20 - borderSize) * this.interpolation);
    }

    public void checkBorderCollision() {
        GameManager.getInstance().getAlivePlayers().forEach(player -> {
            Location location = player.getPlayer().getLocation();
            WorldBorder border = location.getWorld().getWorldBorder();
            double borderSize = border.getSize() / 2.0D;
            double maxX = border.getCenter().getX() + borderSize;
            double minX = border.getCenter().getX() - borderSize;
            double maxZ = border.getCenter().getZ() + borderSize;
            double minZ = border.getCenter().getZ() - borderSize;

            if (location.getX() < minX - 2 || location.getX() > maxX + 2 || location.getZ() < minZ - 2 ||
                    location.getZ() > maxZ + 2)
                player.borderTeleport();
            else if (location.getX() < minX - 0.3D || location.getX() > maxX + 0.3D || location.getZ() < minZ - 0.3D ||
                    location.getZ() > maxZ + 0.3D)
                player.getPlayer().setVelocity(
                        new Vector(border.getCenter().getX() - location.getX(), player.getPlayer().getVelocity().getY(),
                                border.getCenter().getZ() - location.getZ()).normalize());
        });
    }
}
