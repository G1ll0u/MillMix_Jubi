package com.jubitus.millmix.mixin;

import net.minecraft.entity.Entity;
import org.millenaire.common.goal.Goal;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.village.Building;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

//class that helps me
@Mixin(Goal.class) // The class I want to modify
public abstract class MixinGoal {

    @Shadow(remap = false)
    protected abstract Goal.GoalInformation packDest(Point p, Building b);

    @Shadow(remap = false)
    protected abstract Goal.GoalInformation packDest(Point p, Building b, Entity ent);

    @Shadow(remap = false)
    protected abstract Goal.GoalInformation packDest(Point p);
}