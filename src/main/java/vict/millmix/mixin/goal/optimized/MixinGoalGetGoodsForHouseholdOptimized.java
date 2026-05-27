package vict.millmix.mixin.goal.optimized;

import vict.millmix.mixin.goal.MixinGoal;

import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.Goal;
import org.millenaire.common.goal.GoalGetGoodsForHousehold;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.village.Building;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import java.util.*;

@Mixin(value = GoalGetGoodsForHousehold.class, remap = false)
public abstract class MixinGoalGetGoodsForHouseholdOptimized extends MixinGoal {

    /*
     * Same-tick cache per villager.
     * No inner classes, just parallel maps.
     */
    @Unique
    private final WeakHashMap<MillVillager, Long> millmix$cacheTick = new WeakHashMap<>();
    @Unique
    private final WeakHashMap<MillVillager, Point> millmix$cacheHousePos = new WeakHashMap<>();
    @Unique
    private final WeakHashMap<MillVillager, Point> millmix$cacheTownHallPos = new WeakHashMap<>();

    @Unique
    private final WeakHashMap<MillVillager, Boolean> millmix$destFalseComputed = new WeakHashMap<>();
    @Unique
    private final WeakHashMap<MillVillager, Goal.GoalInformation> millmix$destFalseValue = new WeakHashMap<>();

    @Unique
    private final WeakHashMap<MillVillager, Boolean> millmix$destTrueComputed = new WeakHashMap<>();
    @Unique
    private final WeakHashMap<MillVillager, Goal.GoalInformation> millmix$destTrueValue = new WeakHashMap<>();

    @Unique
    private final WeakHashMap<MillVillager, Boolean> millmix$priorityComputed = new WeakHashMap<>();
    @Unique
    private final WeakHashMap<MillVillager, Integer> millmix$priorityValue = new WeakHashMap<>();

    @Overwrite(remap = false)
    public Goal.GoalInformation getDestination(final MillVillager villager) throws Exception {
        return this.getDestination(villager, false);
    }

    @Overwrite(remap = false)
    public Goal.GoalInformation getDestination(final MillVillager villager, final boolean test) throws Exception {
        this.millmix$invalidateIfNeeded(villager);

        if (test) {
            final Boolean computed = this.millmix$destTrueComputed.get(villager);
            if (computed != null && computed.booleanValue()) {
                return this.millmix$destTrueValue.get(villager);
            }

            final Goal.GoalInformation result = this.millmix$computeDestination(villager, true);
            this.millmix$destTrueComputed.put(villager, Boolean.TRUE);
            this.millmix$destTrueValue.put(villager, result);
            return result;
        } else {
            final Boolean computed = this.millmix$destFalseComputed.get(villager);
            if (computed != null && computed.booleanValue()) {
                return this.millmix$destFalseValue.get(villager);
            }

            final Goal.GoalInformation result = this.millmix$computeDestination(villager, false);
            this.millmix$destFalseComputed.put(villager, Boolean.TRUE);
            this.millmix$destFalseValue.put(villager, result);
            return result;
        }
    }

    @Unique
    private void millmix$invalidateIfNeeded(final MillVillager villager) {
        final long now = villager.world.getTotalWorldTime();
        final Point housePos = villager.getHouse() != null ? villager.getHouse().getPos() : null;
        final Point townHallPos = villager.getTownHall() != null ? villager.getTownHall().getPos() : null;

        final Long oldTick = this.millmix$cacheTick.get(villager);
        final Point oldHousePos = this.millmix$cacheHousePos.get(villager);
        final Point oldTownHallPos = this.millmix$cacheTownHallPos.get(villager);

        final boolean sameTick = oldTick != null && oldTick.longValue() == now;
        final boolean sameHouse = Objects.equals(oldHousePos, housePos);
        final boolean sameTownHall = Objects.equals(oldTownHallPos, townHallPos);

        if (sameTick && sameHouse && sameTownHall) {
            return;
        }

        this.millmix$cacheTick.put(villager, now);
        this.millmix$cacheHousePos.put(villager, housePos);
        this.millmix$cacheTownHallPos.put(villager, townHallPos);

        this.millmix$destFalseComputed.remove(villager);
        this.millmix$destFalseValue.remove(villager);

        this.millmix$destTrueComputed.remove(villager);
        this.millmix$destTrueValue.remove(villager);

        this.millmix$priorityComputed.remove(villager);
        this.millmix$priorityValue.remove(villager);
    }

    @Unique
    private Goal.GoalInformation millmix$computeDestination(final MillVillager villager, final boolean test) throws Exception {
        final Building house = villager.getHouse();
        final Building townHall = villager.getTownHall();

        if (house == null || townHall == null) {
            return null;
        }

        List<Building> buildings = null;

        final boolean delayOver =
                !test
                        || !villager.lastGoalTime.containsKey(this)
                        || villager.world.getWorldTime() > villager.lastGoalTime.get(this) + 2000L;

        final Map<InvItem, Integer> houseCountCache = new HashMap<>();
        final IdentityHashMap<Building, HashMap<InvItem, Integer>> availabilityCache = new IdentityHashMap<>();

        for (MillVillager resident : house.getKnownVillagers()) {
            final HashMap<InvItem, Integer> goods = resident.requiresGoods();
            int nb = 0;

            for (InvItem key : goods.keySet()) {
                if (this.millmix$getHouseCountCached(house, key, houseCountCache) >= goods.get(key).intValue() / 2) {
                    continue;
                }

                if (buildings == null) {
                    buildings = townHall.getBuildings();
                }

                for (Building building : buildings) {
                    if (building == house) {
                        continue;
                    }

                    final int nbav = this.millmix$getAvailabilityCached(building, key, availabilityCache);
                    if (nbav <= 0) {
                        continue;
                    }

                    if (!delayOver) {
                        nb += nbav;
                        if (nb <= 16) {
                            continue;
                        }
                    }

                    return this.packDest(building.getResManager().getSellingPos(), building);
                }
            }
        }

        return null;
    }

    @Unique
    private int millmix$getHouseCountCached(final Building house,
                                            final InvItem key,
                                            final Map<InvItem, Integer> houseCountCache) {
        final Integer cached = houseCountCache.get(key);
        if (cached != null) {
            return cached.intValue();
        }

        final int count = house.countGoods(key.getItem(), key.meta);
        houseCountCache.put(key, Integer.valueOf(count));
        return count;
    }

    @Unique
    private int millmix$getAvailabilityCached(final Building building,
                                              final InvItem key,
                                              final IdentityHashMap<Building, HashMap<InvItem, Integer>> availabilityCache) {
        HashMap<InvItem, Integer> byItem = availabilityCache.get(building);
        if (byItem == null) {
            byItem = new HashMap<>();
            availabilityCache.put(building, byItem);
        }

        final Integer cached = byItem.get(key);
        if (cached != null) {
            return cached.intValue();
        }

        final int value = building.nbGoodAvailable(key, false, false, false);
        byItem.put(key, Integer.valueOf(value));
        return value;
    }

    @Overwrite(remap = false)
    public boolean isPossibleSpecific(final MillVillager villager) throws Exception {
        return this.getDestination(villager, true) != null;
    }

    @Overwrite(remap = false)
    public boolean lookAtGoal() {
        return true;
    }

    @Overwrite(remap = false)
    public String nextGoal(final MillVillager villager) throws Exception {
        return Goal.deliverGoodsHousehold.key;
    }

    @Overwrite(remap = false)
    public boolean performAction(final MillVillager villager) throws Exception {
        final Building shop = villager.getGoalBuildingDest();
        final Building house = villager.getHouse();

        if (shop == null || shop == house || house == null) {
            return true;
        }

        final Map<InvItem, Integer> houseCountCache = new HashMap<>();
        final Map<InvItem, Integer> shopAvailabilityCache = new HashMap<>();

        for (MillVillager resident : house.getKnownVillagers()) {
            final HashMap<InvItem, Integer> goods = resident.requiresGoods();

            for (InvItem key : goods.keySet()) {
                final int required = goods.get(key).intValue();
                final int currentHouseCount = this.millmix$getHouseCountCached(house, key, houseCountCache);

                if (currentHouseCount >= required) {
                    continue;
                }

                Integer available = shopAvailabilityCache.get(key);
                if (available == null) {
                    available = Integer.valueOf(shop.nbGoodAvailable(key, false, false, false));
                    shopAvailabilityCache.put(key, available);
                }

                final int missing = required - currentHouseCount;
                final int nb = Math.min(available.intValue(), missing);
                if (nb <= 0) {
                    continue;
                }

                villager.takeFromBuilding(shop, key.getItem(), key.meta, nb);

                houseCountCache.put(key, Integer.valueOf(currentHouseCount + nb));
                shopAvailabilityCache.put(key, Integer.valueOf(available.intValue() - nb));
            }
        }

        return true;
    }

    @Overwrite(remap = false)
    public int priority(final MillVillager villager) throws Exception {
        this.millmix$invalidateIfNeeded(villager);

        final Boolean computed = this.millmix$priorityComputed.get(villager);
        if (computed != null && computed.booleanValue()) {
            final Integer cached = this.millmix$priorityValue.get(villager);
            return cached == null ? 0 : cached.intValue();
        }

        final int result = this.millmix$computePriority(villager);
        this.millmix$priorityComputed.put(villager, Boolean.TRUE);
        this.millmix$priorityValue.put(villager, Integer.valueOf(result));
        return result;
    }

    @Unique
    private int millmix$computePriority(final MillVillager villager) throws Exception {
        final Building house = villager.getHouse();
        final Building townHall = villager.getTownHall();

        if (house == null || townHall == null) {
            return 0;
        }

        final List<Building> shops = townHall.getShops();
        if (shops == null || shops.isEmpty()) {
            return 0;
        }

        final Map<InvItem, Integer> houseCountCache = new HashMap<>();
        final IdentityHashMap<Building, HashMap<InvItem, Integer>> availabilityCache = new IdentityHashMap<>();

        int nb = 0;

        for (MillVillager resident : house.getKnownVillagers()) {
            final HashMap<InvItem, Integer> goods = resident.requiresGoods();

            for (InvItem key : goods.keySet()) {
                if (this.millmix$getHouseCountCached(house, key, houseCountCache) >= goods.get(key).intValue() / 2) {
                    continue;
                }

                for (Building shop : shops) {
                    if (shop == house) {
                        continue;
                    }

                    final int nbav = this.millmix$getAvailabilityCached(shop, key, availabilityCache);
                    if (nbav > 0) {
                        nb += nbav;
                    }
                }
            }
        }

        return nb * 20;
    }
}