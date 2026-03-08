package com.jubitus.millmix.mixin;

import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Blocks;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.pathfinding.WalkNodeProcessor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(WalkNodeProcessor.class)
public abstract class MixinWalkNodeProcessor_CacheNodeTypeRaw_Overwrite {

    @Shadow protected EntityLiving currentEntity;

    @Unique private final Long2ByteOpenHashMap mill_cache = new Long2ByteOpenHashMap(4096);
    @Unique private long mill_tick = Long.MIN_VALUE;

    @Unique private static boolean mill_printedOnce = false;

    /**
     * Beginner-proof approach: Overwrite the method, add cache and prints.
     * This also proves beyond doubt the mixin is applied if you see the log line.
     */
    @Overwrite
    protected PathNodeType getPathNodeTypeRaw(IBlockAccess access, int x, int y, int z) {
        // Print ONCE so you can confirm it's actually running
        if (!mill_printedOnce) {
            mill_printedOnce = true;
            System.out.println("[MillMix] WalkNodeProcessor#getPathNodeTypeRaw OVERWRITE ACTIVE");
        }

        // Tick-based cache reset (only if it's a World)
        if (access instanceof World) {
            long t = ((World) access).getTotalWorldTime();
            if (t != mill_tick) {
                mill_tick = t;
                mill_cache.clear();
            }

            long key = packPos(x, y, z);
            byte cached = mill_cache.get(key);
            if (cached != 0) {
                int ord = (cached & 0xFF) - 1;
                PathNodeType[] values = PathNodeType.values();
                if (ord >= 0 && ord < values.length) {
                    return values[ord];
                }
            }

            PathNodeType computed = computeRaw(access, x, y, z);
            int store = computed.ordinal() + 1;
            if (store >= 1 && store <= 255) {
                mill_cache.put(key, (byte) store);
            }
            return computed;
        }

        // Fallback if not a World (rare)
        return computeRaw(access, x, y, z);
    }

    @Unique
    private PathNodeType computeRaw(IBlockAccess access, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        IBlockState state = access.getBlockState(pos);
        Block block = state.getBlock();
        Material material = state.getMaterial();

        // Vanilla hook (important for mod blocks)
        PathNodeType type = block.getAiPathNodeType(state, access, pos, this.currentEntity);
        if (type != null) return type;

        if (material == Material.AIR) {
            return PathNodeType.OPEN;
        } else if (block != Blocks.TRAPDOOR && block != Blocks.IRON_TRAPDOOR && block != Blocks.WATERLILY) {
            if (block == Blocks.FIRE) {
                return PathNodeType.DAMAGE_FIRE;
            } else if (block == Blocks.CACTUS) {
                return PathNodeType.DAMAGE_CACTUS;
            } else if (block instanceof BlockDoor && material == Material.WOOD && !state.getValue(BlockDoor.OPEN)) {
                return PathNodeType.DOOR_WOOD_CLOSED;
            } else if (block instanceof BlockDoor && material == Material.IRON && !state.getValue(BlockDoor.OPEN)) {
                return PathNodeType.DOOR_IRON_CLOSED;
            } else if (block instanceof BlockDoor && state.getValue(BlockDoor.OPEN)) {
                return PathNodeType.DOOR_OPEN;
            } else if (block instanceof BlockRailBase) {
                return PathNodeType.RAIL;
            } else if (!(block instanceof BlockFence) && !(block instanceof BlockWall)
                    && (!(block instanceof BlockFenceGate) || state.getValue(BlockFenceGate.OPEN))) {
                if (material == Material.WATER) {
                    return PathNodeType.WATER;
                } else if (material == Material.LAVA) {
                    return PathNodeType.LAVA;
                } else {
                    return block.isPassable(access, pos) ? PathNodeType.OPEN : PathNodeType.BLOCKED;
                }
            } else {
                return PathNodeType.FENCE;
            }
        } else {
            return PathNodeType.TRAPDOOR;
        }
    }

    @Unique
    private static long packPos(int x, int y, int z) {
        return ((long)(x & 0x3FFFFFF) << 38) | ((long)(z & 0x3FFFFFF) << 12) | (long)(y & 0xFFF);
    }
}
