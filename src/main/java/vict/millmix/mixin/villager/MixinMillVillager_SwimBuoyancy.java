package vict.millmix.mixin.villager;

import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.pathing.atomicstryker.AS_PathEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vict.millmix.MillMixModConfig;

@Mixin(value = MillVillager.class, remap = false)
public abstract class MixinMillVillager_SwimBuoyancy {

    @Shadow
    public AS_PathEntity pathEntity;
    @Shadow
    public boolean stopMoving;

    @Inject(method = "func_70071_h_", at = @At("TAIL"), remap = false)
    private void millenaire_swim_keepAfloat(CallbackInfo ci) {
        if (!MillMixModConfig.enableVillagerSwimming) return;
        MillVillager self = (MillVillager) (Object) this;

        if (self.isInWater()) { // in water
            if (this.pathEntity != null && this.pathEntity.getCurrentPathLength() > 0 && !this.stopMoving) {
                // keep trying to rise a bit so they don't sink/stall
                self.setJumping(true); // setJumping(true) in SRG-ish names
                if (self.motionY < 0.02D) {
                    self.motionY = 0.02D;
                }
            }
        }
    }
}
