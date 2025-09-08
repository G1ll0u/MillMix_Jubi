package com.jubitus.millmix.mixin;

import com.jubitus.millmix.MillMixModConfig;
import org.millenaire.common.utilities.MillCommonUtilities;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.village.Building;
import org.millenaire.common.world.MillWorldData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MillWorldData.class)
public class MixinMillWorldData {

    @Shadow(remap = false)
    public MillCommonUtilities.VillageList loneBuildingsList; // list of lone building positions

    /**
     * Original method signature: public Building getClosestVillage(Point point)
     * We'll inject at RETURN to check if a lone building is closer than the village.
     */
    @Inject(method = "getClosestVillage", at = @At("RETURN"), cancellable = true, remap = false)
    private void injectGetClosestVillage(Point point, CallbackInfoReturnable<Building> cir) {
        if (!MillMixModConfig.enableDangerousCustomVillageLogic) {
            // let vanilla / original behavior run
            return;
        }

        Building originalVillage = cir.getReturnValue();
        double originalDistance = originalVillage != null ? originalVillage.getPos().distanceTo(point) : Double.MAX_VALUE;

        Building closestLoneBuilding = null;
        double closestDistance = Double.MAX_VALUE;

        // Iterate over all lone building points
        for (Point p : loneBuildingsList.pos) {
            Building b = this.getBuilding(p);
            if (b != null) {
                double d = b.getPos().distanceTo(point);
                if (d < closestDistance) {
                    closestDistance = d;
                    closestLoneBuilding = b;
                }
            }
        }

        // If a lone building is closer than the original village, return it
        if (closestDistance < originalDistance) {
            cir.setReturnValue(closestLoneBuilding);
        }
    }

    /**
     * Shadow the existing getBuilding(Point) method in MillWorldData
     */
    @Shadow(remap = false)
    public Building getBuilding(Point p) {
        throw new IllegalStateException("Shadow method");
    }
}