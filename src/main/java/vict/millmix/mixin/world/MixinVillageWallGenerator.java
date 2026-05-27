package vict.millmix.mixin.world;

import net.minecraft.world.World;
import org.millenaire.common.buildingplan.BuildingPlan;
import org.millenaire.common.buildingplan.BuildingPlanSet;
import org.millenaire.common.culture.VillageType;
import org.millenaire.common.culture.WallType;
import org.millenaire.common.pathing.atomicstryker.RegionMapper;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.utilities.WorldUtilities;
import org.millenaire.common.village.VillageMapInfo;
import org.millenaire.common.world.VillageWallGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vict.millmix.MillMixModConfig;

import java.util.ArrayList;
import java.util.List;

@Mixin(VillageWallGenerator.class)
public class MixinVillageWallGenerator {

    @Shadow(remap = false)
    World world;

    /**
     * idea: Prevent wall building placement near mountains (by simply checking terrain height diff in building footprint)
     *
     * @author Jubitus
     * @reason Walls will sometimes spawn and break landscape because of terrain height difference in building footprint
     */
    @Inject(method = "computeWallElementLocation", at = @At("HEAD"), cancellable = true, remap = false)
    private void checkTerrainFlatness(RegionMapper regionMapper, Point centre, VillageType villageType,
                                      WallType wallType, VillageMapInfo winfo, BuildingPlanSet planSet,
                                      Point pos, int orientation, boolean spawn,
                                      CallbackInfoReturnable<Object> cir) {
        if (MillMixModConfig.disableAllWalls) {
            MillLog.temp(null, "Wall generation skipped globally via config at " + pos);
            cir.setReturnValue(null);
            return;
        }
        BuildingPlan plan = planSet.getFirstStartingPlan();
        int orientatedLength = plan.length;
        int orientatedWidth = plan.width;

        if (orientation % 2 == 1) {
            // Rotate dimensions if orientation is odd
            orientatedLength = plan.width;
            orientatedWidth = plan.length;
        }

        double halfLength = orientatedLength / 2.0;
        double halfWidth = orientatedWidth / 2.0;

        List<Point> points = new ArrayList<>();
        points.add(pos);
        points.add(pos.getRelative(halfLength, 0.0, halfWidth));
        points.add(pos.getRelative(halfLength, 0.0, -halfWidth));
        points.add(pos.getRelative(-halfLength, 0.0, halfWidth));
        points.add(pos.getRelative(-halfLength, 0.0, -halfWidth));

        int maxY = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;

        for (Point check : points) {
            int surfaceY = WorldUtilities.findSurfaceBlock(world, check.getiX(), check.getiZ());
            maxY = Math.max(maxY, surfaceY);
            minY = Math.min(minY, surfaceY);
        }

        int deltaY = maxY - minY;
        int allowedDelta = MillMixModConfig.maxWallTerrainHeightDiff;

        if (deltaY > allowedDelta) {
            MillLog.temp(null, "Terrain too uneven for wall at " + pos + ", ΔY = " + deltaY + ". Skipping.");
            cir.setReturnValue(null);
        }
    }

}