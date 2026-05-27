package vict.millmix.mixin.building;

import org.millenaire.common.utilities.Point;
import org.millenaire.common.village.Building;
import org.millenaire.common.world.MillWorldData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Building.class)
public class MixinBuilding_RegisterDebug {

    @Shadow(remap = false)
    public MillWorldData mw;

    @Inject(method = "registerBuildingEntity", at = @At("HEAD"), remap = false)
    private void dbgRegister(Building buildingEntity, CallbackInfo ci) {
        Point p = buildingEntity.getPos();
        Building existing = mw.getBuilding(p);
        if (existing != null && existing != buildingEntity) {
            System.out.println("[MILLMIX] registerBuildingEntity pos=" + p
                    + " new=" + buildingEntity
                    + " existing=" + existing
                    + " new.loc=" + (buildingEntity.location != null ? buildingEntity.location : "null")
                    + " existing.loc=" + (existing.location != null ? existing.location : "null"));
        }
    }
}