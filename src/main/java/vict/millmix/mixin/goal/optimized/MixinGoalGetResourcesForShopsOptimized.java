package vict.millmix.mixin.goal.optimized;

import vict.millmix.mixin.goal.MixinGoal;

import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.Goal;
import org.millenaire.common.goal.GoalGetResourcesForShops;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.village.Building;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import java.util.WeakHashMap;

@Mixin(value = GoalGetResourcesForShops.class, remap = false)
public abstract class MixinGoalGetResourcesForShopsOptimized extends MixinGoal {

    @Unique private final WeakHashMap<MillVillager, Long> millmix$cacheTick = new WeakHashMap<>();
    @Unique private final WeakHashMap<MillVillager, Boolean> millmix$destFalseComputed = new WeakHashMap<>();
    @Unique private final WeakHashMap<MillVillager, Goal.GoalInformation> millmix$destFalseValue = new WeakHashMap<>();
    @Unique private final WeakHashMap<MillVillager, Boolean> millmix$destTrueComputed = new WeakHashMap<>();
    @Unique private final WeakHashMap<MillVillager, Goal.GoalInformation> millmix$destTrueValue = new WeakHashMap<>();

    @Overwrite(remap = false)
    public Goal.GoalInformation getDestination(final MillVillager villager) {
        return this.getDestination(villager, false);
    }

    @Overwrite(remap = false)
    public Goal.GoalInformation getDestination(final MillVillager villager, final boolean test) {
        this.millmix$invalidateIfNeeded(villager);
        if (test) {
            final Boolean computed = this.millmix$destTrueComputed.get(villager);
            if (Boolean.TRUE.equals(computed)) {
                return this.millmix$destTrueValue.get(villager);
            }
            final Goal.GoalInformation result = this.millmix$compute(villager, true);
            this.millmix$destTrueComputed.put(villager, Boolean.TRUE);
            this.millmix$destTrueValue.put(villager, result);
            return result;
        } else {
            final Boolean computed = this.millmix$destFalseComputed.get(villager);
            if (Boolean.TRUE.equals(computed)) {
                return this.millmix$destFalseValue.get(villager);
            }
            final Goal.GoalInformation result = this.millmix$compute(villager, false);
            this.millmix$destFalseComputed.put(villager, Boolean.TRUE);
            this.millmix$destFalseValue.put(villager, result);
            return result;
        }
    }

    @Unique
    private void millmix$invalidateIfNeeded(final MillVillager villager) {
        final long now = villager.world.getTotalWorldTime();
        final Long lastTick = this.millmix$cacheTick.get(villager);
        if (lastTick != null && lastTick.longValue() == now) return;
        this.millmix$cacheTick.put(villager, now);
        this.millmix$destFalseComputed.remove(villager);
        this.millmix$destFalseValue.remove(villager);
        this.millmix$destTrueComputed.remove(villager);
        this.millmix$destTrueValue.remove(villager);
    }

    @Unique
    private Goal.GoalInformation millmix$compute(final MillVillager villager, final boolean test) {
        final Building townHall = villager.getTownHall();
        if (townHall == null) return null;

        final boolean delayOver = !test
                || !villager.lastGoalTime.containsKey(this)
                || villager.world.getWorldTime() > villager.lastGoalTime.get(this) + 2000L;

        for (final Building shop : townHall.getShops()) {
            if (!villager.getCulture().shopNeeds.containsKey(shop.location.shop)) continue;
            int nb = 0;
            for (final InvItem item : villager.getCulture().shopNeeds.get(shop.location.shop)) {
                if (shop != villager.getHouse()) {
                    final int nbHouse = villager.getHouse().nbGoodAvailable(item, false, false, true);
                    if (nbHouse > 0 && (delayOver || (nb += nbHouse) > 16)) {
                        return this.packDest(villager.getHouse().getResManager().getSellingPos(), villager.getHouse());
                    }
                }
                if (townHall == shop) continue;
                final int nbTH = townHall.nbGoodAvailable(item, false, false, true);
                if (nbTH <= 0) continue;
                if (!delayOver && (nb += nbTH) <= 16) continue;
                return this.packDest(townHall.getResManager().getSellingPos(), townHall);
            }
        }
        return null;
    }

    @Overwrite(remap = false)
    public boolean isPossibleSpecific(final MillVillager villager) {
        return this.getDestination(villager, true) != null;
    }
}
