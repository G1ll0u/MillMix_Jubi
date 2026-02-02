package com.jubitus.millmix.aw;

import com.jubitus.millmix.MillMixModConfig;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraftforge.fml.common.Loader;

import java.util.ArrayList;

public final class AWOverlap {

    // Try common WorldSavedData names (because yours are placeholders right now)

    private static final String[] STRUCTURE_MAP_NAMES = new String[]{
            "StructureMap",
            "structure_map",
            "structureMap",
            "aw_structure_map",
            "AWStructureMap",
            "ancientwarfare_structure_map",
            "AncientWarfareStructureMap"
    };

    private static final String[] TOWN_MAP_NAMES = new String[]{
            "TownMap",
            "town_map",
            "townMap",
            "aw_town_map",
            "AWTownMap",
            "ancientwarfare_town_map",
            "AncientWarfareTownMap"
    };

    // Cache per-world; also cache "not found" as a sentinel to avoid repeated lookups
    private static final Object NO_DATA = new Object();
    private static final java.util.WeakHashMap<World, Object> STRUCTURE_MAP_CACHE = new java.util.WeakHashMap<>();
    private static final java.util.WeakHashMap<World, Object> TOWN_MAP_CACHE = new java.util.WeakHashMap<>();

    // Reuse list (no allocations)
    private static final ThreadLocal<ArrayList<net.shadowmage.ancientwarfare.structure.gamedata.StructureEntry>> TL_LIST =
            ThreadLocal.withInitial(() -> new ArrayList<>(64));

    // One-time debug so you KNOW if it’s failing to find data
    private static boolean loggedNoStructureMap = false;
    private static boolean loggedNoTownMap = false;

    private AWOverlap() {
    }

    public static boolean intersectsAny(World world,
                                        int minX, int minY, int minZ,
                                        int maxX, int maxY, int maxZ) {

        if (world == null || world.isRemote) return false;

        // If you ALWAYS run with AW installed, you can remove this check.
        if (!isAWLoaded()) return false;

        // normalize
        if (minX > maxX) {
            int t = minX;
            minX = maxX;
            maxX = t;
        }
        if (minY > maxY) {
            int t = minY;
            minY = maxY;
            maxY = t;
        }
        if (minZ > maxZ) {
            int t = minZ;
            minZ = maxZ;
            maxZ = t;
        }

        Object sm = getStructureMap(world);
        if (sm instanceof net.shadowmage.ancientwarfare.structure.gamedata.StructureMap) {
            if (intersectsStructureMap((net.shadowmage.ancientwarfare.structure.gamedata.StructureMap) sm,
                    world, minX, minY, minZ, maxX, maxY, maxZ)) {
                return true;
            }
        }

        Object tm = getTownMap(world);
        if (tm instanceof net.shadowmage.ancientwarfare.structure.gamedata.TownMap) {
            if (intersectsTownMap((net.shadowmage.ancientwarfare.structure.gamedata.TownMap) tm,
                    minX, minY, minZ, maxX, maxY, maxZ)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isAWLoaded() {
        // AW2 often uses split modids; structure module may be different
        return Loader.isModLoaded("ancientwarfare")
                || Loader.isModLoaded("ancientwarfarestructure")
                || Loader.isModLoaded("ancientwarfarenpc")
                || Loader.isModLoaded("ancientwarfare-npc");
    }

    private static Object getStructureMap(World world) {
        Object cached = STRUCTURE_MAP_CACHE.get(world);
        if (cached != null) return cached; // includes NO_DATA sentinel

        MapStorage storage = world.getPerWorldStorage();
        if (storage == null) return null;

        net.shadowmage.ancientwarfare.structure.gamedata.StructureMap map = null;

        for (String name : STRUCTURE_MAP_NAMES) {
            map = (net.shadowmage.ancientwarfare.structure.gamedata.StructureMap)
                    storage.getOrLoadData(net.shadowmage.ancientwarfare.structure.gamedata.StructureMap.class, name);
            if (map != null) {
                STRUCTURE_MAP_CACHE.put(world, map);
                return map;
            }
        }

        // Cache "not found" to avoid hammering MapStorage
        STRUCTURE_MAP_CACHE.put(world, NO_DATA);

        if (MillMixModConfig.debugAWOverlapLogs && !loggedNoStructureMap) {
            loggedNoStructureMap = true;
            System.out.println("[MillMix] AWOverlap: StructureMap WorldSavedData not found. " +
                    "Your STRUCTURE_MAP_NAMES list probably doesn't contain the real name.");
        }

        return NO_DATA;
    }

    private static boolean intersectsStructureMap(net.shadowmage.ancientwarfare.structure.gamedata.StructureMap structureMap,
                                                  World world,
                                                  int minX, int minY, int minZ,
                                                  int maxX, int maxY, int maxZ) {

        int minChunkX = minX >> 4;
        int maxChunkX = maxX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkZ = maxZ >> 4;

        int centreWorldX = ((minChunkX + maxChunkX) >> 1) * 16 + 8;
        int centreWorldZ = ((minChunkZ + maxChunkZ) >> 1) * 16 + 8;

        int chunkRadius = Math.max(maxChunkX - minChunkX, maxChunkZ - minChunkZ) / 2 + 2;

        ArrayList<net.shadowmage.ancientwarfare.structure.gamedata.StructureEntry> tmp = TL_LIST.get();
        tmp.clear();

        structureMap.getEntriesNear(world, centreWorldX, centreWorldZ, chunkRadius, true, tmp);
        if (tmp.isEmpty()) return false;

        for (net.shadowmage.ancientwarfare.structure.gamedata.StructureEntry entry : tmp) {
            net.shadowmage.ancientwarfare.structure.template.build.StructureBB bb = entry.getBB();
            BlockPos a = bb.min;
            BlockPos b = bb.max;

            if (aabbIntersects(minX, minY, minZ, maxX, maxY, maxZ,
                    a.getX(), a.getY(), a.getZ(),
                    b.getX(), b.getY(), b.getZ())) {
                return true;
            }
        }
        return false;
    }

    private static Object getTownMap(World world) {
        Object cached = TOWN_MAP_CACHE.get(world);
        if (cached != null) return cached;

        MapStorage storage = world.getPerWorldStorage();
        if (storage == null) return null;

        net.shadowmage.ancientwarfare.structure.gamedata.TownMap map = null;

        for (String name : TOWN_MAP_NAMES) {
            map = (net.shadowmage.ancientwarfare.structure.gamedata.TownMap)
                    storage.getOrLoadData(net.shadowmage.ancientwarfare.structure.gamedata.TownMap.class, name);
            if (map != null) {
                TOWN_MAP_CACHE.put(world, map);
                return map;
            }
        }

        TOWN_MAP_CACHE.put(world, NO_DATA);

        if (MillMixModConfig.debugAWOverlapLogs && !loggedNoTownMap) {
            loggedNoTownMap = true;
            System.out.println("[MillMix] AWOverlap: TownMap WorldSavedData not found. " +
                    "Your TOWN_MAP_NAMES list probably doesn't contain the real name.");
        }

        return NO_DATA;
    }

    private static boolean intersectsTownMap(net.shadowmage.ancientwarfare.structure.gamedata.TownMap townMap,
                                             int minX, int minY, int minZ,
                                             int maxX, int maxY, int maxZ) {

        // TownMap uses StructureBB too (in util package in your snippets)
        net.shadowmage.ancientwarfare.structure.template.build.StructureBB bb =
                new net.shadowmage.ancientwarfare.structure.template.build.StructureBB(
                        new BlockPos(minX, minY, minZ),
                        new BlockPos(maxX, maxY, maxZ)
                );

        return townMap.intersectsWithTown(bb);
    }

    private static boolean aabbIntersects(
            int aminX, int aminY, int aminZ, int amaxX, int amaxY, int amaxZ,
            int bminX, int bminY, int bminZ, int bmaxX, int bmaxY, int bmaxZ
    ) {
        return amaxX >= bminX && aminX <= bmaxX
                && amaxY >= bminY && aminY <= bmaxY
                && amaxZ >= bminZ && aminZ <= bmaxZ;
    }
}
