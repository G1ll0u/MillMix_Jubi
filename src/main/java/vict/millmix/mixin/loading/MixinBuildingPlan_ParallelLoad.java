package vict.millmix.mixin.loading;

import org.millenaire.common.buildingplan.BuildingFileFiler;
import org.millenaire.common.buildingplan.BuildingPlan;
import org.millenaire.common.buildingplan.BuildingPlanSet;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.culture.Culture;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.virtualdir.VirtualDir;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import vict.millmix.MillMixModConfig;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

@Mixin(BuildingPlan.class)
public class MixinBuildingPlan_ParallelLoad {

    @Shadow(remap = false)
    private static void validatePlans(HashMap<String, BuildingPlanSet> plans) {
        throw new AssertionError();
    }

    /**
     * @author Jubitus
     * @reason Parallelize building plan loading across buildings within a culture.
     *         Each BuildingPlanSet is independent (different key, different files).
     *         Thread-safety is ensured by:
     *           - ConcurrentHashMap for the result accumulator
     *           - InvItem.CACHE replaced with ConcurrentHashMap (MixinInvItem_ConcurrentCache)
     *           - MillLog.writeText synchronized on MillLog.class (MixinMillLog)
     */
    @Overwrite(remap = false)
    public static HashMap<String, BuildingPlanSet> loadPlans(VirtualDir cultureVirtualDir, Culture culture) {
        VirtualDir buildingsVirtualDir = cultureVirtualDir.getChildDirectory("buildings");
        List<File> files = buildingsVirtualDir.listFilesRecursive(new BuildingFileFiler("_A.txt"));

        if (!MillMixModConfig.parallelPlanLoading || files.size() < 2) {
            HashMap<String, BuildingPlanSet> plans = new HashMap<>();
            for (File file : files) {
                millmix$loadOnePlan(file, buildingsVirtualDir, culture, plans);
            }
            validatePlans(plans);
            return plans;
        }

        int threads = MillMixModConfig.parallelPlanLoadingThreads > 0
                ? MillMixModConfig.parallelPlanLoadingThreads
                : Math.max(1, Runtime.getRuntime().availableProcessors() - 1);

        ConcurrentHashMap<String, BuildingPlanSet> concurrentPlans = new ConcurrentHashMap<>();
        // Propagate the FML classloader to worker threads so they can access
        // Forge/Millénaire classes; ForkJoinPool's default factory uses the
        // system classloader which would cause ClassNotFoundException on workers.
        ClassLoader fmlClassLoader = Thread.currentThread().getContextClassLoader();
        ForkJoinPool pool = new ForkJoinPool(threads,
                p -> {
                    ForkJoinWorkerThread t = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(p);
                    t.setContextClassLoader(fmlClassLoader);
                    return t;
                },
                null, false);
        try {
            pool.submit(() ->
                files.parallelStream().forEach(file ->
                    millmix$loadOnePlan(file, buildingsVirtualDir, culture, concurrentPlans)
                )
            ).get();
        } catch (Exception e) {
            MillLog.printException("Exception in parallel plan loading for culture " + culture.key + ":", e);
        } finally {
            pool.shutdown();
        }

        HashMap<String, BuildingPlanSet> plans = new HashMap<>(concurrentPlans);
        validatePlans(plans);
        return plans;
    }

    @Unique
    private static void millmix$loadOnePlan(File file, VirtualDir buildingsVirtualDir, Culture culture,
                                             Map<String, BuildingPlanSet> plans) {
        try {
            if (MillConfigValues.LogBuildingPlan >= 1) {
                MillLog.major(file, "Loading pict building: " + file.getAbsolutePath());
            }
            String key = file.getName().substring(0, file.getName().length() - 6);
            BuildingPlanSet set = new BuildingPlanSet(culture, key, buildingsVirtualDir, file);
            set.loadPictPlans(false);
            if (file.getParentFile().getName().startsWith("lone")) {
                set.max = 0;
            }
            plans.put(set.key, set);
        } catch (Exception e) {
            MillLog.printException("Exception when loading " + file.getName() + " plan set in culture " + culture.key + ":", e);
        }
    }
}
