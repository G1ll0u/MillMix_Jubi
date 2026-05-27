package vict.millmix.mixin.goal;

import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.generic.GoalGeneric;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.village.Building;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;
import java.util.WeakHashMap;

@Mixin(GoalGeneric.class)
public abstract class MixinGoalGeneric extends MixinGoal {
    @Shadow(remap = false)
    public String sound = null;
    @Shadow(remap = false)
    public int duration = 100;
    @Shadow(remap = false)
    public boolean lookAtGoal;
    @Shadow(remap = false)
    public int reoccurDelay = 0;

    @Shadow(remap = false)
    public abstract List<Building> getBuildings(MillVillager villager);

    @Shadow(remap = false)
    public abstract boolean isDestPossible(MillVillager villager, Building dest) throws MillLog.MillenaireException;

    @Shadow(remap = false)
    public abstract boolean isPossibleGenericGoal(MillVillager villager) throws Exception;

    @Unique private final WeakHashMap<MillVillager, Long> millmix$genCacheTick = new WeakHashMap<>();
    @Unique private final WeakHashMap<MillVillager, Boolean> millmix$genCacheComputed = new WeakHashMap<>();
    @Unique private final WeakHashMap<MillVillager, Boolean> millmix$genCacheValue = new WeakHashMap<>();

    @Overwrite(remap = false)
    public final boolean isPossibleSpecific(final MillVillager villager) throws Exception {
        final long now = villager.world.getTotalWorldTime();
        final Long lastTick = this.millmix$genCacheTick.get(villager);
        if (lastTick != null && lastTick.longValue() == now) {
            final Boolean computed = this.millmix$genCacheComputed.get(villager);
            if (Boolean.TRUE.equals(computed)) {
                final Boolean cached = this.millmix$genCacheValue.get(villager);
                return Boolean.TRUE.equals(cached);
            }
        }
        final boolean result = this.millmix$computeIsPossible(villager);
        this.millmix$genCacheTick.put(villager, now);
        this.millmix$genCacheComputed.put(villager, Boolean.TRUE);
        this.millmix$genCacheValue.put(villager, Boolean.valueOf(result));
        return result;
    }

    @Unique
    private boolean millmix$computeIsPossible(final MillVillager villager) throws Exception {
        final Long lastGoalT = villager.lastGoalTime.get(this);
        if (this.reoccurDelay > 0 && lastGoalT != null
                && lastGoalT.longValue() + (long) this.reoccurDelay > villager.world.getWorldTime()) {
            return false;
        }
        if (!this.isPossibleGenericGoal(villager)) {
            return false;
        }
        final List<Building> buildings = this.getBuildings(villager);
        for (int i = 0, size = buildings.size(); i < size; i++) {
            if (this.isDestPossible(villager, buildings.get(i))) {
                return true;
            }
        }
        return false;
    }
}
