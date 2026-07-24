package vict.millmix.mixin.villager;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.village.Building;
import org.millenaire.common.village.VillageMapInfo;
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
 * Y handling needs a trustworthy height reference. Neither posY nor dest.y qualify
 * on their own: a villager that drifted onto a roof has a posY that scans would
 * happily confirm (a rooftop is a "valid" standing position), and dest.y is
 * poisoned whenever the goal destination is another entity (GoalGoChat targets a
 * fellow villager who may himself be ghost-standing on a roof). The primary
 * reference is therefore the village's own ground map, VillageMapInfo.topGround on
 * the town hall: Millénaire maintains it for building placement, and columns
 * covered by a building are forced to the building's ground altitude — never the
 * roof. Where the map is unavailable the code falls back to dest.y + 1 (dest.y is
 * the block Y of the destination).
 *
 * Reference-seeded scans never pick a stand more than 1 block above the reference:
 * climbing is what puts villagers on roofs (e.g. a market stall whose dest column
 * holds the counter with a low roof above — the only "valid" stand in that column
 * is the rooftop). Because such a column may genuinely have no coherent stand, the
 * restore also searches neighbouring columns (nearest first, 5×5), which places a
 * stall keeper beside his counter exactly like real pathfinding would.
 *
 * The restore itself is hooked on checkGoals(), not updatePathIfNeeded():
 * Millénaire stops calling the latter entirely while a villager works with
 * stopMoving=true (the default for working goals, e.g. keepstall), which would
 * leave working stall keepers unrestored forever. checkGoals() runs every tick for
 * any villager with an active goal. The restore happens at ghostViewDistance (far
 * beyond entity tracking range), so the player never sees the snap.
 */
@Mixin(value = MillVillager.class, remap = false)
public abstract class MixinMillVillager_GhostMovement {

    @Unique private static final int MILL_SCAN_DOWN = 8;
    @Unique private static final int MILL_NO_STAND = Integer.MIN_VALUE;

    /** 5×5 column offsets sorted nearest-first: own column, then ring 1, then ring 2. */
    @Unique private static final int[][] MILL_COLUMN_OFFSETS = mill_buildColumnOffsets();

    @Unique private boolean mill_ghostWasActive = false;
    @Unique private long mill_ghostLastLog = 0L;

    @Unique
    private static int[][] mill_buildColumnOffsets() {
        int[][] offsets = new int[25][];
        int i = 0;
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++)
                offsets[i++] = new int[]{dx, dz};
        java.util.Arrays.sort(offsets, java.util.Comparator.comparingInt(o -> o[0] * o[0] + o[1] * o[1]));
        return offsets;
    }

    /**
     * Restore trigger. checkGoals() runs every tick for villagers with an active
     * goal — including working ones with stopMoving=true, for which Millénaire
     * never calls updatePathIfNeeded().
     */
    @Inject(method = "checkGoals", at = @At("HEAD"), remap = false)
    private void mill_ghostRestoreCheck(CallbackInfo ci) {
        if (!MillMixModConfig.ghostMovementEnabled || !mill_ghostWasActive) return;
        MillVillager self = (MillVillager) (Object) this;
        if (self.world.isRemote || !mill_isPlayerNearby(self)) return;
        mill_ghostWasActive = false;
        mill_restoreCoherentPosition(self, self.getPathDestPoint());
    }

    @Inject(method = "updatePathIfNeeded", at = @At("HEAD"), cancellable = true, remap = false)
    private void mill_ghostMovement(Point dest, CallbackInfo ci) {
        if (!MillMixModConfig.ghostMovementEnabled) return;

        MillVillager self = (MillVillager) (Object) this;
        if (self.world.isRemote || dest == null) return;
        if (self.vtype != null && self.vtype.noTeleport) return;
        if (self.stopMoving || self.shouldLieDown) return;
        if (mill_isPlayerNearby(self)) return; // restore is handled by the checkGoals hook

        mill_ghostWasActive = true;

        // Beyond keep_active_radius the village is meant to unload its chunks. Any
        // block read from here would defeat that: in 1.12 getLoadedChunk resets
        // unloadQueued, so per-tick reads cancel the pending unload forever and the
        // village stays active ("Kept alive by:" nobody). Freeze instead — no reads,
        // no movement — and let the chunks unload; the villager freezes with them,
        // exactly like vanilla. The restore snap fixes his position when he returns.
        if (!mill_isVillageKeptActive(self)) {
            self.getNavigator().clearPath();
            self.pathEntity = null;
            ci.cancel();
            return;
        }

        double dx = dest.x - self.posX;
        double dz = dest.z - self.posZ;
        double horizDistSq = dx * dx + dz * dz;

        if (horizDistSq > 1e-4) {
            double horizDist = Math.sqrt(horizDistSq);
            double step = Math.min(MillMixModConfig.ghostWalkSpeedBPS / 20.0, horizDist);
            double ratio = step / horizDist;

            double nx = self.posX + dx * ratio;
            double nz = self.posZ + dz * ratio;

            // Never walk into (or read) an unloaded chunk: getBlockState would
            // force-load it synchronously. Wait at the border instead; Millénaire's
            // real pathfinding refuses unloaded chunks too (ChunkAccessException).
            if (!mill_isColumnLoaded(self.world, nx, nz)) {
                self.getNavigator().clearPath();
                self.pathEntity = null;
                ci.cancel();
                return;
            }
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
            // Arrived horizontally: keep Y pinned to the ground reference so long
            // stationary phases can't preserve a bad Y. If the column has no coherent
            // stand (e.g. the counter of a stall), leave Y alone — invisible while
            // ghost, and the restore's column search fixes it on player approach.
            int standY = mill_findStandY(self.world, self.posX, mill_heightRef(self, self.posX, self.posZ, dest), self.posZ, 1);
            if (standY != MILL_NO_STAND && Math.abs(standY - self.posY) > 0.01) {
                self.setPosition(self.posX, standY, self.posZ);
            }
        }

        self.getNavigator().clearPath();
        self.pathEntity = null;
        ci.cancel();
    }

    /**
     * Y for one walk step. Seeded from the ground reference with the up-cap; retries
     * from posY without it when the seeded scan fails (terrain above the reference:
     * hills, or a column with no stand at ground level). The uncapped retry may pick
     * a rooftop mid-walk — invisible, and preferable to leaving the villager inside
     * a block; the restore fixes it before the player can see anything.
     */
    @Unique
    private double mill_walkY(MillVillager self, double nx, double nz, Point dest) {
        int standY = mill_findStandY(self.world, nx, mill_heightRef(self, nx, nz, dest), nz, 1);
        if (standY == MILL_NO_STAND) {
            standY = mill_findStandY(self.world, nx, self.posY, nz, MILL_SCAN_DOWN);
        }
        return standY == MILL_NO_STAND ? self.posY : standY;
    }

    /**
     * One-time snap when transitioning ghost → normal, before the player can see the
     * villager. Placement preference:
     * 1. Nearest column (5×5, own column first) with a coherent stand at its ground
     *    reference — handles stall keepers whose exact dest column has no stand.
     * 2. Current column at posY on thick ground (legitimate hills outside the
     *    village map; the thickness test rejects roofs and overhangs).
     * 3. Same 5×5 column search around the destination.
     * 4. Leave as-is; Millénaire's stuck handling recovers eventually.
     */
    @Unique
    private void mill_restoreCoherentPosition(MillVillager self, Point dest) {
        if (mill_snapToNearbyColumn(self, self.posX, self.posZ, dest, true, "column snap")) return;

        int standY = mill_findStandY(self.world, self.posX, self.posY, self.posZ, 1);
        if (standY != MILL_NO_STAND && mill_isThickGround(self.world, self.posX, standY, self.posZ)) {
            if (standY != self.posY) {
                mill_logRestore(self, "ground snap", self.posX, standY, self.posZ);
                self.setPosition(self.posX, standY, self.posZ);
            }
            return;
        }

        if (dest != null && mill_snapToNearbyColumn(self, dest.x, dest.z, dest, false, "dest snap")) return;
        // else: no coherent spot found — leave position, stuck handling will recover
    }

    /**
     * Searches the 5×5 columns around (cx, cz) nearest-first for a coherent stand at
     * each column's own ground reference, and moves the villager to the first hit.
     * keepExactPos preserves the villager's exact x/z when the hit is his own column.
     */
    @Unique
    private boolean mill_snapToNearbyColumn(MillVillager self, double cx, double cz, Point dest, boolean keepExactPos, String how) {
        for (int[] off : MILL_COLUMN_OFFSETS) {
            double x = cx + off[0];
            double z = cz + off[1];
            double ref = mill_heightRef(self, x, z, dest);
            if (ref == MILL_NO_STAND) continue;
            int standY = mill_findStandY(self.world, x, ref, z, 1);
            if (standY == MILL_NO_STAND) continue;

            boolean ownColumn = keepExactPos && off[0] == 0 && off[1] == 0;
            double nx = ownColumn ? self.posX : MathHelper.floor(x) + 0.5;
            double nz = ownColumn ? self.posZ : MathHelper.floor(z) + 0.5;
            if (!ownColumn || standY != self.posY) {
                mill_logRestore(self, how, nx, standY, nz);
                self.setPosition(nx, standY, nz);
            }
            return true;
        }
        return false;
    }

    /**
     * Trusted standing-level reference for a column: the village ground map when the
     * column is inside it, otherwise the goal destination's standing level, otherwise
     * MILL_NO_STAND.
     */
    @Unique
    private double mill_heightRef(MillVillager self, double x, double z, Point dest) {
        int ground = mill_villageGroundY(self, x, z);
        if (ground != MILL_NO_STAND) return ground;
        return dest != null ? dest.y + 1.0 : MILL_NO_STAND;
    }

    /**
     * Ground level from the town hall's VillageMapInfo. Authoritative: columns under
     * a building are forced to the building's ground altitude, never its roof.
     * Returns MILL_NO_STAND outside the map or when no village data is available.
     */
    @Unique
    private int mill_villageGroundY(MillVillager self, double x, double z) {
        Building townHall = self.getTownHall();
        if (townHall == null) return MILL_NO_STAND;
        VillageMapInfo winfo = townHall.winfo;
        if (winfo == null || winfo.topGround == null) return MILL_NO_STAND;
        int mx = MathHelper.floor(x) - winfo.mapStartX;
        int mz = MathHelper.floor(z) - winfo.mapStartZ;
        if (mx < 0 || mz < 0 || mx >= winfo.length || mz >= winfo.width) return MILL_NO_STAND;
        int ground = winfo.topGround[mx][mz];
        return ground > 0 ? ground : MILL_NO_STAND;
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
     * Finds the nearest valid standing Y around refY, searching MILL_SCAN_DOWN blocks
     * down and upRange blocks up. Downward is checked before upward at each radius.
     * The up range is 1 for reference-seeded scans (never climb onto a roof) and
     * larger only for posY-seeded walk retries. Returns MILL_NO_STAND if nothing is
     * found (unloaded chunk, void, solid or blocked column).
     */
    @Unique
    private int mill_findStandY(World world, double x, double refY, double z, int upRange) {
        if (refY == MILL_NO_STAND) return MILL_NO_STAND;
        if (!mill_isColumnLoaded(world, x, z)) return MILL_NO_STAND;
        int bx = MathHelper.floor(x);
        int bz = MathHelper.floor(z);
        int baseY = MathHelper.floor(refY);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        if (mill_isValidStand(world, pos, bx, baseY, bz)) return baseY;
        for (int r = 1; r <= MILL_SCAN_DOWN; r++) {
            if (mill_isValidStand(world, pos, bx, baseY - r, bz)) return baseY - r;
            if (r <= upRange && mill_isValidStand(world, pos, bx, baseY + r, bz)) return baseY + r;
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

    /**
     * True when a chunk is already loaded, with no side effects: goes through
     * chunkExists (a map lookup) rather than getLoadedChunk, which would reset the
     * chunk's unloadQueued flag and block unloading.
     */
    @Unique
    private boolean mill_isColumnLoaded(World world, double x, double z) {
        return world.isBlockLoaded(new BlockPos(MathHelper.floor(x), 64, MathHelper.floor(z)), false);
    }

    /**
     * Mirrors Millénaire's own keep-active rule (Building.updateBuildingServer):
     * a village unloads once no player is within KeepActiveRadius + 32 of the town
     * hall. Ghost movement must stop working before that boundary, not fight it.
     */
    @Unique
    private boolean mill_isVillageKeptActive(MillVillager self) {
        Building townHall = self.getTownHall();
        double cx = self.posX, cy = self.posY, cz = self.posZ;
        if (townHall != null && townHall.getPos() != null) {
            Point pos = townHall.getPos();
            cx = pos.x;
            cy = pos.y;
            cz = pos.z;
        }
        double radius = MillConfigValues.KeepActiveRadius + 32.0;
        double radiusSq = radius * radius;
        for (EntityPlayer p : self.world.playerEntities) {
            double dx = p.posX - cx, dy = p.posY - cy, dz = p.posZ - cz;
            if (dx * dx + dy * dy + dz * dz <= radiusSq) return true;
        }
        return false;
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