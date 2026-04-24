package vict.millmix;

import net.minecraftforge.common.config.Configuration;

import java.io.File;
import java.io.IOException;

public class MillMixModConfig {
    private static final String CONFIG_VERSION_KEY = "configVersion";
    private static final String CURRENT_VERSION = "3.1";
    public static boolean disableAllWalls = false;
    public static int maxWallTerrainHeightDiff;
    public static int chopRadius;
    public static boolean debugAWOverlapLogs = true;
    public static int maxTerrainHeightDiff;
    public static boolean enableVillagerSwimming = false;
    public static boolean avoidAW2Structures = true;
    private static Configuration config;


    public static void init(File configFile) {
        // Ensure parent folder exists
        File parent = configFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        config = new Configuration(configFile);
        boolean needsRewrite = false;

        try {
            final boolean exists = configFile.exists();
            final boolean isEmpty = exists && configFile.length() == 0L;

            // If there is no file or it's an empty stub, just write defaults and return.
            if (!exists || isEmpty) {
                writeDefaults(config);
                System.out.println("[ModConfig] Config " + (isEmpty ? "was empty; " : "") +
                        "created with version " + CURRENT_VERSION + ".");
                return;
            }

            // Normal load path
            config.load();

            // Remove legacy keys
            if (config.getCategory(Configuration.CATEGORY_GENERAL).containsKey("terrainMargin")) {
                config.getCategory(Configuration.CATEGORY_GENERAL).remove("terrainMargin");
                System.out.println("[ModConfig] Removed legacy key 'terrainMargin'.");
                needsRewrite = true;
            }

            // Version check
            String version = config.get(Configuration.CATEGORY_GENERAL, CONFIG_VERSION_KEY, "").getString();
            if (!CURRENT_VERSION.equals(version)) {
                System.out.println("[ModConfig] Config version mismatch (found '" + version + "'). Recreating config as " + CURRENT_VERSION + "...");
                backupAndDelete(configFile, version);
                config = new Configuration(configFile);
                writeDefaults(config);
                System.out.println("[ModConfig] Config recreated with version " + CURRENT_VERSION + ".");
                return;
            }

            // Version matches → read values
            readValues(config);

        } catch (Exception e) {
            System.err.println("[ModConfig] Error loading config: " + e.getMessage());
        } finally {
            if (config.hasChanged() || needsRewrite) {
                config.save();
                System.out.println("[ModConfig] Config saved.");
            }
            if (needsRewrite) {
                System.out.println("[ModConfig] Config updated due to legacy key removal.");
            }
        }
    }


    /**
     * Write a clean file with current defaults and CURRENT_VERSION set.
     */
    private static void writeDefaults(Configuration cfg) {
        // Always set the version first
        cfg.get(Configuration.CATEGORY_GENERAL, CONFIG_VERSION_KEY, CURRENT_VERSION).set(CURRENT_VERSION);

        // --- VILLAGE GENERATION (defaults) ---
        maxTerrainHeightDiff = cfg.getInt(
                "maxTerrainHeightDiff",
                "Village Generation",
                8, 0, 256,
                "Maximum terrain height difference allowed under a building when generating.\n" +
                        "If the difference between the highest and lowest ground block under the building area is greater than this value, " +
                        "the location will be rejected.\nThis prevents buildings from generating on steep or uneven terrain.\n" +
                        "Needs restart to take effect."
        );

        maxWallTerrainHeightDiff = cfg.getInt(
                "maxWallTerrainHeightDiff",
                "Village Generation",
                8, 0, 256,
                "Maximum terrain height difference allowed under a wall when generated.\n" +
                        "If the difference between the highest and lowest ground block under the wall area is greater than this value, " +
                        "the location will be rejected.\nThis prevents walls from generating on steep or uneven terrain.\n" +
                        "Needs restart to take effect."
        );

        disableAllWalls = cfg.getBoolean(
                "disableAllWalls",
                "Village Generation",
                false,
                "If true, prevents all village walls from generating. Needs restart to take effect."
        );

        chopRadius = cfg.getInt(
                "chopWoodRadius",
                "Lumberman",
                10, 0, 256,
                "chop wood radius for lumberman, WILL BREAK LOG BLOCKS OUTSIDE BUILDING FOOTPRINT IF SET TOO BIG"
        );
        enableVillagerSwimming = cfg.getBoolean(
                "enableVillagerSwimming",
                "Pathing",
                false,
                "If true, villagers may path through water (swimming) and get a small buoyancy assist.\n" +
                        "If false, all water passable/viable logic and the swim fallback are disabled.\n" +
                        "Needs restart to take effect."
        );
        avoidAW2Structures = cfg.getBoolean(
                "avoidAW2Structures",
                "Compat",
                true,
                "If true, Millénaire buildings/walls will not generate inside/overlapping the AW2's structure/town bounding boxes."
        );

        cfg.save();
    }

    /**
     * Backup existing non-empty file and delete it so a clean one can be written.
     */
    private static void backupAndDelete(File configFile, String oldVersion) {
        try {
            if (!configFile.exists()) {
                return; // nothing to back up
            }

            // Skip backing up zero-byte files (often stub/placeholder files)
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
            File backup = new File(
                    configFile.getParentFile(),
                    base + "_" + ts + ".cfg.bak" // add the missing dot before cfg.bak
            );

            java.nio.file.Path src = configFile.toPath();
            java.nio.file.Path dst = backup.toPath();
            // Try an atomic move; fall back to copy+delete if not supported
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

    /**
     * Read current values from an already-loaded, version-matching config.
     */
    private static void readValues(Configuration cfg) {
        // Ensure version key stays pinned
        cfg.get(Configuration.CATEGORY_GENERAL, CONFIG_VERSION_KEY, CURRENT_VERSION).set(CURRENT_VERSION);

        // --- VILLAGE GENERATION ---
        maxTerrainHeightDiff = cfg.getInt(
                "maxTerrainHeightDiff", "Village Generation",
                8, 0, 256,
                "Maximum terrain height difference allowed under a building when generating.\n" +
                        "If the difference between the highest and lowest ground block under the building area is greater than this value, " +
                        "the location will be rejected.\nThis prevents buildings from generating on steep or uneven terrain.\n" +
                        "Needs restart to take effect."
        );

        maxWallTerrainHeightDiff = cfg.getInt(
                "maxWallTerrainHeightDiff", "Village Generation",
                8, 0, 256,
                "Maximum terrain height difference allowed under a wall when generated.\n" +
                        "If the difference between the highest and lowest ground block under the wall area is greater than this value, " +
                        "the location will be rejected.\nThis prevents walls from generating on steep or uneven terrain.\n" +
                        "Needs restart to take effect."
        );

        disableAllWalls = cfg.getBoolean(
                "disableAllWalls", "Village Generation",
                false,
                "If true, prevents all village walls from generating. Needs restart to take effect."
        );

        chopRadius = cfg.getInt(
                "chopWoodRadius",
                "Lumberman",
                10, 0, 256,
                "chop wood radius for lumberman, WILL BREAK LOG BLOCKS OUTSIDE BUILDING FOOTPRINT IF SET TOO BIG"
        );
        enableVillagerSwimming = cfg.getBoolean(
                "enableVillagerSwimming",
                "Pathing",
                false,
                "If true, villagers may path through water (swimming) and get a small buoyancy assist.\n" +
                        "If false, all water passable/viable logic and the swim fallback are disabled.\n" +
                        "Needs restart to take effect."
        );
        avoidAW2Structures = cfg.getBoolean(
                "avoidAW2Structures",
                "Compat",
                true,
                "If true, Millénaire buildings/walls will not generate inside/overlapping the other mod's structure/town bounding boxes.\n" +
                        "Uses reflection; safe even if the other mod is not installed."
        );


    }
}