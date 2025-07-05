package com.jubitus.millmix.mixin;

import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.millenaire.common.annotedparameters.AnnotedParameter;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.Goal;
import org.millenaire.common.goal.generic.GoalGenericHarvestCrop;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.utilities.MillCommonUtilities;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.village.Building;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;

@Mixin(GoalGenericHarvestCrop.class) // The class I want to modify
public abstract class MixinGoalGenericHarvestCrop extends MixinGoalGeneric {
    private boolean soilExistsAt(World world, Point p) {
        BlockPos pos = new BlockPos(p.x, p.y, p.z);
        IBlockState state = world.getBlockState(pos);

        // Adjust this logic to match your crop soil block type
        return state.getBlock() == Blocks.FARMLAND;
    }


    //shadow private class

    @Shadow(remap = false)
    protected abstract boolean isValidHarvestSoil(World world, Point p);
    @Shadow(remap = false)
    public ResourceLocation cropType = null;
    @Shadow(remap = false)
    public List<AnnotedParameter.BonusItem> harvestItem = new ArrayList();


    /**
     * idea: change millvillager's toggleDoor to use BlockDoor's toggleDoor
     * @author Jubitus
     * @reason Make villagers wait for crops to get ripe
     */
    @Overwrite(remap = false)
    public Goal.GoalInformation getDestination(MillVillager villager) throws MillLog.MillenaireException {
        Point dest = null;
        Building destBuilding = null;

        for(Building buildingDest : this.getBuildings(villager)) {
            List<Point> soils;
            if (this.isDestPossible(villager, buildingDest) && (soils = buildingDest.getResManager().getSoilPoints(this.cropType)) != null && !soils.isEmpty()) {
                boolean isSmallField = soils.size() < 64;
                float minStartPercent = isSmallField ? 0.99F : 0.9F;
                float minContinuePercent = isSmallField ? 0.01F : 0.03F;
                int existingSoilCount = 0;
                int ripeCrops = 0;
                Point nearest = null;

                for (Point p : soils) {
                    if (this.soilExistsAt(villager.world, p)) {
                        ++existingSoilCount;

                        if (this.isValidHarvestSoil(villager.world, p)) {
                            ++ripeCrops;

                            if (nearest == null || p.distanceTo(villager) < nearest.distanceTo(villager)) {
                                nearest = p;
                            }
                        }
                    }
                }

                float ripePercent = existingSoilCount > 0 ? (float)ripeCrops / (float)existingSoilCount : 0.0F;

                boolean alreadyHarvesting = villager.getGoalBuildingDest() != null && villager.getGoalBuildingDest().equals(buildingDest);
                boolean allowHarvesting = ripePercent >= minStartPercent || alreadyHarvesting && ripePercent > minContinuePercent;
                if (allowHarvesting && nearest != null) {
                    dest = nearest.getAbove();
                    destBuilding = buildingDest;
                    break;
                }
            }
        }

        return dest == null ? null : this.packDest(dest, destBuilding);
    }
    /**
     * idea: change millvillager's toggleDoor to use BlockDoor's toggleDoor
     * @author Jubitus
     * @reason Make villagers wait for crops to get ripe
     */
    @Overwrite(remap = false)
    public void applyDefaultSettings() {
        this.duration = 10;
        this.lookAtGoal = true;
        this.reoccurDelay = 15000;
        this.tags.add("tag_agriculture");
    }
}



