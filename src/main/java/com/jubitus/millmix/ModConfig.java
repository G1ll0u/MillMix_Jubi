package com.jubitus.millmix;

import net.minecraftforge.common.config.Configuration;
import java.io.File;

public class ModConfig {

    public static Configuration config;

    public static int terrainMargin = 8;
    public static int maxTerrainHeightDiff = 5;

    public static void init(File configFile) {
        config = new Configuration(configFile);

        try {
            config.load();

            terrainMargin = config.getInt(
                    "terrainMargin",
                    Configuration.CATEGORY_GENERAL,
                    8,
                    0,
                    256,
                    "Margin around buildings to check for terrain flatness (in blocks). Requires restart to take effect."
            );

            maxTerrainHeightDiff = config.getInt(
                    "maxTerrainHeightDiff",
                    Configuration.CATEGORY_GENERAL,
                    5,
                    0,
                    256,
                    "Maximum allowed height difference between building area and surrounding margin. Requires restart to take effect."
            );

        } catch (Exception e) {
            System.out.println("Error loading config: " + e.getMessage());
        } finally {
            if (config.hasChanged()) {
                config.save();
            }
        }
    }
}