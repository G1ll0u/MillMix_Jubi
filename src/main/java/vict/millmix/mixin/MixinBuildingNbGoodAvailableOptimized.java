package vict.millmix.mixin;


import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import org.millenaire.common.buildingplan.BuildingPlan;
import org.millenaire.common.culture.Culture;
import org.millenaire.common.culture.VillagerType;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.item.TradeGood;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.village.Building;
import org.millenaire.common.village.BuildingLocation;
import org.millenaire.common.village.ConstructionIP;
import org.millenaire.common.village.VillagerRecord;
import org.millenaire.common.village.buildingmanagers.ResManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.Map;


@Mixin(value = Building.class, remap = false)
public abstract class MixinBuildingNbGoodAvailableOptimized {

    @Shadow(remap = false)
    public Culture culture;
    @Shadow(remap = false)
    public BuildingLocation location;
    @Shadow(remap = false)
    public boolean isTownhall;

    @Overwrite(remap = false)
    public int nbGoodAvailable(IBlockState bs, boolean forConstruction, boolean forExport, boolean forShop) {
        return this.nbGoodAvailable(InvItem.createInvItem(bs), forConstruction, forExport, forShop);
    }

    @Overwrite(remap = false)
    public int nbGoodAvailable(InvItem ii, boolean forConstruction, boolean forExport, boolean forShop) {
        if (this.getResManager().chests.isEmpty()) {
            return 0;
        }

        if (forShop && this.culture.shopNeeds.containsKey(this.location.shop)) {
            final List<InvItem> needs = this.culture.shopNeeds.get(this.location.shop);
            for (int i = 0, size = needs.size(); i < size; i++) {
                final InvItem item = needs.get(i);
                if (item.matches(ii)) {
                    return 0;
                }
            }
        }

        int nb = this.countGoods(ii.getItem(), ii.meta);

        if (this.isTownhall) {
            boolean projectHandled = false;
            final BuildingPlan project = this.getCurrentGoalBuildingPlan();
            final List<ConstructionIP> constructions = this.getConstructionsInProgress();

            for (int i = 0, size = constructions.size(); i < size; i++) {
                final ConstructionIP cip = constructions.get(i);
                if (cip.getBuildingLocation() == null) {
                    continue;
                }

                final BuildingPlan plan = cip.getBuildingLocation().getPlan();

                if (plan != null) {
                    for (Map.Entry<InvItem, Integer> entry : plan.resCost.entrySet()) {
                        final InvItem key = entry.getKey();
                        if (!key.matches(ii)) {
                            continue;
                        }

                        final MillVillager builder = cip.getBuilder();
                        final int builderHas = builder != null ? builder.countInv(key) : 0;
                        final int needed = entry.getValue().intValue();

                        if (builderHas < needed) {
                            nb -= needed - builderHas;
                        }
                    }
                }

                if (project == plan) {
                    projectHandled = true;
                }
            }

            if (!projectHandled && project != null) {
                for (Map.Entry<InvItem, Integer> entry : project.resCost.entrySet()) {
                    final InvItem key = entry.getKey();
                    if (key.matches(ii)) {
                        nb -= entry.getValue().intValue();
                    }
                }
            }
        }

        boolean tradedHere = false;
        if (this.location.shop != null && this.culture.shopSells.containsKey(this.location.shop)) {
            final List<TradeGood> sells = this.culture.shopSells.get(this.location.shop);
            for (int i = 0, size = sells.size(); i < size; i++) {
                final TradeGood g = sells.get(i);
                if (g.item.matches(ii)) {
                    tradedHere = true;
                    break;
                }
            }
        }

        if (!forConstruction && (this.isTownhall || tradedHere || forExport)) {
            for (InvItem key : this.culture.getInvItemsWithTradeGoods()) {
                if (!key.matches(ii)) {
                    continue;
                }

                final TradeGood good = this.culture.getTradeGood(key);
                if (good == null) {
                    continue;
                }

                if (forExport) {
                    nb -= good.targetQuantity;
                } else {
                    nb -= good.reservedQuantity;
                }
            }
        }

        if (!forConstruction) {
            final Point buildingPos = this.getPos();

            for (VillagerRecord vr : this.getVillagerRecords().values()) {
                final Point housePos = vr.getHousePos();
                if (housePos == null || !housePos.equals(buildingPos)) {
                    continue;
                }

                final VillagerType type = vr.getType();
                if (type == null) {
                    continue;
                }

                for (Map.Entry<InvItem, Integer> entry : type.requiredFoodAndGoods.entrySet()) {
                    final InvItem requiredItem = entry.getKey();
                    if (ii.matches(requiredItem)) {
                        nb -= entry.getValue().intValue();
                    }
                }
            }
        }

        return Math.max(nb, 0);
    }

    @Shadow(remap = false)
    public abstract ResManager getResManager();

    @Shadow(remap = false)
    public abstract int countGoods(Item item, int meta);

    @Shadow(remap = false)
    public abstract BuildingPlan getCurrentGoalBuildingPlan();

    @Shadow(remap = false)
    public abstract List<ConstructionIP> getConstructionsInProgress();

    @Shadow(remap = false)
    public abstract Point getPos();

    @Shadow(remap = false)
    public abstract Map<Long, VillagerRecord> getVillagerRecords();

    @Overwrite(remap = false)
    public int nbGoodAvailable(Item item, int meta, boolean forConstruction, boolean forExport, boolean forShop) {
        return this.nbGoodAvailable(InvItem.createInvItem(item, meta), forConstruction, forExport, forShop);
    }
}