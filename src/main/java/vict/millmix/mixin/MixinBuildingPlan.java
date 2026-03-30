package vict.millmix.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLog;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.millenaire.common.buildingplan.BuildingPlan;
import org.millenaire.common.pathing.atomicstryker.RegionMapper;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.village.BuildingLocation;
import org.millenaire.common.village.VillageMapInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vict.millmix.MillMixModConfig;

import java.util.Random;

@Mixin(BuildingPlan.class)
public class MixinBuildingPlan {

    @Inject(
            method = "testSpot(Lorg/millenaire/common/village/VillageMapInfo;Lorg/millenaire/common/pathing/atomicstryker/RegionMapper;Lorg/millenaire/common/utilities/Point;IILjava/util/Random;IZ)Lorg/millenaire/common/buildingplan/BuildingPlan$LocationReturn;",
            at = @At("RETURN"),
            cancellable = true,
            remap = false
    )
    private void preventMountainEdgePlacement(
            VillageMapInfo winfo,
            RegionMapper regionMapper,
            Point centre,
            int x,
            int z,
            Random random,
            int porientation,
            boolean ignoreExtraConstraints,
            CallbackInfoReturnable<BuildingPlan.LocationReturn> cir
    ) {
        BuildingPlan.LocationReturn returnValue = cir.getReturnValue();
        if (returnValue == null || returnValue.location == null) return;

        BuildingLocation loc = returnValue.location;
        BuildingPlan plan = loc.getPlan();

        int orientation = porientation == -1
                ? BuildingPlan.computeOrientation(new Point(x + winfo.mapStartX, 0.0, z + winfo.mapStartZ), centre)
                : porientation;
        orientation = (orientation + plan.buildingOrientation) % 4;

        int xwidth, zwidth;
        if (orientation == 0 || orientation == 2) {
            xwidth = plan.length + plan.areaToClearLengthBefore + plan.areaToClearLengthAfter + 2;
            zwidth = plan.width + plan.areaToClearWidthBefore + plan.areaToClearWidthAfter + 2;
        } else {
            xwidth = plan.width + plan.areaToClearWidthBefore + plan.areaToClearWidthAfter + 2;
            zwidth = plan.length + plan.areaToClearLengthBefore + plan.areaToClearLengthAfter + 2;
        }

        // --- Slope check (tree/foliage-safe) ---
        int minHeight = Integer.MAX_VALUE;
        int maxHeight = Integer.MIN_VALUE;

        for (int dx = -xwidth / 2; dx <= xwidth / 2; dx++) {
            for (int dz = -zwidth / 2; dz <= zwidth / 2; dz++) {
                int i = x + dx;
                int j = z + dz;

                if (i < 0 || i >= winfo.length || j < 0 || j >= winfo.width) continue;

                int h = getEffectiveGroundY(winfo, i, j);

                if (h < minHeight) minHeight = h;
                if (h > maxHeight) maxHeight = h;
            }
        }

        // If footprint had no valid samples for some reason, do nothing.
        if (minHeight == Integer.MAX_VALUE || maxHeight == Integer.MIN_VALUE) return;

        int maxSlope = MillMixModConfig.maxTerrainHeightDiff;
        if (maxHeight - minHeight > maxSlope) {
            cir.setReturnValue(new BuildingPlan.LocationReturn(9, loc.pos));
        }
    }

    /**
     * Returns a "ground Y" that ignores trees/leaves/foliage columns.
     * Fast path: use winfo.topGround unless the column looks like a tree/foliage column.
     */
    private static int getEffectiveGroundY(VillageMapInfo winfo, int i, int j) {
        int y = winfo.topGround[i][j];

        // If we can’t rescan world, fall back to original behavior.
        World world = winfo.world;
        if (world == null) return y;

        int worldX = i + winfo.mapStartX;
        int worldZ = j + winfo.mapStartZ;

        // Decide whether to rescan: if map thinks "tree" OR block below looks like log/leaves/foliage.
        if (!winfo.tree[i][j] && !looksLikeTreeOrFoliageBelow(world, worldX, y, worldZ)) {
            return y;
        }

        // Rescan down from (y-1) to find real terrain top.
        return findRealGroundTopY(world, worldX, y, worldZ);
    }

    private static boolean looksLikeTreeOrFoliageBelow(World world, int x, int topY, int z) {
        int yBelow = clampY(topY - 1);
        IBlockState below = world.getBlockState(new BlockPos(x, yBelow, z));
        Block b = below.getBlock();

        // Common offenders for "topGround" being a treetop/plant top
        if (b instanceof BlockLog) return true;
        if (b instanceof BlockLeaves) return true;
        if (b == Blocks.LOG || b == Blocks.LOG2) return true;
        if (b == Blocks.LEAVES || b == Blocks.LEAVES2) return true;

        // Also treat replaceable-ish stuff as suspicious
        if (b == Blocks.TALLGRASS || b == Blocks.DOUBLE_PLANT || b == Blocks.SAPLING) return true;
        if (b == Blocks.VINE) return true;
        return b == Blocks.SNOW_LAYER;
    }

    /**
     * Walks downward to find the first "real ground" block (solid, not log/leaves/foliage),
     * then returns the Y of the air block above it (like topGround is supposed to represent).
     */
    private static int findRealGroundTopY(World world, int x, int approxTopY, int z) {
        int y = clampY(approxTopY);

        // Start from the block under "air above ground"
        int scanY = clampY(y - 1);

        // Hard stop to avoid stupid loops; world is 0..255 in 1.12.
        for (int k = 0; k < 80 && scanY > 1; k++) {
            BlockPos pos = new BlockPos(x, scanY, z);
            IBlockState state = world.getBlockState(pos);
            Block b = state.getBlock();

            // Skip obvious non-ground / tree column blocks
            if (isIgnoredSurfaceBlock(b, state)) {
                scanY--;
                continue;
            }

            // If it's a solid block you can build on, we found terrain.
            // Return "air above it" => scanY + 1
            if (isBuildableGroundBlock(b, state)) {
                return clampY(scanY + 1);
            }

            // Otherwise keep going down (covers weird modded plants/non-solids)
            scanY--;
        }

        // Fallback: if scan failed, return original approx
        return y;
    }

    private static int clampY(int y) {
        if (y < 1) return 1;
        if (y > 255) return 255;
        return y;
    }

    private static boolean isIgnoredSurfaceBlock(Block b, IBlockState state) {
        if (b == Blocks.AIR) return true;

        if (b instanceof BlockLog) return true;
        if (b instanceof BlockLeaves) return true;

        if (b == Blocks.LOG || b == Blocks.LOG2) return true;
        if (b == Blocks.LEAVES || b == Blocks.LEAVES2) return true;

        if (b == Blocks.TALLGRASS || b == Blocks.DOUBLE_PLANT || b == Blocks.SAPLING) return true;
        if (b == Blocks.VINE) return true;

        // Snow layer sits on top of ground; ignore it for height sampling.
        return b == Blocks.SNOW_LAYER;
    }

    private static boolean isBuildableGroundBlock(Block b, IBlockState state) {
        // Reject liquids as "ground"
        if (state.getMaterial().isLiquid()) return false;

        // Must be solid enough to count as terrain
        // (covers dirt/grass/stone/path/farmland/etc.)
        return state.getMaterial().isSolid();
    }
}
