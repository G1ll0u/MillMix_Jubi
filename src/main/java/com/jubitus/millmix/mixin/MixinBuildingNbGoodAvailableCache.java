package com.jubitus.millmix.mixin;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import org.millenaire.common.item.InvItem;
import org.millenaire.common.village.Building;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.item.Item;

@Mixin(value = Building.class, remap = false)
public class MixinBuildingNbGoodAvailableCache {

    /**
     * Cache is valid only for a single world tick.
     */
    @Unique private long mill$nbAvailCacheTick = Long.MIN_VALUE;

    /**
     * Key: packed (itemId, meta, flags)
     * Value: nbGoodAvailable result
     */
    @Unique private final Long2IntOpenHashMap mill$nbAvailCache = new Long2IntOpenHashMap();

    @Unique
    private static long mill$packKey(InvItem ii, boolean forConstruction, boolean forExport, boolean forShop) {
        // Item.getIdFromItem is stable in 1.12
        final int itemId = Item.getIdFromItem(ii.getItem());
        final int meta = ii.meta; // can be -1 for wildcard in some systems; keep full 16 bits

        final int flags =
                (forConstruction ? 1 : 0) |
                (forExport ? 2 : 0) |
                (forShop ? 4 : 0);

        // Pack into long:
        // [ itemId (20 bits+) | meta (16 bits) | flags (3 bits) ]
        // itemId can be > 2^20 in theory, but in 1.12 it’s fine; we’ll store full 32 anyway.
        return ((long) itemId << 32) ^ ((long) (meta & 0xFFFF) << 3) ^ (long) flags;
    }

    @Inject(method = "nbGoodAvailable(Lorg/millenaire/common/item/InvItem;ZZZ)I", at = @At("HEAD"), cancellable = true)
    private void mill$nbGoodAvailableCacheHead(InvItem ii, boolean forConstruction, boolean forExport, boolean forShop,
                                               CallbackInfoReturnable<Integer> cir) {
        long tick = ((Building) (Object) this).world.getWorldTime();

        if (mill$nbAvailCacheTick != tick) {
            mill$nbAvailCacheTick = tick;
            mill$nbAvailCache.clear();
            mill$nbAvailCache.defaultReturnValue(Integer.MIN_VALUE);
        }

        long key = mill$packKey(ii, forConstruction, forExport, forShop);
        int cached = mill$nbAvailCache.get(key);
        if (cached != Integer.MIN_VALUE) {
            cir.setReturnValue(cached);
        }
    }

    @Inject(method = "nbGoodAvailable(Lorg/millenaire/common/item/InvItem;ZZZ)I", at = @At("RETURN"))
    private void mill$nbGoodAvailableCacheReturn(InvItem ii, boolean forConstruction, boolean forExport, boolean forShop,
                                                 CallbackInfoReturnable<Integer> cir) {
        long tick = ((Building) (Object) this).world.getWorldTime();

        if (mill$nbAvailCacheTick != tick) {
            mill$nbAvailCacheTick = tick;
            mill$nbAvailCache.clear();
            mill$nbAvailCache.defaultReturnValue(Integer.MIN_VALUE);
        }

        long key = mill$packKey(ii, forConstruction, forExport, forShop);
        mill$nbAvailCache.put(key, cir.getReturnValue().intValue());
    }

}
