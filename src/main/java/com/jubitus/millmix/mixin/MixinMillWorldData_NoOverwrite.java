package com.jubitus.millmix.mixin;

import org.millenaire.common.utilities.Point;
import org.millenaire.common.village.Building;
import org.millenaire.common.world.MillWorldData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;

@Mixin(MillWorldData.class)
public class MixinMillWorldData_NoOverwrite {
    @Shadow(remap = false)
    private HashMap<Point, Building> buildings;

    @Inject(method = "addBuilding", at = @At("HEAD"), cancellable = true, remap = false)
    private void noOverwrite(Building b, Point p, CallbackInfo ci) {
        Building existing = buildings.get(p);
        if (existing != null && existing != b) {
            System.out.println("[MILLMIX] BLOCKED overwrite at " + p
                    + " existing=" + existing + " new=" + b);
            ci.cancel();
        }
    }
}