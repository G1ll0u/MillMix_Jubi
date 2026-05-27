package vict.millmix.mixin.building;

import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.village.Building;
import org.millenaire.common.world.MillWorldData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Building.class)
public abstract class MixinBuilding {
    @Shadow(remap = false)
    public MillWorldData mw;

    /**
     * Bug fix: when addAdult can't find a slot (type == null), the teenager stays in the village
     * and retries every night action, causing infinite log spam and a perpetual retry loop.
     * Removing the teenager from the village stops the loop. Worker conversion (males only) is
     * handled upstream by Travellers' HEAD injection, which cancels before this point is reached.
     *
     * @author Jubitus
     * @reason Fix stuck teenager infinite retry loop
     */
    @Inject(
            method = "addAdult",
            at = @At(
                    value = "INVOKE",
                    target = "org/millenaire/common/utilities/MillLog.error(Ljava/lang/Object;Ljava/lang/String;)V",
                    ordinal = 0,
                    shift = At.Shift.BEFORE
            ),
            cancellable = true,
            remap = false
    )
    private void handleTeenagerWithNoSlot(MillVillager child, CallbackInfo ci) {
        // No adult slot available for this teenager. Remove them from the village so they
        // don't retry every night forever. Travellers' mixin handles worker conversion for
        // male teenagers and cancels before this point, so we only reach here for females
        // or when Travellers is absent.
        try {
            this.mw.removeVillagerRecord(child.getVillagerId());
            child.despawnVillagerSilent();
        } catch (Exception e) {
            MillLog.error(this, "[MillMix] Failed to remove stuck teenager " + child + ": " + e);
        }
        ci.cancel();
    }
}
