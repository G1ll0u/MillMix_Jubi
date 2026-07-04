package vict.millmix;

import net.minecraftforge.common.config.Configuration;

import java.io.File;
import java.io.IOException;

public class MillMixModConfig {
    private static final String CONFIG_VERSION_KEY = "configVersion";
    private static final String CURRENT_VERSION = "3.2";

    public static boolean disableAllWalls = false;
    public static int maxWallTerrainHeightDiff;
    public static int chopRadius;
    public static boolean debugAWOverlapLogs = true;
    public static int maxTerrainHeightDiff;
    public static boolean enableVillagerSwimming = false;
    public static boolean ghostMovementEnabled = true;
    public static int ghostViewDistance = 160;
    public static double ghostWalkSpeedBPS = 3.0;
    public static boolean ghostMovementDebug = false;
    public static boolean avoidAW2Structures = true;
    public static boolean parallelPlanLoading = true;
    public static int parallelPlanLoadingThreads = 0;

    private static Configuration config;

    public static void reload() {
        if (config == null) return;
        config.load();
        sync();
        if (config.hasChanged()) config.save();
    }

    public static void init(File configFile) {
        File parent = configFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        config = new Configuration(configFile);

        try {
            final boolean exists = configFile.exists();
            final boolean isEmpty = exists && configFile.length() == 0L;

            if (!exists || isEmpty) {
                sync();
                config.save();
                System.out.println("[ModConfig] Config " + (isEmpty ? "was empty; " : "") +
                        "created with version " + CURRENT_VERSION + ".");
                return;
            }

            config.load();

            boolean dirty = removeLegacyKeys();

            String version = config.get(Configuration.CATEGORY_GENERAL, CONFIG_VERSION_KEY, "").getString();
            if (!CURRENT_VERSION.equals(version)) {
                System.out.println("[ModConfig] Config version mismatch (found '" + version + "'). Recreating config as " + CURRENT_VERSION + "...");
                backupAndDelete(configFile);
                config = new Configuration(configFile);
                sync();
                config.save();
                System.out.println("[ModConfig] Config recreated with version " + CURRENT_VERSION + ".");
                return;
            }

            sync();
            if (config.hasChanged() || dirty) {
                config.save();
            }

        } catch (Exception e) {
            System.err.println("[ModConfig] Error loading config: " + e.getMessage());
        }
    }

    /**
     * Single source of truth for all config entries.
     * Reads from file if present, writes defaults otherwise.
     * Called for both fresh-file creation and normal load.
     */
    private static void sync() {
        config.get(Configuration.CATEGORY_GENERAL, CONFIG_VERSION_KEY, CURRENT_VERSION).set(CURRENT_VERSION);

        maxTerrainHeightDiff = config.getInt(
                "maxTerrainHeightDiff",
                "Village Generation",
                8, 0, 256,
                "Maximum terrain height difference allowed under a building when generating.\n" +
                        "If the difference between the highest and lowest ground block under the building area is greater than this value, " +
                        "the location will be rejected.\nThis prevents buildings from generating on steep or uneven terrain.\n" +
                        "Needs restart to take effect."
        );

        maxWallTerrainHeightDiff = config.getInt(
                "maxWallTerrainHeightDiff",
                "Village Generation",
                8, 0, 256,
                "Maximum terrain height difference allowed under a wall when generated.\n" +
                        "If the difference between the highest and lowest ground block under the wall area is greater than this value, " +
                        "the location will be rejected.\nThis prevents walls from generating on steep or uneven terrain.\n" +
                        "Needs restart to take effect."
        );

        disableAllWalls = config.getBoolean(
                "disableAllWalls",
                "Village Generation",
                false,
                "If true, prevents all village walls from generating. Needs restart to take effect."
        );

        chopRadius = config.getInt(
                "chopWoodRadius",
                "Lumberman",
                10, 0, 256,
                "chop wood radius for lumberman, WILL BREAK LOG BLOCKS OUTSIDE BUILDING FOOTPRINT IF SET TOO BIG"
        );

        enableVillagerSwimming = config.getBoolean(
                "enableVillagerSwimming",
                "Pathing",
                false,
                "If true, villagers may path through water (swimming) and get a small buoyancy assist.\n" +
                        "If false, all water passable/viable logic and the swim fallback are disabled.\n" +
                        "Needs restart to take effect."
        );

        ghostMovementEnabled = config.getBoolean(
                "ghostMovementEnabled",
                "Pathing",
                true,
                "If true, villagers outside the ghost view distance skip A* pathfinding and walk\n" +
                        "linearly toward their goal at walking speed. Goal completion still works because\n" +
                        "Millenaire detects arrival by horizontal distance, not by path state.\n" +
                        "Greatly reduces server load when many villagers are active but unobserved."
        );

        ghostViewDistance = config.getInt(
                "ghostMovementViewDistance",
                "Pathing",
                160, 16, 1024,
                "Radius in blocks around each player within which villagers use real A* pathfinding.\n" +
                        "Outside this radius they use ghost movement. Default 160 = 10 chunks."
        );

        ghostWalkSpeedBPS = config.getFloat(
                "ghostMovementWalkSpeed",
                "Pathing",
                3.0f, 0.1f, 50.0f,
                "Villager walk speed in blocks/second used for ghost movement.\n" +
                        "Default 3.0 is a reasonable approximation for a standard Millenaire villager.\n" +
                        "Can be set higher than real walking speed to help offscreen villagers keep up with their goals."
        );

        ghostMovementDebug = config.getBoolean(
                "ghostMovementDebug",
                "Pathing",
                false,
                "If true, logs ghost movement events (position, destination, distance) to stdout.\n" +
                        "Rate-limited per villager to avoid spam. For debugging only."
        );

        avoidAW2Structures = config.getBoolean(
                "avoidAW2Structures",
                "Compat",
                true,
                "If true, Millénaire buildings/walls will not generate inside/overlapping the AW2's structure/town bounding boxes."
        );

        parallelPlanLoading = config.getBoolean(
                "parallelPlanLoading",
                "Performance",
                true,
                "If true, building plans (PNG files) within each culture are loaded in parallel.\n" +
                        "Significantly reduces startup time with many culture addons installed.\n" +
                        "Disable if you experience crashes or incorrect building layouts on startup."
        );

        parallelPlanLoadingThreads = config.getInt(
                "parallelPlanLoadingThreads",
                "Performance",
                0, 0, 64,
                "Number of threads used for parallel plan loading. 0 = auto (availableProcessors - 1).\n" +
                        "Only used when parallelPlanLoading is true."
        );
    }

    private static boolean removeLegacyKeys() {
        boolean dirty = false;
        if (config.getCategory(Configuration.CATEGORY_GENERAL).containsKey("terrainMargin")) {
            config.getCategory(Configuration.CATEGORY_GENERAL).remove("terrainMargin");
            System.out.println("[ModConfig] Removed legacy key 'terrainMargin'.");
            dirty = true;
        }
        return dirty;
    }

    private static void backupAndDelete(File configFile) {
        try {
            if (!configFile.exists()) return;

            if (configFile.length() == 0L) {
                if (!configFile.delete()) {
                    System.err.println("[ModConfig] Failed to delete empty old config file.");
                }
                return;
            }

            String ts = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss").format(new java.util.Date());
            String name = configFile.getName();
            int dot = name.lastIndexOf('.');
            String base = (dot > 0 ? name.substring(0, dot) : name);
            File backup = new File(configFile.getParentFile(), base + "_" + ts + ".cfg.bak");

            java.nio.file.Path src = configFile.toPath();
            java.nio.file.Path dst = backup.toPath();
            try {
                java.nio.file.Files.move(src, dst, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception moveEx) {
                java.nio.file.Files.copy(src, dst, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                if (!configFile.delete()) {
                    throw new IOException("Could not delete original config after backup");
                }
            }

            System.out.println("[ModConfig] Backed up old config to: " + backup.getName());
        } catch (Throwable t) {
            System.err.println("[ModConfig] Failed to backup old config: " + t.getMessage());
            if (configFile.exists() && !configFile.delete()) {
                System.err.println("[ModConfig] Also failed to delete old config file.");
            }
        }
    }
}
