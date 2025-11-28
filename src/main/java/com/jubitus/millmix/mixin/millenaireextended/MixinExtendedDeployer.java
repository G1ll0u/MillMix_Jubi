package com.jubitus.millmix.mixin.millenaireextended;

import com.jubitus.millmix.MillMixModConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


import java.io.File;

/**
 * One mixin that targets BOTH:
 *
 *  - com.laikaivanova.millenaireextended.common.deployer.ContentDeployer
 *  - com.laikaiwanowa.millenaireextended.common.deployer.ContentDeployer
 *
 * (base Millénaire Extended + all addons that use the second package)
 */
@Mixin(
        targets = {
                "com.laikaivanova.millenaireextended.common.ContentDeployer",
                "com.laikaiwanowa.millenaireextended.common.deployer.ContentDeployer"
        },
        remap = false
)
public abstract class MixinExtendedDeployer {

    /**
     * Intercept the static deployContent(File) method at HEAD.
     * If our config switch is true, cancel the method so it does nothing.
     */
    @Inject(method = "deployContent", at = @At("HEAD"), cancellable = true)
    private static void millmix$disableDeploy(File ourJar, CallbackInfo ci) {
        if (MillMixModConfig.disableAllMillenaireExtendedDeployment) {
            // We can’t safely reference their LOGGER without importing them,
            // but we can still log via System.out or your own logger:
            System.out.println("[MillMix] Skipping Millénaire Extended content deployment (disabled in config).");
            ci.cancel();
        }
    }
}

