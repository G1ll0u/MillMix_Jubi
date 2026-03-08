package com.jubitus.millmix.mixin.accessor;

import net.minecraft.entity.EntityLiving;
import net.minecraft.pathfinding.PathNavigate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Access private/protected fields in PathNavigate safely.
 */
@Mixin(PathNavigate.class)
public interface AccessorPathNavigate {
    @Accessor("entity")
    EntityLiving mill_getEntity();
}
