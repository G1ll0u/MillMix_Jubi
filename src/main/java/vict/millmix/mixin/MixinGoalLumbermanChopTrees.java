package vict.millmix.mixin;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumHand;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.GoalLumbermanChopTrees;
import org.millenaire.common.utilities.MillCommonUtilities;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.Point;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import vict.millmix.MillMixModConfig;


@Mixin(GoalLumbermanChopTrees.class)
public class MixinGoalLumbermanChopTrees {

    /**
     * Make chop radius bigger, but safely:
     * - snapshot IBlockState before changing blocks
     * - never compute leaf type from a state that might already be AIR
     * - reduce neighbor-update cascades while we manually handle drops
     */
    @Overwrite(remap = false)
    public boolean performAction(MillVillager villager) throws Exception {
        boolean woodFound = false;

        if (MillConfigValues.LogLumberman >= 3) {
            MillLog.debug(this, "Attempting to gather wood around: " + villager.getGoalDestPoint()
                    + ", central block: " + villager.getGoalDestPoint().getBlock(villager.world));
        }

        final int radiusXZ = MillMixModConfig.chopRadius;
        final int height = 24;

        for (int deltaY = height / 2; deltaY > -height / 2; --deltaY) {
            for (int deltaX = -radiusXZ; deltaX <= radiusXZ; ++deltaX) {
                for (int deltaZ = -radiusXZ; deltaZ <= radiusXZ; ++deltaZ) {

                    Point p = villager.getGoalDestPoint().getRelative(deltaX, deltaY, deltaZ);

                    // Snapshot state ONCE and operate from it.
                    // (Do not re-query state after breaking; that’s how you end up with AIR state.)
                    final net.minecraft.util.math.BlockPos pos = p.getBlockPos();
                    final net.minecraft.block.state.IBlockState state = villager.world.getBlockState(pos);
                    final Block block = state.getBlock();

                    final boolean isLog = (block == Blocks.LOG || block == Blocks.LOG2);
                    final boolean isLeaves = (block == Blocks.LEAVES || block == Blocks.LEAVES2);

                    if (!isLog && !isLeaves) {
                        continue;
                    }

                    // If we already chopped one wood block this action, stop so the villager keeps working nearby.
                    if (woodFound) {
                        if (MillConfigValues.LogLumberman >= 3) {
                            MillLog.debug(this, "More wood found.");
                        }
                        return false;
                    }

                    // Derive meta from the SNAPSHOT state.
                    // This remains valid even after we set the world to AIR.
                    final int meta = block.getMetaFromState(state);

                    if (isLeaves) {
                        // Mimic original sapling logic using meta bits (old behavior),
                        // but avoid any leaf drop calls that might explode on mismatched state.
                        if (block == Blocks.LEAVES) {
                            if (MillCommonUtilities.randomInt(4) == 0) {
                                villager.addToInv(Blocks.SAPLING, meta & 3, 1);
                            }
                        } else { // LEAVES2
                            if ((meta & 3) == 0) {
                                if (MillCommonUtilities.randomInt(4) == 0) {
                                    villager.addToInv(Blocks.SAPLING, 4, 1); // acacia
                                }
                            } else {
                                if (MillCommonUtilities.randomInt(2) == 0) {
                                    villager.addToInv(Blocks.SAPLING, 5, 1); // dark oak
                                }
                            }
                        }

                        // Break quietly: we already handle drops.
                        // Flag 2 = send to clients, minimal neighbor updates.
                        villager.world.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
                        villager.swingArm(EnumHand.MAIN_HAND);

                        if (MillConfigValues.LogLumberman >= 3) {
                            MillLog.debug(this, "Destroyed leaves at: " + p);
                        }

                    } else { // logs
                        villager.world.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
                        villager.swingArm(EnumHand.MAIN_HAND);

                        // Original code used meta & 3 for wood type.
                        // Keep same behavior for inventory items.
                        final int woodType = meta & 3;

                        if (block == Blocks.LOG) {
                            villager.addToInv(Blocks.LOG, woodType, 1);
                        } else {
                            villager.addToInv(Blocks.LOG2, woodType, 1);
                        }

                        woodFound = true;

                        if (MillConfigValues.LogLumberman >= 3) {
                            MillLog.debug(this, "Gathered wood at: " + p);
                        }
                    }
                }
            }
        }

        return true;
    }
}
