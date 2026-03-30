package vict.millmix.mixin;

import org.millenaire.common.pathing.atomicstryker.AStarConfig;
import org.millenaire.common.pathing.atomicstryker.AStarNode;
import org.millenaire.common.pathing.atomicstryker.AStarPathPlannerJPS;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vict.millmix.MillMixModConfig;

import java.util.ArrayList;

@Mixin(value = AStarPathPlannerJPS.class, remap = false)
public abstract class MixinAStarPathPlannerJPS {

    @Shadow
    public AStarConfig config;
    @Shadow
    private AStarNode lastStart;
    @Shadow
    private AStarNode lastEnd;
    @Unique
    private boolean millenaire_swimFallbackTried = false;

    @Inject(method = "getPath", at = @At("HEAD"), remap = false)
    private void millenaire_swim_resetFallback(AStarNode start, AStarNode end, AStarConfig cfg, CallbackInfo ci) {
        this.millenaire_swimFallbackTried = false;
    }

    @Inject(method = "onFoundPath(Ljava/util/ArrayList;)V", at = @At("HEAD"), remap = false)
    private void millenaire_swim_resetFallbackOnSuccess(ArrayList<AStarNode> result, CallbackInfo ci) {
        this.millenaire_swimFallbackTried = false;
    }

    @Inject(method = "onNoPathAvailable()V", at = @At("HEAD"), cancellable = true, remap = false)
    private void millenaire_swim_trySwimFallbackFirst(CallbackInfo ci) {
        if (!MillMixModConfig.enableVillagerSwimming) return;
        if (this.config == null) return;

        // If the first attempt was "no swimming", retry once with swimming enabled.
        if (!this.millenaire_swimFallbackTried && !this.config.canSwim) {
            this.millenaire_swimFallbackTried = true;

            AStarConfig swimCfg = copyWithSwimmingEnabled(this.config);

            // Re-run search with canSwim=true. Cancel original "no path" handling for now.
            ((AStarPathPlannerJPS) (Object) this).getPath(this.lastStart, this.lastEnd, swimCfg);
            ci.cancel();
        }
    }

    @Unique
    private static AStarConfig copyWithSwimmingEnabled(AStarConfig c) {
        if (c.tolerance) {
            return new AStarConfig(
                    c.canUseDoors,
                    c.canTakeDiagonals,
                    c.allowDropping,
                    true,               // canSwim
                    c.canClearLeaves,
                    c.toleranceHorizontal,
                    c.toleranceVertical
            );
        }
        return new AStarConfig(
                c.canUseDoors,
                c.canTakeDiagonals,
                c.allowDropping,
                true,               // canSwim
                c.canClearLeaves
        );
    }
}
