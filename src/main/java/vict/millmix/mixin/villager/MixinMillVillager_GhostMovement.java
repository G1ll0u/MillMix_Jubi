package vict.millmix.mixin.villager;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
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
 * Ghost movement: villagers outside view distance skip A* pathfinding and walk
 * linearly toward their destination at walking speed. Goal completion still works
 * because MillVillager.checkGoals() detects arrival by horizontal distance to
 * pathDestPoint, not by path state.
 *
 * Y handling is built around one invariant: the goal destination's standing level
 * (dest.y + 1, since dest.y is the block Y of the destination) is the only height
 * reference that is coherent by definition. posY is never trusted as a reference
 * on its own, because a villager that drifted onto a roof has a posY that is a
 * perfectly "valid" standing position — scans seeded from it would confirm the
 * roof instead of correcting it.
 *
 * While walking, Y follows the terrain via a nearest-first vertical scan seeded
 * from min(posY, dest.y + 1). Capping at the destination level prevents
 * incremental rooftop-climbing when crossing a building; if the capped scan finds
 * nothing (terrain more than SCAN_RANGE above the destination, e.g. a hill), it
 * retries from posY so the villager follows the hill surface instead of being
 * embedded inside it.
 *
 * Once arrived horizontally (e.g. stall keepers standing still for long periods),
 * Y is kept pinned to the destination standing level every tick.
 *
 * On player approach, a one-time restore places the villager at a position the
 * player can never question: the current column at goal-coherent height if it is
 * at/below that height or sits on thick ground (roofs and overhangs have air
 * pockets a few blocks under the surface and are rejected), otherwise the goal
 * destination itself via Millénaire's own findVerticalStandingPos — the same
 * placement jumpToDest() uses for stuck villagers. The restore happens at
 * ghostViewDistance (far beyond entity tracking range), so the player never sees
 * the snap.
 */
@Mixin(value = MillVillager.class, remap = false)
public abstract class MixinMillVillager_GhostMovement {

    @Unique private static final int MILL_SCAN_RANGE = 8;
    @Unique private static final int MILL_NO_STAND = Integer.MIN_VALUE;

    @Unique private boolean mill_ghostWasActive = false;
    @Unique private long mill_ghostLastLog = 0L;

    @Inject(method = "updatePathIfNeeded", at = @At("HEAD"), cancellable = true, remap = false)
    private void mill_ghostMovement(Point dest, CallbackInfo ci) {
        if (!MillMixModConfig.ghostMovementEnabled) return;

        MillVillager self = (MillVillager) (Object) this;
        if (self.world.isRemote || dest == null) return;
        if (self.vtype != null && self.vtype.noTeleport) return;
        if (self.stopMoving || self.shouldLieDown) return;

        if (mill_isPlayerNearby(self)) {
            if (mill_ghostWasActive) {
                mill_ghostWasActive = false;
                mill_restoreCoherentPosition(self, dest);
            }
            return;
        }

        mill_ghostWasActive = true;

        double dx = dest.x - self.posX;
        double dz = dest.z - self.posZ;
        double horizDistSq = dx * dx + dz * dz;

        if (horizDistSq > 1e-4) {
            double horizDist = Math.sqrt(horizDistSq);
            double step = Math.min(MillMixModConfig.ghostWalkSpeedBPS / 20.0, horizDist);
            double ratio = step / horizDist;

            double nx = self.posX + dx * ratio;
            double nz = self.posZ + dz * ratio;
            double ny = mill_walkY(self, nx, nz, dest);

            if (MillMixModConfig.ghostMovementDebug) {
                long now = self.world.getTotalWorldTime();
                if (now - mill_ghostLastLog > 200) {
                    mill_ghostLastLog = now;
                    System.out.println("[GhostMove] " + self.firstName + " " + self.familyName
                            + " dist=" + String.format("%.1f", horizDist)
                            + " → (" + String.format("%.0f,%.0f,%.0f", dest.x, dest.y, dest.z) + ")");
                }
            }

            self.setPosition(nx, ny, nz);
            self.longDistanceStuck = 0;
        } else {
            // Arrived horizontally (stall keepers, idle stands): keep Y pinned to the
            // goal standing level so long stationary phases can't preserve a bad Y.
            int standY = mill_findStandY(self.world, self.posX, dest.y + 1.0, self.posZ);
            if (standY != MILL_NO_STAND && Math.abs(standY - self.posY) > 0.01) {
                self.setPosition(self.posX, standY, self.posZ);
            }
        }

        self.getNavigator().clearPath();
        self.pathEntity = null;
        ci.cancel();
    }

    /**
     * Y for one walk step. Seeded from min(posY, dest.y+1) to prevent rooftop-climbing;
     * falls back to posY when the capped scan fails (terrain well above the destination),
     * and keeps posY when nothing is found at all (unloaded chunk, void).
     */
    @Unique
    private double mill_walkY(MillVillager self, double nx, double nz, Point dest) {
        double refY = Math.min(self.posY, dest.y + 1.0);
        int standY = mill_findStandY(self.world, nx, refY, nz);
        if (standY == MILL_NO_STAND && refY < self.posY) {
            standY = mill_findStandY(self.world, nx, self.posY, nz);
        }
        return standY == MILL_NO_STAND ? self.posY : standY;
    }

    /**
     * One-time snap when transitioning ghost → normal, before the player can see the
     * villager. Placement preference:
     * 1. Current column at goal-coherent height — accepted if at/below dest.y+2
     *    (can't be a roof: roofs are above the goal's standing level) or standing on
     *    thick ground (rejects roofs/overhangs met while crossing elevated terrain).
     * 2. The goal destination itself, placed like Millénaire's jumpToDest().
     * 3. Leave as-is; Millénaire's stuck handling recovers eventually.
     */
    @Unique
    private void mill_restoreCoherentPosition(MillVillager self, Point dest) {
        int standY = mill_findStandY(self.world, self.posX, dest.y + 1.0, self.posZ);
        if (standY == MILL_NO_STAND) {
            standY = mill_findStandY(self.world, self.posX, self.posY, self.posZ);
        }
        if (standY != MILL_NO_STAND
                && (standY <= dest.y + 2.0 || mill_isThickGround(self.world, self.posX, standY, self.posZ))) {
            if (standY != self.posY) {
                mill_logRestore(self, "column snap", self.posX, standY, self.posZ);
                self.setPosition(self.posX, standY, self.posZ);
            }
            return;
        }

        Point jumpTo = WorldUtilities.findVerticalStandingPos(self.world, dest);
        if (jumpTo != null && Math.abs(jumpTo.y - (dest.y + 1.0)) < 4.0) {
            mill_logRestore(self, "dest snap", jumpTo.x + 0.5, jumpTo.y, jumpTo.z + 0.5);
            self.setPosition(jumpTo.x + 0.5, jumpTo.y, jumpTo.z + 0.5);
        }
        // else: no coherent spot found — leave position, stuck handling will recover
    }

    @Unique
    private void mill_logRestore(MillVillager self, String how, double x, double y, double z) {
        if (MillMixModConfig.ghostMovementDebug) {
            System.out.println("[GhostMove] " + self.firstName + " " + self.familyName
                    + " restore (" + how + ") " + String.format("%.1f/%.1f/%.1f", self.posX, self.posY, self.posZ)
                    + " → " + String.format("%.1f/%.1f/%.1f", x, y, z));
        }
    }

    /**
     * Finds the nearest valid standing Y around refY, searching ±MILL_SCAN_RANGE.
     * Downward is checked before upward at each radius: this preferentially finds
     * ground below rather than a rooftop above. Returns MILL_NO_STAND if nothing
     * is found (unloaded chunk, void, solid terrain).
     */
    @Unique
    private int mill_findStandY(World world, double x, double refY, double z) {
        int bx = MathHelper.floor(x);
        int bz = MathHelper.floor(z);
        int baseY = MathHelper.floor(refY);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        if (mill_isValidStand(world, pos, bx, baseY, bz)) return baseY;
        for (int r = 1; r <= MILL_SCAN_RANGE; r++) {
            if (mill_isValidStand(world, pos, bx, baseY - r, bz)) return baseY - r;
            if (mill_isValidStand(world, pos, bx, baseY + r, bz)) return baseY + r;
        }
        return MILL_NO_STAND;
    }

    @Unique
    private boolean mill_isValidStand(World world, BlockPos.MutableBlockPos pos, int bx, int standY, int bz) {
        if (standY < 1 || standY > 254) return false;
        pos.setPos(bx, standY - 1, bz);
        if (!world.getBlockState(pos).isSideSolid(world, pos, EnumFacing.UP)) return false;
        pos.setPos(bx, standY, bz);
        if (world.getBlockState(pos).getMaterial().blocksMovement()) return false;
        pos.setPos(bx, standY + 1, bz);
        return !world.getBlockState(pos).getMaterial().blocksMovement();
    }

    /**
     * True when the support under standY is real ground rather than a thin structure:
     * roofs, bridges and overhangs have passable blocks (interior air, water) within a
     * few blocks below their surface, while natural ground is solid all the way down.
     */
    @Unique
    private boolean mill_isThickGround(World world, double x, int standY, double z) {
        int bx = MathHelper.floor(x);
        int bz = MathHelper.floor(z);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 2; i <= 4; i++) {
            int y = standY - i;
            if (y < 1) return true;
            pos.setPos(bx, y, bz);
            if (!world.getBlockState(pos).getMaterial().blocksMovement()) return false;
        }
        return true;
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