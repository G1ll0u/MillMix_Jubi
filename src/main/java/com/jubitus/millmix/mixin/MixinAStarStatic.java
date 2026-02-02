package com.jubitus.millmix.mixin;

import com.jubitus.millmix.MillMixModConfig;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import org.millenaire.common.pathing.atomicstryker.AStarConfig;
import org.millenaire.common.pathing.atomicstryker.AStarNode;
import org.millenaire.common.pathing.atomicstryker.AStarStatic;
import org.millenaire.common.utilities.ThreadSafeUtilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AStarStatic.class, remap = false)
public abstract class MixinAStarStatic {

    @Inject(
            method = "isPassableBlock(Lnet/minecraft/world/World;IIILorg/millenaire/common/pathing/atomicstryker/AStarConfig;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void millenaire_swim_waterPassable(
            World world, int x, int y, int z, AStarConfig config,
            CallbackInfoReturnable<Boolean> cir
    ) throws ThreadSafeUtilities.ChunkAccessException {
        if (!MillMixModConfig.enableVillagerSwimming) return;
        if (config != null && config.canSwim) {
            Block b = ThreadSafeUtilities.getBlock(world, x, y, z);
            if (b == Blocks.WATER || b == Blocks.FLOWING_WATER) {
                cir.setReturnValue(true);
            }
        }
    }

    // ✅ Inject into the *AStarNode overload* (the one Mixin is complaining about)
    @Inject(
            method = "isViable(Lnet/minecraft/world/World;Lorg/millenaire/common/pathing/atomicstryker/AStarNode;ILorg/millenaire/common/pathing/atomicstryker/AStarConfig;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void millenaire_swim_waterViable_node(
            World world, AStarNode target, int yoffset, AStarConfig config,
            CallbackInfoReturnable<Boolean> cir
    ) throws ThreadSafeUtilities.ChunkAccessException {

        if (config == null || !config.canSwim || target == null) return;

        int x = target.x, y = target.y, z = target.z;
        Block here = ThreadSafeUtilities.getBlock(world, x, y, z);

        if (here == Blocks.WATER || here == Blocks.FLOWING_WATER) {
            boolean headOk = AStarStatic.isPassableBlock(world, x, y + 1, z, config);
            cir.setReturnValue(headOk);
        }
    }

    // (Optional) also inject into the int,int,int overload so BOTH are consistent
    @Inject(
            method = "isViable(Lnet/minecraft/world/World;IIIILorg/millenaire/common/pathing/atomicstryker/AStarConfig;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void millenaire_swim_waterViable_xyz(
            World world, int x, int y, int z, int yoffset, AStarConfig config,
            CallbackInfoReturnable<Boolean> cir
    ) throws ThreadSafeUtilities.ChunkAccessException {

        if (config == null || !config.canSwim) return;

        Block here = ThreadSafeUtilities.getBlock(world, x, y, z);
        if (here == Blocks.WATER || here == Blocks.FLOWING_WATER) {
            boolean headOk = AStarStatic.isPassableBlock(world, x, y + 1, z, config);
            cir.setReturnValue(headOk);
        }
    }
}
