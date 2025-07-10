package com.jubitus.millmix;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

public class MillMixModConfig {

    private static Configuration config;
    private static final String CONFIG_VERSION_KEY = "configVersion";
    private static final String CURRENT_VERSION = "1.3"; // Increment when config format changes
    public static int maxWallStepHeight;
    public static int maxTerrainHeightDiff;
    public static int maxWallTerrainHeightDiff;
    public static int maxWallCheckRadius;
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
                    7,
                    0,
                    256,
                    "Maximum terrain height difference allowed under a building when generating.\n" +
                            "If the difference between the highest and lowest ground block under the building area is greater than this value, " +
                            "the location will be rejected.\nThis prevents buildings from generating on steep or uneven terrain.\n" +
                            "Needs restart to take effect."
            );
            // Read updated config values
            maxWallTerrainHeightDiff = config.getInt(
                    "maxWallTerrainHeightDiff",
                    Configuration.CATEGORY_GENERAL,
                    10,
                    0,
                    256,
                    "Maximum terrain height difference allowed under a building when generating.\n" +
                            "Same as above but for walls."
            );
            maxWallCheckRadius = config.getInt(
                    "maxWallCheckRadius",
                    Configuration.CATEGORY_GENERAL,
                    5,
                    0,
                    256,
                    "Radius around wall location to check terrain flatness."
            );
            maxWallStepHeight = config.getInt(
                    "maxWallStepHeight",
                    Configuration.CATEGORY_GENERAL,
                    2,
                    -32,
                    32,
                    "test test test test."
            );
            // Set config version if it's outdated or missing
            config.get(Configuration.CATEGORY_GENERAL, CONFIG_VERSION_KEY, CURRENT_VERSION).set(CURRENT_VERSION);

        } catch (Exception e) {
            System.err.println("Error loading config: " + e.getMessage());
        } finally {
            if (config.hasChanged()) {
                config.save();
                System.out.println("[ModConfig] Config saved.");
            }

            if (needsRewrite) {
                System.out.println("[ModConfig] Config updated due to old keys or version mismatch.");
            }
        }
    }
}