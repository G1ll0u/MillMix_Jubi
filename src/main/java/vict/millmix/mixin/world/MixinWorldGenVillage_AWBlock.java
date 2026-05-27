package vict.millmix.mixin.world;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLLog;
import org.millenaire.common.culture.VillageType;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.world.WorldGenVillage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vict.millmix.MillMixModConfig;
import vict.millmix.aw.AWOverlap;

import java.util.Random;

@Mixin(value = WorldGenVillage.class, remap = false)
public abstract class MixinWorldGenVillage_AWBlock {

    @Inject(
            method = "generateVillage(Lorg/millenaire/common/utilities/Point;Lnet/minecraft/world/World;Lorg/millenaire/common/culture/VillageType;Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/entity/player/EntityPlayer;Ljava/util/Random;ILjava/lang/String;Lorg/millenaire/common/utilities/Point;FZZ)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void millmix_blockVillageIfAWNearby(
            Point targetPos, World world, VillageType villageType, EntityPlayer player, EntityPlayer closestPlayer, Random random, int minDistance, String name, Point parentVillage, float completionRatio, boolean testBiomeValidity, boolean alwaysSpawn, CallbackInfoReturnable<Boolean> cir
    ) {

        if (world == null || world.isRemote) return;
        if (villageType == null) return;

        int radius = villageType.radius;

        int minX = targetPos.getiX() - radius;
        int maxX = targetPos.getiX() + radius;
        int minZ = targetPos.getiZ() - radius;
        int maxZ = targetPos.getiZ() + radius;

        // if ANY AW structure BB intersects the village area -> cancel entire generation attempt
        if (AWOverlap.intersectsAny(world, minX, 0, minZ, maxX, 255, maxZ)) {
            if (MillMixModConfig.debugAWOverlapLogs) {
                FMLLog.log.info("[MillMix] CANCEL village generation (AW overlap) type={} radius={} centreXZ=({}, {}) areaX=[{},{}] Z=[{},{}]",
                        villageType.key, radius, targetPos.getiX(), targetPos.getiZ(), minX, maxX, minZ, maxZ);
            }
            cir.setReturnValue(false);
        }
    }
}
