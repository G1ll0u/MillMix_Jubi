package com.jubitus.millmix;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

public class MillMixModConfig {
    public static boolean disableAllWalls = false;
    public static int maxWallTerrainHeightDiff;
    private static Configuration config;
    private static final String CONFIG_VERSION_KEY = "configVersion";
    private static final String CURRENT_VERSION = "2.1";
    public static int maxTerrainHeightDiff;
    public static int logsToKeep;

    public static void init(File configFile) {
        config = new Configuration(configFile);
        boolean needsRewrite = false;
        try {
            config.load();

            // Check and handle outdated config


            // Check for old key "I:terrainMargin"
            if (config.getCategory(Configuration.CATEGORY_GENERAL).containsKey("terrainMargin")) {
                config.getCategory(Configuration.CATEGORY_GENERAL).remove("terrainMargin");
                System.out.println("[ModConfig] Removed legacy key 'terrainMargin'.");
                needsRewrite = true;
            }


            // Check for version tag
            String version = config.get(Configuration.CATEGORY_GENERAL, CONFIG_VERSION_KEY, "").getString();
            if (!CURRENT_VERSION.equals(version)) {
                System.out.println("[ModConfig] Config version mismatch or missing. Updating config version to " + CURRENT_VERSION);
                needsRewrite = true;
            }

            // Read updated config values
            maxTerrainHeightDiff = config.getInt(
                    "maxTerrainHeightDiff",
                    Configuration.CATEGORY_GENERAL,
                    8,
                    0,
                    256,
                    "Maximum terrain height difference allowed under a building when generating.\n" +
                            "If the difference between the highest and lowest ground block under the building area is greater than this value, " +
                            "the location will be rejected.\nThis prevents buildings from generating on steep or uneven terrain.\n" +
                            "Needs restart to take effect."
            );
            maxWallTerrainHeightDiff = config.getInt(
                    "maxWallTerrainHeightDiff",
                    Configuration.CATEGORY_GENERAL,
                    8,
                    0,
                    256,
                    "Maximum terrain height difference allowed under a wall when generated.\n" +
                            "If the difference between the highest and lowest ground block under the building area is greater than this value, " +
                            "the location will be rejected.\nThis prevents buildings from generating on steep or uneven terrain.\n" +
                            "Needs restart to take effect."
            );
            disableAllWalls = config.getBoolean(
                    "disableAllWalls",
                    Configuration.CATEGORY_GENERAL,
                    false,
                    "Maximum terrain height difference allowed under a wall when generated.\n" +
                            "If the difference between the highest and lowest ground block under the building area is greater than this value, " +
                            "the location will be rejected.\nThis prevents buildings from generating on steep or uneven terrain.\n" +
                            "Needs restart to take effect."
            );
            logsToKeep = config.getInt(
                    "logsToKeep",
                    Configuration.CATEGORY_GENERAL,
                    5,
                    1,
                    30,
                    "Maximum logs to keep"
            );

            // Set config version if it's outdated or missing
            config.get(Configuration.CATEGORY_GENERAL, CONFIG_VERSION_KEY, CURRENT_VERSION).set(CURRENT_VERSION);
            needsRewrite = true; // Make sure config.save() is triggered

        } catch (Exception e) {
            System.err.println("Error loading config: " + e.getMessage());
        } finally {
            // Check for old key "don't touch that"
            if (config.hasChanged()) {
                if (config.hasCategory("don't touch that")) {
                    config.removeCategory(config.getCategory("don't touch that"));
                    System.out.println("[ModConfig] Removed legacy 'don't touch that' category.");
                    needsRewrite = true;
                }
                config.save();
                System.out.println("[ModConfig] Config saved.");
            }

            if (needsRewrite) {
                System.out.println("[ModConfig] Config updated due to old keys or version mismatch.");
            }
        }
    }
}