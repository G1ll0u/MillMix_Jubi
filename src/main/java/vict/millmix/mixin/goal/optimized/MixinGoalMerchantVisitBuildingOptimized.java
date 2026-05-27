package vict.millmix.mixin.goal.optimized;

import vict.millmix.mixin.goal.MixinGoal;

import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.Goal;
import org.millenaire.common.goal.GoalMerchantVisitBuilding;
import org.millenaire.common.item.TradeGood;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.village.Building;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import java.util.HashMap;
import java.util.WeakHashMap;

@Mixin(value = GoalMerchantVisitBuilding.class, remap = false)
public abstract class MixinGoalMerchantVisitBuildingOptimized extends MixinGoal {

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
    private Goal.GoalInformation millmix$compute(final MillVillager villager) throws Exception {
        final Building townHall = villager.getTownHall();
        if (townHall == null) return null;

        for (final TradeGood good : townHall.culture.goodsList) {
            if (good.item.meta < 0) continue;
            final int invCount = villager.countInv(good.item.getItem(), good.item.meta);
            if (invCount <= 0) continue;
            final int nbNeeded = townHall.nbGoodNeeded(good.item.getItem(), good.item.meta);
            if (nbNeeded <= 0) continue;
            if (MillConfigValues.LogMerchant >= 3) {
                MillLog.debug(villager, "TH needs " + nbNeeded + " good " + good.item.getName() + ", merchant has " + invCount);
            }
            return this.packDest(townHall.getResManager().getSellingPos(), townHall);
        }

        final HashMap<TradeGood, Integer> neededGoods = townHall.getImportsNeededbyOtherVillages();
        final Building house = villager.getHouse();

        for (final Building shop : townHall.getShops()) {
            if (shop.isInn) continue;
            for (final TradeGood good : townHall.culture.goodsList) {
                if (good.item.meta < 0) continue;
                final int nbAvail = shop.nbGoodAvailable(good.item.getItem(), good.item.meta, false, true, false);
                if (nbAvail <= 0) continue;
                if (!neededGoods.containsKey(good)) continue;
                final Integer needed = neededGoods.get(good);
                if (needed == null) continue;
                final int houseCount = house != null ? house.countGoods(good.item.getItem(), good.item.meta) : 0;
                if (needed.intValue() <= houseCount + villager.countInv(good.item.getItem(), good.item.meta)) continue;
                if (MillConfigValues.LogMerchant >= 3) {
                    MillLog.debug(villager, "Shop " + shop + " has " + nbAvail + " good to pick up.");
                }
                return this.packDest(shop.getResManager().getSellingPos(), shop);
            }
        }
        return null;
    }

    @Overwrite(remap = false)
    public boolean isPossibleSpecific(final MillVillager villager) throws Exception {
        return this.getDestination(villager) != null;
    }
}
