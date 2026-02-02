package com.jubitus.millmix.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.millenaire.common.annotedparameters.AnnotedParameter;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.Goal;
import org.millenaire.common.goal.generic.GoalGenericHarvestCrop;
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
    @Shadow(remap = false)
    public ResourceLocation cropType = null;


    //shadow private class
    @Shadow(remap = false)
    public List<AnnotedParameter.BonusItem> harvestItem = new ArrayList();

    /**
     * idea: Make villagers wait for ripe crops
     *
     * @author Jubitus
     * @reason Villagers always rush on the first ripe crop instead of waiting for all crops to be ripe.
     */
    @Overwrite(remap = false)
    public Goal.GoalInformation getDestination(MillVillager villager) throws MillLog.MillenaireException {
        Point dest = null;
        Building destBuilding = null;

        for (Building buildingDest : this.getBuildings(villager)) {
            List<Point> soils;
            if (this.isDestPossible(villager, buildingDest) && (soils = buildingDest.getResManager().getSoilPoints(this.cropType)) != null && !soils.isEmpty()) {
                boolean isSmallField = soils.size() < 200;
                float minStartPercent = 0.9F;
                float minContinuePercent = isSmallField ? 0.01F : 0.03F;
                int existingSoilCount = 0;
                int ripeCrops = 0;
                Point nearest = null;

                for (Point p : soils) {
                    if (this.soilExistsAt(villager.world, p)) {
                        // Skip soil with no crop above (invalidate)
                        if (!hasCropAbove(villager.world, p)) {
                            continue;
                        }

                        ++existingSoilCount;

                        if (this.isValidHarvestSoil(villager.world, p)) {
                            ++ripeCrops;

                            if (nearest == null || p.distanceTo(villager) < nearest.distanceTo(villager)) {
                                nearest = p;
                            }
                        }
                    }
                }

                float ripePercent = existingSoilCount > 0 ? (float) ripeCrops / (float) existingSoilCount : 0.0F;

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

    private boolean soilExistsAt(World world, Point p) {
        BlockPos pos = new BlockPos(p.x, p.y, p.z);
        IBlockState state = world.getBlockState(pos);

        // Adjust this logic to match your crop soil block type
        return state.getBlock() == Blocks.FARMLAND;
    }

    private boolean hasCropAbove(World world, Point p) {
        Block blockAbove = p.getAbove().getBlock(world);
        // Check if blockAbove is air (no crop)
        return blockAbove != Blocks.AIR;
    }

    @Shadow(remap = false)
    protected abstract boolean isValidHarvestSoil(World world, Point p);

    /**
     * idea: Make villagers harvest crop slower
     *
     * @author Jubitus
     * @reason Immersion
     */
    @Overwrite(remap = false)
    public void applyDefaultSettings() {
        this.duration = 10;
        this.lookAtGoal = true;
        this.reoccurDelay = 15000;
        this.tags.add("tag_agriculture");
    }

}



