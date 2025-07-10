package com.jubitus.millmix.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.millenaire.common.block.BlockGrapeVine;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.Goal;
import org.millenaire.common.goal.generic.GoalGenericPlantCrop;
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

@Mixin(GoalGenericPlantCrop.class)
public abstract class MixinGoalGenericPlantCrop extends MixinGoalGeneric {
    @Shadow(remap = false)
    public InvItem seed = null;
    @Shadow(remap = false)
    public List<IBlockState> plantBlockState = new ArrayList<IBlockState>();
    @Shadow(remap = false)
    public ResourceLocation soilType = null;

    @Shadow(remap = false)
    public abstract boolean isValidPlantingLocation(World world, Point p);

    @Shadow(remap = false)
    public ResourceLocation cropType = null;

    @Shadow(remap = false)
    public abstract Goal.GoalInformation getDestination(MillVillager villager) throws MillLog.MillenaireException;

    /**
     * idea: change millvillager's toggleDoor to use BlockDoor's toggleDoor
     *
     * @author Jubitus
     * @reason Make villagers wait for crops to get ripe
     */
    @Overwrite(remap = false)
    public void applyDefaultSettings() {
        this.duration = 20;
        this.lookAtGoal = true;
        this.tags.add("tag_agriculture");
    }

    /**
     * idea: change millvillager's toggleDoor to use BlockDoor's toggleDoor
     *
     * @author Jubitus
     * @reason Make villagers wait for crops to get ripe
     */
    @Overwrite(remap = false)
    public boolean isDestPossibleSpecific(MillVillager villager, Building b) {
        if (this.seed == null) return true;

        int invCount = villager.countInv(this.seed);
        int fieldCount = b.countGoods(this.seed);

        // New: also check home building
        Building home = villager.getHouse();
        int homeCount = (home != null && home != b) ? home.countGoods(this.seed) : 0;

        return invCount + fieldCount + homeCount > 0;
    }
    /**
     * idea: change millvillager's toggleDoor to use BlockDoor's toggleDoor
     *
     * @author Jubitus
     * @reason Make villagers wait for crops to get ripe
     */
    @Overwrite(remap = false)
    public boolean performAction(MillVillager villager) {
        Building dest = villager.getGoalBuildingDest();
        if (dest == null) {
            return true;
        }

        // Check planting location validity
        if (!this.isValidPlantingLocation(villager.world, villager.getGoalDestPoint().getBelow())) {
            return true;
        }

        int taken = 0;
        if (this.seed != null) {
            // Try villager inventory first
            taken = villager.takeFromInv(this.seed, 1);

            // If none, try field building
            if (taken == 0) {
                taken = dest.takeGoods(this.seed, 1);
            }

            // If still none, try home building if different
            if (taken == 0) {
                Building home = villager.getHouse();
                if (home != null && home != dest) {
                    taken = home.takeGoods(this.seed, 1);
                }
            }

            // If no seeds anywhere, abort planting
            if (taken == 0) {
                return true;
            }
        }

        // Prepare soil block
        Block soil = (Block)Block.REGISTRY.getObject(this.soilType);
        if (villager.getGoalDestPoint().getBelow().getBlock(villager.world) != soil) {
            villager.setBlockAndMetadata(villager.getGoalDestPoint().getBelow(), soil, 0);
        }

        // Place the crop block
        if (!this.plantBlockState.isEmpty()) {
            IBlockState cropState = this.plantBlockState.get(MillCommonUtilities.randomInt(this.plantBlockState.size()));
            villager.setBlockstate(villager.getGoalDestPoint(), cropState);
            if (cropState.getBlock() instanceof BlockDoublePlant) {
                villager.setBlockstate(villager.getGoalDestPoint().getAbove(), cropState.withProperty(BlockDoublePlant.HALF, BlockDoublePlant.EnumBlockHalf.UPPER));
            }
        } else {
            Block cropBlock = (Block)Block.REGISTRY.getObject(this.cropType);
            int cropMeta = GoalGenericPlantCrop.getCropBlockMeta(this.cropType);
            villager.setBlockAndMetadata(villager.getGoalDestPoint(), cropBlock, cropMeta);
            if (cropBlock instanceof BlockDoublePlant || cropBlock instanceof BlockGrapeVine) {
                villager.setBlockAndMetadata(villager.getGoalDestPoint().getAbove(), cropBlock, cropMeta | 8);
            }
        }

        villager.swingArm(EnumHand.MAIN_HAND);

        if (this.isDestPossibleSpecific(villager, villager.getGoalBuildingDest())) {
            try {
                villager.setGoalInformation(this.getDestination(villager));
            } catch (MillLog.MillenaireException e) {
                MillLog.printException(e);
            }
            return false;
        }
        return true;
    }

    // You will need to include other necessary methods/fields from the original class or reference them appropriately.
}

