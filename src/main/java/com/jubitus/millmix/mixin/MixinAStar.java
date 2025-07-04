package com.jubitus.millmix.mixin;

import net.minecraft.pathfinding.PathPoint;
import net.minecraft.world.World;
import org.millenaire.common.pathing.atomicstryker.*;
import org.millenaire.common.utilities.ThreadSafeUtilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.ArrayList;
import java.util.List;

@Mixin(AStarStatic.class)
public class MixinAStar {
    /**
     * idea: change millvillager's toggleDoor to use BlockDoor's toggleDoor
     * @author Vict
     * @reason toggleDoor doesn't make use of BlockDoor.toggleDoor(),
     * causing issues when other mods override the behaviour of doors.
     * This way we don't manually set block states, but instead tell
     * the blocks to change their states.
     */
    @Overwrite(remap = false)
    public static AS_PathEntity translateAStarPathtoPathEntity(World world, List<AStarNode> input, AStarConfig config) throws ThreadSafeUtilities.ChunkAccessException {
        if (!config.canTakeDiagonals) {
            List<AStarNode> oldInput = input;
            input = new ArrayList<>();
            for (int i = 0; i < oldInput.size() - 1; ++i) {
                AStarNode newNode;
                input.add(oldInput.get(i));

                boolean sameX = oldInput.get(i).x == oldInput.get(i + 1).x;
                boolean sameZ = oldInput.get(i).z == oldInput.get(i + 1).z;
                boolean diffY = oldInput.get(i).y != oldInput.get(i + 1).y;

                if (sameX || sameZ || diffY) continue;

                if (!AStarStatic.isPassableBlock(world, oldInput.get(i).x, oldInput.get(i).y - 1, oldInput.get(i + 1).z, config)
                        && AStarStatic.isPassableBlock(world, oldInput.get(i).x, oldInput.get(i).y, oldInput.get(i + 1).z, config)
                        && AStarStatic.isPassableBlock(world, oldInput.get(i).x, oldInput.get(i).y + 1, oldInput.get(i + 1).z, config)) {

                    newNode = new AStarNode(oldInput.get(i).x, oldInput.get(i).y, oldInput.get(i + 1).z, 0, null);
                } else {
                    newNode = new AStarNode(oldInput.get(i + 1).x, oldInput.get(i).y, oldInput.get(i).z, 0, null);
                }

                input.add(newNode);
            }
        }

        PathPoint[] points = new PathPoint[input.size()]; // FIX: Now the array is truly PathPoint[]
        int i = 0;
        int size = input.size();

        while (size > 0) {
            AStarNode reading = input.get(size - 1);

            AS_PathPoint asp = new AS_PathPoint(reading.x, reading.y, reading.z);
            asp.setIndex(i);
            asp.setTotalPathDistance(i);
            asp.setDistanceToNext(1.0f);
            asp.setDistanceToTarget(size);

            if (i > 0) {
                asp.setPrevious(points[i - 1]); // still safe because points[i-1] is an AS_PathPoint
            }

            points[i] = asp;
            input.remove(size - 1);
            --size;
            ++i;
        }

        return new AS_PathEntity(points);
    }

}