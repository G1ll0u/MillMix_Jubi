package vict.millmix.mixin;

import org.millenaire.common.utilities.Point;
import org.millenaire.common.village.Building;
import org.millenaire.common.village.BuildingLocation;
import org.millenaire.common.village.ConstructionIP;
import org.millenaire.common.world.MillWorldData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Building.class)
public class MixinBuilding_InitialiseConstructionFix {

    @Shadow(remap = false)
    public MillWorldData mw;

    @Inject(
            method = "initialiseConstruction(Lorg/millenaire/common/village/ConstructionIP;Lorg/millenaire/common/utilities/Point;)V",
            at = @At("HEAD"),
            remap = false,
            require = 1
    )
    private void rekeyOnCollision(ConstructionIP cip, Point refPos, CallbackInfo ci) {
        BuildingLocation bl = cip.getBuildingLocation();
        if (bl == null || bl.chestPos == null || bl.pos == null) return;

        Building existing = this.mw.getBuilding(bl.chestPos);
        if (existing == null) return;

        // Only act when collision is real (different building already uses that key)
        // and rekey is possible
        if (!bl.chestPos.equals(bl.pos)) {
            System.out.println("[MILLMIX] rekey chestPos to avoid overwrite: planKey=" + bl.planKey
                    + " old=" + bl.chestPos + " new=" + bl.pos + " existing=" + existing);
            bl.chestPos = bl.pos;
        }
    }
}