package com.jubitus.millmix.mixin;

import com.jubitus.millmix.MillMixModConfig;
import org.millenaire.common.buildingplan.BuildingPlan;
import org.millenaire.common.pathing.atomicstryker.RegionMapper;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.village.BuildingLocation;
import org.millenaire.common.village.VillageMapInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

@Mixin(BuildingPlan.class)
public class MixinBuildingPlan {
    /**
     * idea: Prevent buildings placement near mountains (by simply checking terrain height diff in building footprint)
     *
     * @author Jubitus
     * @reason Buildings will sometimes spawn and break landscape because of terrain height difference in building footprint
     */
    @Inject(
            method = "testSpot(Lorg/millenaire/common/village/VillageMapInfo;Lorg/millenaire/common/pathing/atomicstryker/RegionMapper;Lorg/millenaire/common/utilities/Point;IILjava/util/Random;IZ)Lorg/millenaire/common/buildingplan/BuildingPlan$LocationReturn;",
            at = @At("RETURN"),
            cancellable = true,
            remap = false
    )
    private void preventMountainEdgePlacement(
            VillageMapInfo winfo,
            RegionMapper regionMapper,
            Point centre,
            int x,
            int z,
            Random random,
            int porientation,
            boolean ignoreExtraConstraints,
            CallbackInfoReturnable<BuildingPlan.LocationReturn> cir
    ) {
        BuildingPlan.LocationReturn returnValue = cir.getReturnValue();

        if (returnValue == null || returnValue.location == null) {
            return; // No building location found, no need to check
        }

        BuildingLocation loc = returnValue.location;
        BuildingPlan plan = loc.getPlan();

        int orientation = porientation == -1 ? BuildingPlan.computeOrientation(new Point(x + winfo.mapStartX, 0.0, z + winfo.mapStartZ), centre) : porientation;
        orientation = (orientation + plan.buildingOrientation) % 4;

        int xwidth, zwidth;

        if (orientation == 0 || orientation == 2) {
            xwidth = plan.length + plan.areaToClearLengthBefore + plan.areaToClearLengthAfter + 2;
            zwidth = plan.width + plan.areaToClearWidthBefore + plan.areaToClearWidthAfter + 2;
        } else {
            xwidth = plan.width + plan.areaToClearWidthBefore + plan.areaToClearWidthAfter + 2;
            zwidth = plan.length + plan.areaToClearLengthBefore + plan.areaToClearLengthAfter + 2;
        }

        int margin = 2;
        int minDx = -xwidth / 2 + margin;
        int maxDx = xwidth / 2 - margin;
        int minDz = -zwidth / 2 + margin;
        int maxDz = zwidth / 2 - margin;

        int minHeight = Integer.MAX_VALUE;
        int maxHeight = Integer.MIN_VALUE;

        for (int dx = minDx; dx <= maxDx; dx++) {
            for (int dz = minDz; dz <= maxDz; dz++) {
                int i = x + dx;
                int j = z + dz;

                if (i < 0 || i >= winfo.length || j < 0 || j >= winfo.width) continue;

                int h = winfo.topGround[i][j];
                minHeight = Math.min(minHeight, h);
                maxHeight = Math.max(maxHeight, h);
            }
        }

        // Loop over the building area to find min and max terrain height
        for (int dx = -xwidth / 2; dx <= xwidth / 2; dx++) {
            for (int dz = -zwidth / 2; dz <= zwidth / 2; dz++) {
                int i = x + dx;
                int j = z + dz;

                if (i < 0 || i >= winfo.length || j < 0 || j >= winfo.width) continue;

                int h = winfo.topGround[i][j];
                minHeight = Math.min(minHeight, h);
                maxHeight = Math.max(maxHeight, h);
            }
        }

        int maxSlope = MillMixModConfig.maxTerrainHeightDiff; // Maximum allowed height difference
        if (maxHeight - minHeight > maxSlope) {
            // Terrain too uneven, reject this location
            cir.setReturnValue(new BuildingPlan.LocationReturn(9, loc.pos)); // 9 = custom error code for slope fail
        }
    }
}


// shit code :
//    @Inject(method = "build", at = @At("RETURN"), remap = false)
//    private void afterBuild(MillWorldData mw, VillageType villageType, BuildingLocation location,
//                            boolean villageGeneration, boolean isBuildingTownHall, Building townHall, boolean wandimport,
//                            boolean includeSpecialPoints, EntityPlayer owner, boolean rushBuilding, CallbackInfoReturnable<List<BuildingPlan.LocationBuildingPair>> cir) {
//
//        World world = mw.world;
//        int seed = (int) (location.pos.x * 31 + location.pos.z * 17);  // deterministic seed based on position
//
//        terrainBlend(world, location, seed);
//    }
//
//    private void terrainBlend(World world, BuildingLocation location, int seed) {
//        Random rand = new Random(seed);
//
//        Point center = location.pos;
//        int blendRadius = 12;  // radius *outside* building bounds to blend
//        // Assuming you have a bounding box for building (replace with actual from location)
//        // Here we just mock it as a square of size buildingSize around center:
//        int buildingSizeX = location.width; // replace with actual width
//        int buildingSizeZ = location.length; // replace with actual depth
//
//        // Calculate building bounds
//        int minX = center.getiX();
//        int minZ = center.getiZ();
//        int maxX = minX + buildingSizeX - 1;
//        int maxZ = minZ + buildingSizeZ - 1;
//
//        // We'll blend only outside these bounds but within blendRadius
//        for (int x = minX - blendRadius; x <= maxX + blendRadius; x++) {
//            for (int z = minZ - blendRadius; z <= maxZ + blendRadius; z++) {
//
//                // Skip positions inside building footprint
//                if (x >= minX && x <= maxX && z >= minZ && z <= maxZ) {
//                    continue;
//                }
//
//                // Distance to closest building edge (X or Z)
//                int dx = 0;
//                if (x < minX) dx = minX - x;
//                else if (x > maxX) dx = x - maxX;
//
//                int dz = 0;
//                if (z < minZ) dz = minZ - z;
//                else if (z > maxZ) dz = z - maxZ;
//
//                double dist = Math.sqrt(dx * dx + dz * dz);
//
//                if (dist > blendRadius) continue;
//
//                // Fade factor 0 (at building edge) -> 1 (at max radius)
//                double fade = dist / blendRadius;
//
//                // Sample biome at this position
//                int buildingMinY = location.pos.getiY();
//                int buildingMaxY = location.maxy;  // Capital Y here
//                int groundY = findTopSoilBlock(world, x, z);
//
//                if (groundY >= buildingMinY && groundY <= buildingMaxY) {
//                    continue; // skip blending inside building vertical space
//                }
//
//                Biome biome = world.getBiome(new BlockPos(x, groundY, z));
//
/// / Don't blend inside building vertical range
//                if (groundY >= buildingMinY && groundY <= buildingMaxY) {
//                    continue;
//                }
//
//
//                // Noise - smaller amplitude for low fade (close to building), bigger for farther
//                double noise = (rand.nextDouble() - 0.5) * 2.0; // [-1,1]
//
//                double biomeFactor = 1.0;
//                if (biome == Biomes.DESERT) biomeFactor = 0.1;
//                else if (biome == Biomes.PLAINS) biomeFactor = 0.5;
//                else if (biome == Biomes.FOREST) biomeFactor = 1.0;
//                else if (biome == Biomes.SWAMPLAND) biomeFactor = 0.7;
//
//                // Adjust height with noise scaled by biome and fade - less influence close to building
//                int offsetY = (int) (noise * biomeFactor * fade);
//
//                int targetY = groundY + offsetY;
//
//                BlockPos pos = new BlockPos(x, targetY, z);
//                IBlockState currentBlock = world.getBlockState(pos);
//
//                // Only place block if air or ground (to avoid messing with other structures)
//                if (currentBlock.getBlock().isAir(currentBlock, world, pos) ||
//                        currentBlock.getMaterial() == Material.GROUND) {
//
//                    // Blend block: e.g. grass or dirt based on biome
//                    IBlockState blendBlock = Blocks.GRASS.getDefaultState();
//                    if (biome == Biomes.DESERT) blendBlock = Blocks.SAND.getDefaultState();
//                    else if (biome == Biomes.SWAMPLAND) blendBlock = Blocks.DIRT.getDefaultState();
//
//                    world.setBlockState(pos, blendBlock, 2);
