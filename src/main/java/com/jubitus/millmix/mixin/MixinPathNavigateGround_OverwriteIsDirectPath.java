package com.jubitus.millmix.mixin;

import net.minecraft.entity.EntityLiving;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.millenaire.common.entity.MillVillager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.lang.reflect.Field;

@Mixin(PathNavigateGround.class)
public abstract class MixinPathNavigateGround_OverwriteIsDirectPath {

    @Unique private static boolean mill_printedOnce = false;
    @Unique private static Field mill_entityField = null;
    @Unique private static boolean mill_entityFieldInitTried = false;

    /**
     * Overwrite: disable expensive shortcut checks for MillVillagers.
     * Keep vanilla behavior for everything else.
     */
    @Overwrite
    protected boolean isDirectPathBetweenPoints(Vec3d posVec31, Vec3d posVec32, int sizeX, int sizeY, int sizeZ) {
        if (!mill_printedOnce) {
            mill_printedOnce = true;
            System.out.println("[MillMix] PathNavigateGround#isDirectPathBetweenPoints OVERWRITE ACTIVE");
        }

        EntityLiving ent = mill_getNavigatingEntity();
        if (ent instanceof MillVillager) {
            // Optional periodic proof:
            // if ((ent.world.getTotalWorldTime() % 200) == 0) {
            //     System.out.println("[MillMix] Blocked shortcut checks for MillVillager");
            // }
            return false;
        }

        // === Vanilla method body (your decompiled code) ===
        int i = MathHelper.floor(posVec31.x);
        int j = MathHelper.floor(posVec31.z);
        double d0 = posVec32.x - posVec31.x;
        double d1 = posVec32.z - posVec31.z;
        double d2 = d0 * d0 + d1 * d1;

        if (d2 < 1.0E-8D) {
            return false;
        } else {
            double d3 = 1.0D / Math.sqrt(d2);
            d0 = d0 * d3;
            d1 = d1 * d3;
            sizeX = sizeX + 2;
            sizeZ = sizeZ + 2;

            if (!this.isSafeToStandAt(i, (int) posVec31.y, j, sizeX, sizeY, sizeZ, posVec31, d0, d1)) {
                return false;
            } else {
                sizeX = sizeX - 2;
                sizeZ = sizeZ - 2;
                double d4 = 1.0D / Math.abs(d0);
                double d5 = 1.0D / Math.abs(d1);
                double d6 = (double) i - posVec31.x;
                double d7 = (double) j - posVec31.z;

                if (d0 >= 0.0D) ++d6;
                if (d1 >= 0.0D) ++d7;

                d6 = d6 / d0;
                d7 = d7 / d1;
                int k = d0 < 0.0D ? -1 : 1;
                int l = d1 < 0.0D ? -1 : 1;
                int i1 = MathHelper.floor(posVec32.x);
                int j1 = MathHelper.floor(posVec32.z);
                int k1 = i1 - i;
                int l1 = j1 - j;

                while (k1 * k > 0 || l1 * l > 0) {
                    if (d6 < d7) {
                        d6 += d4;
                        i += k;
                        k1 = i1 - i;
                    } else {
                        d7 += d5;
                        j += l;
                        l1 = j1 - j;
                    }

                    if (!this.isSafeToStandAt(i, (int) posVec31.y, j, sizeX, sizeY, sizeZ, posVec31, d0, d1)) {
                        return false;
                    }
                }

                return true;
            }
        }
    }

    @Shadow
    protected abstract boolean isSafeToStandAt(int x, int y, int z, int sizeX, int sizeY, int sizeZ,
                                               Vec3d vec31, double p_179683_8_, double p_179683_10_);

    @Unique
    private EntityLiving mill_getNavigatingEntity() {
        // Lazy init the reflected field once
        if (!mill_entityFieldInitTried) {
            mill_entityFieldInitTried = true;
            try {
                // Deobf dev name (common in dev)
                mill_entityField = PathNavigate.class.getDeclaredField("entity");
                mill_entityField.setAccessible(true);
                System.out.println("[MillMix] Found PathNavigate.entity via reflection");
            } catch (Throwable ignored) {
                try {
                    // SRG name in 1.12.2 is usually field_75515_a for PathNavigate#entity
                    mill_entityField = PathNavigate.class.getDeclaredField("field_75515_a");
                    mill_entityField.setAccessible(true);
                    System.out.println("[MillMix] Found PathNavigate.field_75515_a (entity) via reflection");
                } catch (Throwable t2) {
                    System.out.println("[MillMix] ERROR: Could not reflect PathNavigate entity field!");
                    t2.printStackTrace();
                    mill_entityField = null;
                }
            }
        }

        if (mill_entityField != null) {
            try {
                return (EntityLiving) mill_entityField.get(this);
            } catch (Throwable t) {
                System.out.println("[MillMix] ERROR: Failed reading PathNavigate entity field");
                t.printStackTrace();
            }
        }
        return null;
    }
}
