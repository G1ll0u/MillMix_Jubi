package vict.millmix.mixin.villager;

import org.millenaire.common.forge.CommonProxy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.millenaire.common.entity.MillVillager;

/**
 * Suppresses repeated "Error in onUpdate()" chat spam: two catch blocks in
 * func_70071_h_ call sendChatAdmin every tick while the villager is broken.
 * We let the first call through and swallow all subsequent ones.
 */
@Mixin(value = MillVillager.class, remap = false)
public abstract class MixinMillVillager_OnUpdateSpam {

    @Unique
    private boolean mill_onUpdateErrorReported = false;

    @Redirect(
        method = "func_70071_h_",
        at = @At(value = "INVOKE",
            target = "org/millenaire/common/forge/CommonProxy.sendChatAdmin(Ljava/lang/String;)V"),
        allow = 2,
        remap = false
    )
    private void mill_suppressOnUpdateSpam(CommonProxy proxy, String message) {
        if (!mill_onUpdateErrorReported) {
            mill_onUpdateErrorReported = true;
            proxy.sendChatAdmin(message);
        }
    }
}
