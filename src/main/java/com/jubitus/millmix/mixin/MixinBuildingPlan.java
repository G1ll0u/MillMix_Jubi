package com.jubitus.millmix.mixin;

import com.jubitus.millmix.ModConfig;
import org.millenaire.common.buildingplan.BuildingPlan;
import org.millenaire.common.pathing.atomicstryker.RegionMapper;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.village.VillageMapInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

@Mixin(BuildingPlan.class)
public class MixinBuildingPlan {
    @Inject(
            method = "testSpot(Lorg/millenaire/common/village/VillageMapInfo;Lorg/millenaire/common/pathing/atomicstryker/RegionMapper;Lorg/millenaire/common/utilities/Point;IILjava/util/Random;IZ)Lorg/millenaire/common/buildingplan/BuildingPlan$LocationReturn;",
            at = @At("RETURN"),
            cancellable = true,
            remap = false
    )
    private void preventMountainEdgePlacement(VillageMapInfo winfo, RegionMapper regionMapper, Point centre, int x, int z, Random random, int porientation, boolean ignoreExtraConstraints, CallbackInfoReturnable<BuildingPlan.LocationReturn> cir) {
        BuildingPlan.LocationReturn result = cir.getReturnValue();

        // Only check accepted locations
        if (result.location == null) return;

        BuildingPlan plan = (BuildingPlan)(Object)this;

        int xwidth, zwidth;
        int orientation = porientation == -1 ? BuildingPlan.computeOrientation(new Point(x + winfo.mapStartX, 0.0, z + winfo.mapStartZ), centre) : porientation;
        orientation = (orientation + plan.buildingOrientation) % 4;

        if (orientation == 0 || orientation == 2) {
            xwidth = plan.length + plan.areaToClearLengthBefore + plan.areaToClearLengthAfter + 2;
            zwidth = plan.width + plan.areaToClearWidthBefore + plan.areaToClearWidthAfter + 2;
        } else {
            xwidth = plan.width + plan.areaToClearWidthBefore + plan.areaToClearWidthAfter + 2;
            zwidth = plan.length + plan.areaToClearLengthBefore + plan.areaToClearLengthAfter + 2;
        }

        int margin = ModConfig.terrainMargin;
        int maxAllowedHeightDiff = ModConfig.maxTerrainHeightDiff;
        if (!isMarginTerrainFlatEnough(winfo, x, z, xwidth, zwidth, margin, maxAllowedHeightDiff)) {
            cir.setReturnValue(new BuildingPlan.LocationReturn(9, new Point(x + winfo.mapStartX, 64, z + winfo.mapStartZ)));
        }
    }

    private boolean isMarginTerrainFlatEnough(VillageMapInfo winfo, int startX, int startZ, int width, int length, int margin, int maxDiff) {
        int total = 0;
        int count = 0;

        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < length; dz++) {
                int x = startX + dx;
                int z = startZ + dz;
                if (x < 0 || z < 0 || x >= winfo.topGround.length || z >= winfo.topGround[0].length) continue;
                total += winfo.topGround[x][z];
                count++;
            }
        }

        if (count == 0) return false;

        int avgHeight = total / count;

        for (int dx = -margin; dx < width + margin; dx++) {
            for (int dz = -margin; dz < length + margin; dz++) {
                if (dx >= 0 && dx < width && dz >= 0 && dz < length) continue;

                int x = startX + dx;
                int z = startZ + dz;

                if (x < 0 || z < 0 || x >= winfo.topGround.length || z >= winfo.topGround[0].length) continue;

                int h = winfo.topGround[x][z];
                if (Math.abs(h - avgHeight) > maxDiff) {
                    return false;
                }
            }
        }

        return true;
    }
}
