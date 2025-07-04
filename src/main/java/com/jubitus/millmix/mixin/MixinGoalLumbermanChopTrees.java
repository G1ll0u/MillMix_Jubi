package com.jubitus.millmix.mixin;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumHand;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.GoalLumbermanChopTrees;
import org.millenaire.common.utilities.MillCommonUtilities;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.utilities.WorldUtilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;


@Mixin(GoalLumbermanChopTrees.class)
public class MixinGoalLumbermanChopTrees {
    /**
     * idea: change millvillager's toggleDoor to use BlockDoor's toggleDoor
     *
     * @author Jubitus
     * @reason Tweak Lumberman goal
     */
    @Overwrite(remap = false)
    public boolean performAction(MillVillager villager) throws Exception {
        boolean woodFound = false;
        if (MillConfigValues.LogLumberman >= 3) {
            MillLog.debug(this, "Attempting to gather wood around: " + villager.getGoalDestPoint() + ", central block: " + villager.getGoalDestPoint().getBlock(villager.world));
        }

        int radiusXZ = 10;
        int height = 24;
        for (int deltaY = height / 2; deltaY > -height / 2; --deltaY) {
            for (int deltaX = -radiusXZ; deltaX <= radiusXZ; ++deltaX) {
                for (int deltaZ = -radiusXZ; deltaZ <= radiusXZ; ++deltaZ) {
                    Point p = villager.getGoalDestPoint().getRelative((double)deltaX, (double)deltaY, (double)deltaZ);
                    Block block = villager.getBlock(p);
                    if (block == Blocks.LOG || block == Blocks.LOG2 || block == Blocks.LEAVES || block == Blocks.LEAVES2) {
                        if (woodFound) {
                            if (MillConfigValues.LogLumberman >= 3) {
                                MillLog.debug(this, "More wood found.");
                            }

                            return false;
                        }

                        if (block != Blocks.LOG && block != Blocks.LOG2) {
                            int meta = WorldUtilities.getBlockMeta(villager.world, p);
                            if (block == Blocks.LEAVES) {
                                if (MillCommonUtilities.randomInt(4) == 0) {
                                    villager.addToInv(Blocks.SAPLING, meta & 3, 1);
                                }
                            } else if ((meta & 3) == 0) {
                                if (MillCommonUtilities.randomInt(4) == 0) {
                                    villager.addToInv(Blocks.SAPLING, 4, 1);
                                }
                            } else if (MillCommonUtilities.randomInt(2) == 0) {
                                villager.addToInv(Blocks.SAPLING, 5, 1);
                            }

                            villager.setBlock(p, Blocks.AIR);
                            villager.swingArm(EnumHand.MAIN_HAND);
                            if (MillConfigValues.LogLumberman >= 3) {
                                MillLog.debug(this, "Destroyed leaves at: " + p);
                            }
                        } else {
                            int meta = villager.getBlockMeta(p) & 3;
                            villager.setBlock(p, Blocks.AIR);
                            villager.swingArm(EnumHand.MAIN_HAND);
                            if (block == Blocks.LOG) {
                                villager.addToInv(Blocks.LOG, meta, 1);
                            } else {
                                villager.addToInv(Blocks.LOG2, meta, 1);
                            }

                            woodFound = true;
                            if (MillConfigValues.LogLumberman >= 3) {
                                MillLog.debug(this, "Gathered wood at: " + p);
                            }
                        }
                    }
                }
            }
        }

        return true;
    }
}
