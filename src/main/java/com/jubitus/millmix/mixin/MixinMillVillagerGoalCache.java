package com.jubitus.millmix.mixin;

import com.jubitus.millmix.access.IGoalEvalCache;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.Goal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.IdentityHashMap;
import java.util.Map;

@Mixin(value = MillVillager.class, remap = false)
public class MixinMillVillagerGoalCache implements IGoalEvalCache {

    /**
     * IdentityHashMap is important: Goal instances are singletons in Goal.goals,
     * so identity-based keys are fast and correct.
     */
    @Unique
    private final Map<Goal, GoalEvalCacheEntry> mill$goalEvalCache = new IdentityHashMap<>();

    @Override
    public GoalEvalCacheEntry mill$getGoalEvalCacheEntry(Goal goal) {
        GoalEvalCacheEntry entry = mill$goalEvalCache.get(goal);
        if (entry == null) {
            entry = new GoalEvalCacheEntry();
            mill$goalEvalCache.put(goal, entry);
        }
        return entry;
    }
}