package com.jubitus.millmix.mixin;

import com.jubitus.millmix.MillMixModConfig;
import org.millenaire.common.buildingplan.BuildingPlan;
import org.millenaire.common.pathing.atomicstryker.RegionMapper;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.village.BuildingLocation;
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
            zwidth = plan.width  + plan.areaToClearWidthBefore  + plan.areaToClearWidthAfter  + 2;
        } else {
            xwidth = plan.width  + plan.areaToClearWidthBefore  + plan.areaToClearWidthAfter  + 2;
            zwidth = plan.length + plan.areaToClearLengthBefore + plan.areaToClearLengthAfter + 2;
        }

        // --- Slope check (only ONE scan; you had 2 scans before) ---
        int minHeight = Integer.MAX_VALUE;
        int maxHeight = Integer.MIN_VALUE;

        // Choose one: FULL footprint scan is simplest
        for (int dx = -xwidth / 2; dx <= xwidth / 2; dx++) {
            for (int dz = -zwidth / 2; dz <= zwidth / 2; dz++) {
                int i = x + dx;
                int j = z + dz;

                if (i < 0 || i >= winfo.length || j < 0 || j >= winfo.width) continue;

                int h = winfo.topGround[i][j];
                if (h < minHeight) minHeight = h;
                if (h > maxHeight) maxHeight = h;
            }
        }

        int maxSlope = MillMixModConfig.maxTerrainHeightDiff;
        if (maxHeight - minHeight > maxSlope) {
            cir.setReturnValue(new BuildingPlan.LocationReturn(9, loc.pos));
            // no need to return, nothing below
        }
    }
}

