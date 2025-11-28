package com.jubitus.millmix;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Loader;

import java.io.File;
import java.io.IOException;

public class MillMixModConfig {

    // existing fields...
    public static boolean disableAllWalls = false;
    public static int maxWallTerrainHeightDiff;
    public static boolean enableDangerousCustomVillageLogic = false;
    public static int chopRadius;
    public static int maxTerrainHeightDiff;
    public static boolean disableAllMillenaireExtendedDeployment = false;

    public static volatile boolean configLoaded = false;

    private static Configuration config;
    private static final String CONFIG_VERSION_KEY = "configVersion";
    private static final String CURRENT_VERSION = "3.2";
    private static final String CONFIG_FILE_NAME = "millmix.cfg";

    /**
     * Called from your @Mod.preInit (optional now, but still fine).
     */
    public static synchronized void init(File configFile) {
        if (configLoaded) {
            return; // already done
        }
        loadInternal(configFile);
    }

    /**
     * Called from the mixin if configLoaded is false.
     * It uses Forge's config dir directly and does NOT depend on FML events.
     */
    public static synchronized void ensureLoaded() {
        if (configLoaded) {
            return;
        }
        File configDir  = Loader.instance().getConfigDir();
        File configFile = new File(configDir, CONFIG_FILE_NAME);
        loadInternal(configFile);
    }

    private static void loadInternal(File configFile) {
        // Ensure parent folder exists
        File parent = configFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        config = new Configuration(configFile);
        boolean needsRewrite = false;

        try {
            final boolean exists  = configFile.exists();
            final boolean isEmpty = exists && configFile.length() == 0L;

            if (!exists || isEmpty) {
                config.load();
                syncConfig(config); // creates entries with defaults
                System.out.println("[ModConfig] Config " +
                        (isEmpty ? "was empty; " : "") +
                        "created with version " + CURRENT_VERSION + ".");
            } else {
                config.load();

                if (config.getCategory(Configuration.CATEGORY_GENERAL).containsKey("terrainMargin")) {
                    config.getCategory(Configuration.CATEGORY_GENERAL).remove("terrainMargin");
                    System.out.println("[ModConfig] Removed legacy key 'terrainMargin'.");
                    needsRewrite = true;
                }

                String version = config.get(
                        Configuration.CATEGORY_GENERAL,
                        CONFIG_VERSION_KEY,
                        ""
                ).getString();

                if (!CURRENT_VERSION.equals(version)) {
                    System.out.println("[ModConfig] Config version mismatch (found '" + version + "'). " +
                            "Recreating config as " + CURRENT_VERSION + "...");
                    backupAndDelete(configFile, version);
                    config = new Configuration(configFile);
                    config.load();
                    syncConfig(config);
                    System.out.println("[ModConfig] Config recreated with version " + CURRENT_VERSION + ".");
                } else {
                    syncConfig(config);
                }
            }
        } catch (Exception e) {
            System.err.println("[ModConfig] Error loading config: " + e.getMessage());
        } finally {
            try {
                if (config.hasChanged() || needsRewrite) {
                    config.save();
                    System.out.println("[ModConfig] Config saved.");
                }
                if (needsRewrite) {
                    System.out.println("[ModConfig] Config updated due to legacy key removal.");
                }
            } finally {
                // IMPORTANT: mark loaded even if partly failed,
                // so we don't spam reload attempts every deployContent call
                configLoaded = true;
            }
        }
    }

    private static void syncConfig(Configuration cfg) {
        cfg.get(Configuration.CATEGORY_GENERAL, CONFIG_VERSION_KEY, CURRENT_VERSION)
                .set(CURRENT_VERSION);

        // --- your existing options, abbreviated here ---
        maxTerrainHeightDiff = cfg.getInt(
                "maxTerrainHeightDiff",
                "Village Generation",
                8, 0, 256,
                "Maximum terrain height difference allowed under a building when generating."
        );

        maxWallTerrainHeightDiff = cfg.getInt(
                "maxWallTerrainHeightDiff",
                "Village Generation",
                8, 0, 256,
                "Maximum terrain height difference allowed under a wall when generated."
        );

        disableAllWalls = cfg.getBoolean(
                "disableAllWalls",
                "Village Generation",
                false,
                "If true, prevents all village walls from generating. Needs restart to take effect."
        );

        disableAllMillenaireExtendedDeployment = config.getBoolean(
                "disableAllMillenaireExtendedDeployment",
                Configuration.CATEGORY_GENERAL,
                false,
                "If true, Millénaire Extended and ALL its addons will NOT deploy/overwrite content at startup.\n" +
                        "Set to false to restore the original deployment behaviour.\n" +
                        "Requires restart."
        );

        enableDangerousCustomVillageLogic = cfg.getBoolean(
                "DangerousCustomVillageLogicForDebuggingLoneBuildings",
                "debug",
                false,
                "DEBUG ONLY..."
        );

        chopRadius = cfg.getInt(
                "chopWoodRadius",
                "Lumberman",
                10, 0, 256,
                "chop wood radius for lumberman."
        );
    }


    /**
     * Backup existing non-empty file and delete it so a clean one can be written.
     */
    private static void backupAndDelete(File configFile, String oldVersion) {
        try {
            if (!configFile.exists()) {
                return; // nothing to back up
            }

            if (configFile.length() == 0L) {
                if (!configFile.delete()) {
                    System.err.println("[ModConfig] Failed to delete empty old config file.");
                }
                return;
            }

            String ts = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss")
                    .format(new java.util.Date());
            String name = configFile.getName();
            int dot = name.lastIndexOf('.');
            String base = (dot > 0 ? name.substring(0, dot) : name);
            File backup = new File(
                    configFile.getParentFile(),
                    base + "_" + ts + ".cfg.bak"
            );

            java.nio.file.Path src = configFile.toPath();
            java.nio.file.Path dst = backup.toPath();

            try {
                java.nio.file.Files.move(
                        src,
                        dst,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE
                );
            } catch (Exception moveEx) {
                java.nio.file.Files.copy(
                        src,
                        dst,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                );
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

