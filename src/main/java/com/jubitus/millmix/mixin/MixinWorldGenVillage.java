package com.jubitus.millmix.mixin;

import com.jubitus.millmix.MillMixModConfig;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.millenaire.common.culture.VillageType;
import org.millenaire.common.culture.WallType;
import org.millenaire.common.pathing.atomicstryker.RegionMapper;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.village.BuildingLocation;
import org.millenaire.common.village.VillageMapInfo;
import org.millenaire.common.world.VillageWallGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Mixin(VillageWallGenerator.class)
public class MixinWorldGenVillage {
    @Inject(
            method = "computeWallBuildingLocations",
            at = @At("RETURN"),
            cancellable = true,
            remap = false
    )
    private void filterUnflatWallLocations(
            VillageType villageType,
            WallType wallType,
            int radius,
            RegionMapper regionMapper,
            Point center,
            VillageMapInfo winfo,
            CallbackInfoReturnable<List<BuildingLocation>> cir
    ) {
        List<BuildingLocation> original = cir.getReturnValue();
        if (original == null || original.isEmpty()) return;

        List<BuildingLocation> filtered = new ArrayList<>();
        int maxSlope = MillMixModConfig.maxWallTerrainHeightDiff;
        int checkRadius = MillMixModConfig.maxWallCheckRadius; // Configurable footprint radius

        for (BuildingLocation loc : original) {
            Point pos = loc.pos;
            int x = (int) (pos.x - winfo.mapStartX);
            int z = (int) (pos.z - winfo.mapStartZ);

            if (isFlatEnough(winfo, x, z, checkRadius, maxSlope)) {
                // Compute average height and adjust Y-level
                int avgHeight = getAverageHeight(winfo, x, z, checkRadius);
                loc.pos = new Point(pos.x, avgHeight, pos.z);
                filtered.add(loc);
            }
        }

        cir.setReturnValue(filtered);
    }

    private boolean isFlatEnough(VillageMapInfo winfo, int x, int z, int radius, int maxSlope) {
        int minHeight = Integer.MAX_VALUE;
        int maxHeight = Integer.MIN_VALUE;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int i = x + dx;
                int j = z + dz;

                if (i < 0 || i >= winfo.length || j < 0 || j >= winfo.width) continue;

                int h = winfo.topGround[i][j];
                minHeight = Math.min(minHeight, h);
                maxHeight = Math.max(maxHeight, h);
            }
        }

        return (maxHeight - minHeight) <= maxSlope;
    }

    private int getAverageHeight(VillageMapInfo winfo, int x, int z, int radius) {
        int totalHeight = 0;
        int count = 0;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int i = x + dx;
                int j = z + dz;

                if (i < 0 || i >= winfo.length || j < 0 || j >= winfo.width) continue;

                totalHeight += winfo.topGround[i][j];
                count++;
            }
        }

        return count > 0 ? totalHeight / count : 0;
    }
}