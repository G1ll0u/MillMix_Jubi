package com.jubitus.millmix.mixin;


import org.millenaire.common.config.MillConfigParameter;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.utilities.MillLog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

import static org.millenaire.common.config.MillConfigValues.*;

@Mixin(MillConfigValues.class)

public class MixinMillConfigValues {
    /**
     * idea: TODO
     *
     * @author Jubitus
     * @reason add config values
     */
    @Overwrite(remap = false)
    private static void initConfigItems() {
        try {
            ArrayList<MillConfigParameter> configSection = new ArrayList<>();
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("fallback_language"), "fallback_language", "en", "fr"));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("languageLearning"), "language_learning", new Object[0]));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("TRAVEL_BOOK_LEARNING"), "travel_book_learning", new Object[0]));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("loadAllLanguages"), "load_all_languages", new Object[0]));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("displayStart"), "display_start", new Object[0]));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("displayNames"), "display_names", new Object[0]));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("VillagersNamesDistance"), "villagers_names_distance", 0, 5, 10, 20, 30, 50));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("VillagersSentenceInChatDistanceSP"), "villagers_sentence_in_chat_distance_sp", 0, 1, 2, 3, 4, 6, 10));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("VillagersSentenceInChatDistanceClient"), "villagers_sentence_in_chat_distance_client", 0, 1, 2, 3, 4, 6, 10));
            configPages.add(configSection);
            configPageTitles.add("config.page.uisettings");
            configPageDesc.add(null);
            configSection = new ArrayList();
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("generateVillages"), "generate_villages", new Object[0]));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("generateLoneBuildings"), "generate_lone_buildings", new Object[0]));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("generateHamlets"), "generate_hamlets", new Object[0]));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("minDistanceBetweenVillages"), "min_village_distance", 300, 450, 600, 800, 1000));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("minDistanceBetweenVillagesAndLoneBuildings"), "min_village_lonebuilding_distance", 100, 200, 300, 500, 800));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("minDistanceBetweenLoneBuildings"), "min_lonebuilding_distance", 300, 450, 600, 800, 1000));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("spawnProtectionRadius"), "spawn_protection_radius", 0, 50, 100, 150, 250, 500));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("villageSpawnCompletionMaxPercentage"), "village_spawn_completion_max_percentage", 0, 10, 25, 50, 75, 100));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("villageSpawnCompletionMinDistance"), "village_spawn_completion_min_distance", 0, 1000, 2000, 5000, 10000, 25000));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("villageSpawnCompletionMaxDistance"), "village_spawn_completion_max_distance", 0, 5000, 10000, 25000, 100000));
            configPages.add(configSection);
            configPageTitles.add("config.page.worldgeneration");
            configPageDesc.add("config.page.worldgeneration.desc");
            configSection = new ArrayList();
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("KeepActiveRadius"), "keep_active_radius", 0, 100, 150, 200, 250, 300, 400, 450, 500, 550, 600, 800, 900, 1000, 2000, 2500, 3000));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("VillageRadius"), "village_radius", 50, 60, 70, 80, 90, 100, 120, 140, 160, 180, 200, 250, 300, 400));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("minDistanceBetweenBuildings"), "min_distance_between_buildings", 0, 1, 2, 3, 4));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("BuildVillagePaths"), "village_paths", new Object[0]));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("maxChildrenNumber"), "max_children_number", 2, 5, 10, 15, 20));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("BackgroundRadius"), "background_radius", 0, 200, 500, 1000, 1500, 2000, 2500, 3000));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("BanditRaidRadius"), "bandit_raid_radius", 0, 200, 500, 1000, 1500, 2000));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("RaidingRate"), "raiding_rate", 0, 10, 20, 50, 100));

            configPages.add(configSection);
            configPageTitles.add("config.page.villagebehaviour");
            configPageDesc.add("config.page.villagebehaviour.desc");
            configSection = new ArrayList();
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("sendStatistics"), "send_statistics", new Object[0]));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("sendAdvancementLogin"), "send_advancement_login", new Object[0]));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("autoConvertProfiles"), "auto_convert_profiles", new Object[0]));
            configPages.add(configSection);
            configPageTitles.add("config.page.system");
            configPageDesc.add("config.page.system.desc");
            configSection = new ArrayList();
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("generateTranslationGap"), "generate_translation_gap", new Object[0]));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("generateTravelBookExport"), "generate_travel_book_export", new Object[0]));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("generateHelpData"), "generate_help_data", new Object[0]));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("generateBuildingRes"), "generate_building_res", new Object[0]));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("ignoreResourceCost"), "ignore_resource_cost", new Object[0]));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("LogTileEntityBuilding"), "LogTileEntityBuilding", 5).setDisplayDev(true));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("LogWorldGeneration"), "LogWorldGeneration", 5).setDisplayDev(true));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("LogDiplomacy"), "LogDiplomacy", 5).setDisplayDev(true));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("LogWifeAI"), "LogWifeAI", 5).setDisplayDev(true));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("LogVillager"), "LogVillager", 5).setDisplayDev(true));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("LogQuest"), "LogQuest", 5).setDisplayDev(true));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("LogPathing"), "LogPathing", 5).setDisplayDev(true));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("LogConnections"), "LogConnections", 5).setDisplayDev(true));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("LogGetPath"), "LogGetPath", 5).setDisplayDev(true));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("LogLumberman"), "LogLumberman", 5).setDisplayDev(true));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("LogBuildingPlan"), "LogBuildingPlan", 5).setDisplayDev(true));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("LogGeneralAI"), "LogGeneralAI", 5).setDisplayDev(true));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("LogSelling"), "LogSelling", 5).setDisplayDev(true));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("LogHybernation"), "LogHybernation", 5).setDisplayDev(true));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("LogOther"), "LogOther", 5).setDisplayDev(true));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("LogChildren"), "LogChildren", 5).setDisplayDev(true));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("LogCattleFarmer"), "LogCattleFarmer", 5).setDisplayDev(true));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("LogMiner"), "LogMiner", 5).setDisplayDev(true));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("LogVillage"), "LogVillage", 5).setDisplayDev(true));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("LogWorldInfo"), "LogWorldInfo", 5).setDisplayDev(true));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("LogPujas"), "LogPujas", 5).setDisplayDev(true));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("LogVillagerSpawn"), "LogVillagerSpawn", 5).setDisplayDev(true));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("LogVillagePaths"), "LogVillagePaths", 5).setDisplayDev(true));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("LogNetwork"), "LogNetwork", 5).setDisplayDev(true));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("LogMerchant"), "LogMerchant", 5).setDisplayDev(true));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("LogCulture"), "LogCulture", 5).setDisplayDev(true));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("LogTranslation"), "LogTranslation", 5).setDisplayDev(true));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("LogChunkLoader"), "LogChunkLoader", 5).setDisplayDev(true));
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("LogTags"), "LogTags", 5).setDisplayDev(true));
            configPages.add(configSection);
            configPageTitles.add("config.page.devtools");
            configPageDesc.add(null);
            configSection = new ArrayList();
            configSection.add(new MillConfigParameter(MillConfigValues.class.getField("bonusCode"), "bonus_code", 6).setMaxStringLength(4));
            configPages.add(configSection);

            configPageTitles.add("config.page.bonus");
            configPageDesc.add("config.page.bonus.desc");
            for (List<MillConfigParameter> aConfigPage : configPages) {
                for (MillConfigParameter config : aConfigPage) {
                    configParameters.put(config.key, config);
                }
            }

        } catch (Exception e) {
            MillLog.error(null, "Exception when initialising config items: " + e);
        }

    }
}