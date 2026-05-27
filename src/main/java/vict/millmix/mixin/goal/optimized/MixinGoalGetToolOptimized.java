package vict.millmix.mixin.goal.optimized;

import vict.millmix.mixin.goal.MixinGoal;

import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.Goal;
import org.millenaire.common.goal.GoalGetTool;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.village.Building;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import java.util.WeakHashMap;

@Mixin(value = GoalGetTool.class, remap = false)
public abstract class MixinGoalGetToolOptimized extends MixinGoal {

    @Unique private final WeakHashMap<MillVillager, Long> millmix$cacheTick = new WeakHashMap<>();
    @Unique private final WeakHashMap<MillVillager, Boolean> millmix$cacheComputed = new WeakHashMap<>();
    @Unique private final WeakHashMap<MillVillager, Goal.GoalInformation> millmix$cacheValue = new WeakHashMap<>();

    @Overwrite(remap = false)
    public Goal.GoalInformation getDestination(final MillVillager villager) throws Exception {
        final long now = villager.world.getTotalWorldTime();
        final Long lastTick = this.millmix$cacheTick.get(villager);
        if (lastTick != null && lastTick.longValue() == now) {
            final Boolean computed = this.millmix$cacheComputed.get(villager);
            if (Boolean.TRUE.equals(computed)) {
                return this.millmix$cacheValue.get(villager);
            }
        }
        final Goal.GoalInformation result = this.millmix$compute(villager);
        this.millmix$cacheTick.put(villager, now);
        this.millmix$cacheComputed.put(villager, Boolean.TRUE);
        this.millmix$cacheValue.put(villager, result);
        return result;
    }

    @Unique
    private Goal.GoalInformation millmix$compute(final MillVillager villager) throws MillLog.MillenaireException {
        final Building townHall = villager.getTownHall();
        if (townHall == null) return null;
        for (final Building shop : townHall.getShops()) {
            if (this.millmix$testShopValidity(villager, shop)) {
                return this.packDest(shop.getResManager().getSellingPos(), shop);
            }
        }
        return null;
    }

    @Unique
    private boolean millmix$testShopValidity(final MillVillager villager, final Building shop) throws MillLog.MillenaireException {
        for (final InvItem key : villager.getItemsNeeded()) {
            if (villager.countInv(key.getItem(), key.meta) != 0) continue;
            if (shop.countGoods(key.getItem(), key.meta) <= 0) continue;
            if (!this.validateDest(villager, shop)) continue;
            return true;
        }
        for (final String toolCategory : villager.getToolsCategoriesNeeded()) {
            final InvItem bestItem = villager.getConfig().getBestItemByCategoryName(toolCategory, villager);
            for (final InvItem key : villager.getConfig().categories.get(toolCategory)) {
                if (key == bestItem) break;
                if (shop.countGoods(key.getItem(), key.meta) <= 0) continue;
                if (!this.validateDest(villager, shop)) continue;
                return true;
            }
        }
        return false;
    }

    @Overwrite(remap = false)
    public boolean isPossibleSpecific(final MillVillager villager) throws Exception {
        return this.getDestination(villager) != null;
    }
}
