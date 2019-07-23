package fr.roro.zlan.util;

import net.minecraft.server.v1_8_R3.BiomeBase;

/**
 * This file is a part of ZLAN project.
 *
 * @author roro1506_HD
 */
public class BiomeChanger {

    private static final BiomeBase[] BIOMES;

    public static void changeBiome(int fromId, int toId) {
        BiomeBase.getBiomes()[fromId] = BIOMES[toId];
    }

    static {
        BIOMES = new BiomeBase[BiomeBase.getBiomes().length];
        System.arraycopy(BiomeBase.getBiomes(), 0, BIOMES, 0, BIOMES.length);
    }

}
