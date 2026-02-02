package com.jubitus.millmix.utility;


import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ancient Warfare (StructureMap / TownMap) overlap prevention.
 */
public final class ExternalStructureCompat {

    // MapStorage.loadedDataMap SRG name in 1.12.2
    // (Used only to enumerate already-loaded WorldSavedData instances)
    private static final String MAPSTORAGE_LOADED_DATA = "field_75749_b";
    // How often (in ticks) we rescan MapStorage to rediscover maps (in case they load later)
    private static final long RESCAN_INTERVAL_TICKS = 200L; // ~10 seconds
    // Sampling step inside building footprint; smaller = safer but more checks
    private static final int SAMPLE_STEP_BLOCKS = 8;
    private static final ConcurrentHashMap<Integer, Cache> CACHE_BY_DIM = new ConcurrentHashMap<>();

    private ExternalStructureCompat() {
    }

    /**
     * Return true if the rectangle [minX..maxX, minZ..maxZ] overlaps an AW structure/town.
     * This is done by sampling points inside the rectangle and asking AW if a structure exists there.
     */
    public static boolean isAreaBlocked(World world, int minX, int minZ, int maxX, int maxZ) {
        if (world == null || world.isRemote) return false;

        final int dim = world.provider.getDimension();
        final Cache cache = CACHE_BY_DIM.computeIfAbsent(dim, d -> new Cache());

        ensureScanned(world, cache);

        final Object structureMap = cache.structureMap;
        final Object townMap = cache.townMap;

        if (structureMap == null && townMap == null) {
            return false; // Ancient Warfare not present / not initialized / no data loaded
        }

        // Keep sampling cost bounded for large plans
        int step = SAMPLE_STEP_BLOCKS;
        int spanX = Math.max(1, maxX - minX);
        int spanZ = Math.max(1, maxZ - minZ);
        if (spanX > 96 || spanZ > 96) step = 12;
        if (spanX > 160 || spanZ > 160) step = 16;

        // Sample grid + corners to reduce "edge miss" risk
        for (int x = minX; x <= maxX; x += step) {
            for (int z = minZ; z <= maxZ; z += step) {
                if (isBlockedAt(world, structureMap, townMap, x, z)) return true;
            }
        }

        // Always check the corners and center-ish positions too
        if (isBlockedAt(world, structureMap, townMap, minX, minZ)) return true;
        if (isBlockedAt(world, structureMap, townMap, minX, maxZ)) return true;
        if (isBlockedAt(world, structureMap, townMap, maxX, minZ)) return true;
        if (isBlockedAt(world, structureMap, townMap, maxX, maxZ)) return true;
        if (isBlockedAt(world, structureMap, townMap, (minX + maxX) >> 1, (minZ + maxZ) >> 1)) return true;

        return false;
    }

    private static void ensureScanned(World world, Cache cache) {
        long now = world.getTotalWorldTime();
        if (cache.lastRescanTick != 0 && (now - cache.lastRescanTick) < RESCAN_INTERVAL_TICKS) {
            return;
        }
        cache.lastRescanTick = now;

        Object sm = findSavedDataBySimpleName(world, "StructureMap");
        Object tm = findSavedDataBySimpleName(world, "TownMap");

        if (sm != null) cache.structureMap = sm;
        if (tm != null) cache.townMap = tm;
    }

    /**
     * Tries a few Y levels at (x,z) so we don't miss tall/underground BBs.
     */
    private static boolean isBlockedAt(World world, Object structureMap, Object townMap, int x, int z) {
        // getHeight gives us a good surface-ish Y
        int ySurface = world.getHeight(new BlockPos(x, 0, z)).getY();

        int[] ys = new int[]{
                1,
                clampY(ySurface),
                clampY(ySurface + 32)
        };

        for (int y : ys) {
            BlockPos pos = new BlockPos(x, y, z);

            if (structureMap != null && structureContains(structureMap, world, pos)) return true;
            if (townMap != null && townBlocksHostiles(townMap, world, pos)) return true;
        }

        return false;
    }

    private static Object findSavedDataBySimpleName(World world, String simpleName) {
        // Mods may attach WorldSavedData to either storage in 1.12
        Object found = findInStorage(safeGetMapStorage(world), simpleName);
        if (found != null) return found;

        MapStorage pws = safeGetPerWorldStorage(world);
        if (pws != null) {
            found = findInStorage(pws, simpleName);
            if (found != null) return found;
        }
        return null;
    }

    private static int clampY(int y) {
        if (y < 1) return 1;
        if (y > 255) return 255;
        return y;
    }

    private static boolean structureContains(Object structureMap, World world, BlockPos pos) {
        try {
            Method m = structureMap.getClass().getMethod("getStructureAt", World.class, BlockPos.class);
            Object result = m.invoke(structureMap, world, pos);

            if (result instanceof Optional) {
                return ((Optional<?>) result).isPresent();
            }
        } catch (Throwable ignored) {
        }

        return false;
    }

    private static boolean townBlocksHostiles(Object townMap, World world, BlockPos pos) {
        try {
            Method m = townMap.getClass().getMethod("shouldPreventSpawnAtPos", World.class, BlockPos.class);
            Object result = m.invoke(townMap, world, pos);

            return (result instanceof Boolean) && (Boolean) result;
        } catch (Throwable ignored) {
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private static Object findInStorage(MapStorage storage, String simpleName) {
        if (storage == null) return null;

        try {
            Field f = MapStorage.class.getDeclaredField(MAPSTORAGE_LOADED_DATA);
            f.setAccessible(true);
            Object mapObj = f.get(storage);

            if (!(mapObj instanceof Map)) return null;

            Map<String, WorldSavedData> loaded = (Map<String, WorldSavedData>) mapObj;
            for (WorldSavedData data : loaded.values()) {
                if (data != null && data.getClass().getSimpleName().equals(simpleName)) {
                    return data;
                }
            }
        } catch (Throwable ignored) {
            // If this fails, we just behave like "compat not available"
        }

        return null;
    }

    private static MapStorage safeGetMapStorage(World world) {
        try {
            return world.getMapStorage();
        } catch (Throwable t) {
            return null;
        }
    }

    private static MapStorage safeGetPerWorldStorage(World world) {
        try {
            // WorldServer has this; World in client often doesn't
            return world.getPerWorldStorage();
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Utility: extract a World reference from an unknown object by scanning fields.
     * This lets you avoid hard-depending on VillageMapInfo having a specific field name.
     */
    public static World extractWorld(Object obj) {
        if (obj == null) return null;
        try {
            for (Field f : obj.getClass().getDeclaredFields()) {
                if (World.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    return (World) f.get(obj);
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /**
     * Utility: read an int field by name (returns fallback if absent).
     * Useful for winfo.mapStartX / winfo.mapStartZ without hard dependency.
     */
    public static int extractInt(Object obj, String fieldName, int fallback) {
        if (obj == null) return fallback;
        try {
            Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            Object v = f.get(obj);
            if (v instanceof Integer) return (Integer) v;
        } catch (Throwable ignored) {
        }
        return fallback;
    }

    private static final class Cache {
        volatile Object structureMap; // instance of StructureMap (WorldSavedData) if found
        volatile Object townMap;      // instance of TownMap (WorldSavedData) if found
        volatile long lastRescanTick;
    }
}
