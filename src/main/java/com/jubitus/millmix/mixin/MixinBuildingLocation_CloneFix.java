package com.jubitus.millmix.mixin;

import org.millenaire.common.village.BuildingLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BuildingLocation.class)
public class MixinBuildingLocation_CloneFix {
    @Inject(method="clone", at=@At("RETURN"), remap=false)
    private void fixClone(CallbackInfoReturnable<BuildingLocation> cir) {
        BuildingLocation bl = cir.getReturnValue();
        if (bl == null) return;

        if (bl.isSubBuildingLocation) {
            if (bl.pos != null && (bl.chestPos == null || !bl.chestPos.equals(bl.pos))) {
                System.out.println("[MILLMIX] clone(): forcing subbuilding chestPos to pos. old="
                        + bl.chestPos + " new=" + bl.pos + " planKey=" + bl.planKey);
                bl.chestPos = bl.pos;
            }
        }
    }
}