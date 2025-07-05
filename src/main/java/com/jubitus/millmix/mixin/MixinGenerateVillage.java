package com.jubitus.millmix.mixin;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.millenaire.common.buildingplan.BuildingPlan;
import org.millenaire.common.buildingplan.BuildingPlanSet;
import org.millenaire.common.buildingplan.TreeClearer;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.culture.VillageType;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.item.ItemParchment;
import org.millenaire.common.network.ServerSender;
import org.millenaire.common.pathing.atomicstryker.RegionMapper;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.utilities.WorldUtilities;
import org.millenaire.common.village.Building;
import org.millenaire.common.village.BuildingLocation;
import org.millenaire.common.village.BuildingProject;
import org.millenaire.common.village.VillageMapInfo;
import org.millenaire.common.world.MillWorldData;
import org.millenaire.common.world.VillageWallGenerator;
import org.millenaire.common.world.WorldGenVillage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

@Mixin(WorldGenVillage.class)
public class MixinGenerateVillage {
    @Inject(
            method = "generateVillage(Lorg/millenaire/common/utilities/Point;Lnet/minecraft/world/World;Lorg/millenaire/common/culture/VillageType;Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/entity/player/EntityPlayer;Ljava/util/Random;ILjava/lang/String;Lorg/millenaire/common/utilities/Point;FZZ)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/millenaire/common/buildingplan/BuildingPlan;findBuildingLocation(Lorg/millenaire/common/village/VillageMapInfo;Lorg/millenaire/common/pathing/atomicstryker/RegionMapper;Lorg/millenaire/common/utilities/Point;ILjava/util/Random;I)Lorg/millenaire/common/village/BuildingLocation;",
                    shift = At.Shift.BEFORE,
                    ordinal = 0
            ),
            cancellable = true,
            remap = false
    )

    void onBeforeFindBuildingLocation(
            Point targetPos,
            World world,
            VillageType villageType,
            EntityPlayer player,
            EntityPlayer closestPlayer,
            Random random,
            int minDistance,
            String name,
            Point parentVillage,
            float completionRatio,
            boolean testBiomeValidity,
            boolean alwaysSpawn,
            CallbackInfoReturnable<Boolean> cir
    ) {
        smoothTerrainAroundVillage(world, targetPos, villageType.radius);
    }
         void smoothTerrainAroundVillage(World world, Point center, int radius) {
            int margin = 3; // extra padding around village radius
            int startX = center.getiX() - radius - margin;
            int endX = center.getiX() + radius + margin;
            int startZ = center.getiZ() - radius - margin;
            int endZ = center.getiZ() + radius + margin;

            // Store original heights in a grid
            int[][] heights = new int[endX - startX + 1][endZ - startZ + 1];
            for (int x = startX; x <= endX; x++) {
                for (int z = startZ; z <= endZ; z++) {
                    heights[x - startX][z - startZ] = WorldUtilities.findTopSoilBlock(world, x, z);
                }
            }

            // Calculate smooth heights using a simple 3x3 averaging kernel
            int kernelRadius = 1;
            int[][] smoothHeights = new int[heights.length][heights[0].length];
            for (int x = kernelRadius; x < heights.length - kernelRadius; x++) {
                for (int z = kernelRadius; z < heights[0].length - kernelRadius; z++) {
                    int sum = 0, count = 0;
                    for (int dx = -kernelRadius; dx <= kernelRadius; dx++) {
                        for (int dz = -kernelRadius; dz <= kernelRadius; dz++) {
                            sum += heights[x + dx][z + dz];
                            count++;
                        }
                    }
                    smoothHeights[x][z] = sum / count;
                }
            }

            // Apply smoothing: raise or lower terrain to match smoothed heights
            for (int x = kernelRadius; x < heights.length - kernelRadius; x++) {
                for (int z = kernelRadius; z < heights[0].length - kernelRadius; z++) {
                    int worldX = x + startX;
                    int worldZ = z + startZ;
                    int currentHeight = heights[x][z];
                    int targetHeight = smoothHeights[x][z];

                    if (targetHeight > currentHeight) {
                        for (int y = currentHeight + 1; y <= targetHeight; y++) {
                            BlockPos pos = new BlockPos(worldX, y, worldZ);
                            world.setBlockState(pos, Blocks.DIRT.getDefaultState());
                        }
                    } else if (targetHeight < currentHeight) {
                        for (int y = currentHeight; y > targetHeight; y--) {
                            BlockPos pos = new BlockPos(worldX, y, worldZ);
                            world.setBlockToAir(pos);
                        }
                    }
                }
            }
        }

    }


