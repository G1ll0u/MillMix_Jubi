package vict.millmix.mixin.world;

import com.google.common.collect.ImmutableSetMultimap;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.ForgeChunkManager;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.forge.BuildingChunkLoader;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.village.Building;
import org.millenaire.common.village.VillageMapInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vict.millmix.MillMixModConfig;

/**
 * Millénaire's unloadChunks() only releases its ForgeChunkManager tickets, which
 * makes the village chunks *eligible* for unload but queues nothing. In 1.12.2 the
 * only sweeps that queue non-player-watched chunks are PlayerChunkMap (player view
 * only) and WorldServer.saveAllChunks — so with an infrequent autosave, villages
 * beyond keep_active_radius stay loaded (and active, since isActive follows
 * isVillageChunksLoaded()) until the next world save.
 *
 * This mixin queues the village's chunk area for unload right when Millénaire
 * releases its tickets, skipping chunks still watched by a player or force-loaded
 * by another ticket (overlapping village, other mods). Unloading then happens on
 * the next chunk-provider tick instead of the next autosave.
 */
@Mixin(value = BuildingChunkLoader.class, remap = false)
public abstract class MixinBuildingChunkLoader_PromptUnload {

    @Shadow Building townHall;

    @Inject(method = "unloadChunks", at = @At("TAIL"), remap = false)
    private void millmix_queueVillageChunksForUnload(CallbackInfo ci) {
        if (!MillMixModConfig.promptVillageChunkUnload) return;
        Building th = this.townHall;
        if (th == null || th.winfo == null || !(th.world instanceof WorldServer)) return;

        WorldServer world = (WorldServer) th.world;
        ChunkProviderServer provider = world.getChunkProvider();
        VillageMapInfo winfo = th.winfo;
        ImmutableSetMultimap<ChunkPos, ForgeChunkManager.Ticket> forced = world.getPersistentChunks();

        int queued = 0;
        // Same chunk range loadChunks() forced.
        for (int cx = winfo.chunkStartX - 1; cx < winfo.chunkStartX + winfo.length / 16 + 1; cx++) {
            for (int cz = winfo.chunkStartZ - 1; cz < winfo.chunkStartZ + winfo.width / 16 + 1; cz++) {
                if (world.getPlayerChunkMap().contains(cx, cz)) continue;
                if (forced.containsKey(new ChunkPos(cx, cz))) continue;
                Chunk chunk = provider.getLoadedChunk(cx, cz);
                if (chunk != null) {
                    provider.queueUnload(chunk);
                    queued++;
                }
            }
        }
        if (queued > 0 && MillConfigValues.LogChunkLoader >= 1) {
            MillLog.major(th, "MillMix: queued " + queued + " village chunks for unload.");
        }
    }
}
