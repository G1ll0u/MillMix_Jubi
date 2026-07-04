package vict.millmix.mixin.loading;

import org.millenaire.common.item.InvItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Replaces InvItem.CACHE (plain HashMap) with a ConcurrentHashMap so that
 * createInvItem() calls from multiple threads during parallel plan loading
 * cannot corrupt the map. The race itself is benign: two threads creating the
 * same item produce identical, immutable InvItem instances, so the last-write
 * wins without any semantic difference.
 */
@Mixin(InvItem.class)
public class MixinInvItem_ConcurrentCache {

    @Shadow
    private static Map<Integer, InvItem> CACHE;

    @Inject(method = "<clinit>", at = @At("TAIL"), remap = false)
    private static void replaceCacheWithConcurrentHashMap(CallbackInfo ci) {
        CACHE = new ConcurrentHashMap<>(CACHE);
    }
}
