package com.jubitus.millmix.mixin;


import com.jubitus.millmix.access.IGoalEvalCache;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.Goal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Goal.class, remap = false)
public class MixinGoalEvalMemoize {

    @Inject(method = "isPossible", at = @At("HEAD"), cancellable = true)
    private void mill$memoizeIsPossible(MillVillager villager, CallbackInfoReturnable<Boolean> cir) {
        // world time (ticks)
        final long tick = villager.world.getWorldTime();

        IGoalEvalCache cacheOwner = (IGoalEvalCache) villager;
        IGoalEvalCache.GoalEvalCacheEntry entry = cacheOwner.mill$getGoalEvalCacheEntry((Goal) (Object) this);

        // If we already computed it this tick, reuse
        if (entry.possibleTick == tick) {
            cir.setReturnValue(entry.possibleValue);
        }
    }

    @Inject(method = "isPossible", at = @At("RETURN"))
    private void mill$storeIsPossible(MillVillager villager, CallbackInfoReturnable<Boolean> cir) {
        final long tick = villager.world.getWorldTime();

        IGoalEvalCache cacheOwner = (IGoalEvalCache) villager;
        IGoalEvalCache.GoalEvalCacheEntry entry = cacheOwner.mill$getGoalEvalCacheEntry((Goal) (Object) this);

        entry.possibleTick = tick;
        entry.possibleValue = cir.getReturnValue();
    }

    @Inject(method = "isStillValid", at = @At("HEAD"), cancellable = true)
    private void mill$memoizeIsStillValid(MillVillager villager, CallbackInfoReturnable<Boolean> cir) {
        final long tick = villager.world.getWorldTime();

        IGoalEvalCache cacheOwner = (IGoalEvalCache) villager;
        IGoalEvalCache.GoalEvalCacheEntry entry = cacheOwner.mill$getGoalEvalCacheEntry((Goal) (Object) this);

        if (entry.stillValidTick == tick) {
            cir.setReturnValue(entry.stillValidValue);
        }
    }

    @Inject(method = "isStillValid", at = @At("RETURN"))
    private void mill$storeIsStillValid(MillVillager villager, CallbackInfoReturnable<Boolean> cir) {
        final long tick = villager.world.getWorldTime();

        IGoalEvalCache cacheOwner = (IGoalEvalCache) villager;
        IGoalEvalCache.GoalEvalCacheEntry entry = cacheOwner.mill$getGoalEvalCacheEntry((Goal) (Object) this);

        entry.stillValidTick = tick;
        entry.stillValidValue = cir.getReturnValue();
    }
}
