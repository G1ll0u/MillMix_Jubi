package vict.millmix.mixin.villager;

import net.minecraft.entity.player.EntityPlayer;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.utilities.WorldUtilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vict.millmix.MillMixModConfig;

/**
 * Ghost movement: villagers outside view distance skip pathfinding entirely and
 * teleport to their goal destination after the time it would have taken them to walk there.
 */
@Mixin(value = MillVillager.class, remap = false)
public abstract class MixinMillVillager_GhostMovement {

    @Unique private long mill_ghostArrivalTick = 0L;
    @Unique private double mill_ghostTX, mill_ghostTZ;
    @Unique private long mill_ghostLastLog = 0L;

    @Inject(method = "updatePathIfNeeded", at = @At("HEAD"), cancellable = true, remap = false)
    private void mill_ghostMovement(Point dest, CallbackInfo ci) {
        if (!MillMixModConfig.ghostMovementEnabled) return;

        MillVillager self = (MillVillager) (Object) this;
        if (self.world.isRemote) return;
        if (self.vtype != null && self.vtype.noTeleport) return;

        if (mill_isPlayerNearby(self)) {
            if (mill_ghostArrivalTick > 0) {
                if (MillMixModConfig.ghostMovementDebug)
                    System.out.println("[GhostMove] " + self.firstName + " " + self.familyName + " — player nearby, resuming pathfinding");
                mill_ghostArrivalTick = 0L;
            }
            return;
        }

        long now = self.world.getTotalWorldTime();

        double ddx = dest.x - mill_ghostTX, ddz = dest.z - mill_ghostTZ;
        boolean destChanged = mill_ghostArrivalTick > 0 && (ddx * ddx + ddz * ddz > 25.0);

        if (mill_ghostArrivalTick <= 0 || destChanged) {
            mill_ghostTX = dest.x;
            mill_ghostTZ = dest.z;
            double dx = self.posX - dest.x, dy = self.posY - dest.y, dz = self.posZ - dest.z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            long ticks = (long) (dist * 20.0 / Math.max(0.01, MillMixModConfig.ghostWalkSpeedBPS));
            mill_ghostArrivalTick = now + Math.max(1L, ticks);

            if (MillMixModConfig.ghostMovementDebug) {
                String reason = destChanged ? "dest changed" : "start";
                System.out.println("[GhostMove] " + self.firstName + " " + self.familyName
                        + " (" + reason + ") dist=" + String.format("%.1f", dist)
                        + " eta=" + ticks + "t → " + dest);
            }
        }

        if (now >= mill_ghostArrivalTick) {
            Point goalDest = self.getGoalDestPoint();
            Point target = (goalDest != null) ? goalDest : dest;
            Point safe = WorldUtilities.findVerticalStandingPos(self.world, target);

            double tx, ty, tz;
            if (safe != null) {
                tx = safe.x + 0.5; ty = safe.y; tz = safe.z + 0.5;
            } else {
                tx = target.x + 0.5; ty = target.y; tz = target.z + 0.5;
            }

            if (MillMixModConfig.ghostMovementDebug)
                System.out.println("[GhostMove] " + self.firstName + " " + self.familyName
                        + " TELEPORT from (" + String.format("%.0f,%.0f,%.0f", self.posX, self.posY, self.posZ)
                        + ") to (" + String.format("%.0f,%.0f,%.0f", tx, ty, tz) + ")");

            self.setPosition(tx, ty, tz);
            self.longDistanceStuck = 0;
            mill_ghostArrivalTick = 0L;
        } else if (MillMixModConfig.ghostMovementDebug && now - mill_ghostLastLog > 100) {
            mill_ghostLastLog = now;
            System.out.println("[GhostMove] " + self.firstName + " " + self.familyName
                    + " waiting " + (mill_ghostArrivalTick - now) + "t  dest=" + dest);
        }

        self.getNavigator().clearPath();
        self.pathEntity = null;
        ci.cancel();
    }

    @Unique
    private boolean mill_isPlayerNearby(MillVillager self) {
        double maxDistSq = (double) MillMixModConfig.ghostViewDistance * MillMixModConfig.ghostViewDistance;
        for (EntityPlayer p : self.world.playerEntities) {
            if (self.getDistanceSq(p) <= maxDistSq) return true;
        }
        return false;
    }
}
