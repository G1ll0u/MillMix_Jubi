package vict.millmix.mixin.building;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.millenaire.common.entity.TileEntityLockedChest;
import org.millenaire.common.utilities.Point;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import vict.millmix.MillMixModConfig;

/**
 * ResManager.readFromNBT() ends with a loop over every registered chest point that
 * re-stamps TileEntityLockedChest.buildingPos, presumably to repair desyncs. Each
 * iteration calls Point.getMillChest(world), i.e. World.getTileEntity(): if the
 * chunk is only generated on disk but not currently loaded in memory, this forces
 * a synchronous chunk load (World.isChunkGeneratedAt() only checks the region file,
 * it doesn't require the chunk to be in memory - so the guard before getMillChest()
 * does nothing to prevent this).
 *
 * On worlds with hundreds of villages this runs during FMLServerAboutToStartEvent,
 * for every building of every village, regardless of whether a player is anywhere
 * near it - which is what causes the multi-minute "loads everything" startup hitch,
 * and leaves hundreds of chunks resident in memory before the world has even
 * finished loading.
 *
 * This repair is redundant in the common case: buildingPos is already stamped
 * directly on chest placement (Building.placeChest and friends) and is persisted
 * in the chest's own NBT (TileEntityLockedChest#readFromNBT/writeToNBT), so a chunk
 * that loads normally already carries the correct value. This mixin keeps the
 * repair for chunks that happen to already be loaded (e.g. near a player at world
 * load, or a village close to spawn) but skips it - rather than force-loading -
 * for everything else. The desync case this exists for (e.g. a chest manually
 * replaced while its chunk was unloaded) will simply get repaired the next time
 * that chunk loads and the village updates, instead of at world-load time.
 */
@Mixin(targets = "org.millenaire.common.village.buildingmanagers.ResManager", remap = false)
public class MixinResManager_LazyChestBuildingPos {

    @Redirect(
        method = "readFromNBT",
        at = @At(
            value = "INVOKE",
            target = "Lorg/millenaire/common/utilities/Point;getMillChest(Lnet/minecraft/world/World;)Lorg/millenaire/common/entity/TileEntityLockedChest;"
        ),
        remap = false
    )
    private TileEntityLockedChest millmix_getMillChestWithoutForceLoading(Point point, World world) {
        if (!MillMixModConfig.lazyChestBuildingPosRepair) {
            return point.getMillChest(world);
        }
        BlockPos pos = new BlockPos(point.getiX(), point.getiY(), point.getiZ());
        if (!world.isBlockLoaded(pos, false)) {
            return null;
        }
        return point.getMillChest(world);
    }
}
