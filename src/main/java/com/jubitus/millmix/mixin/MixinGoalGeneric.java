package com.jubitus.millmix.mixin;

import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.generic.GoalGeneric;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.village.Building;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
//class that helps me
@Mixin(GoalGeneric.class) // The class I want to modify
public abstract class MixinGoalGeneric extends MixinGoal {
    @Shadow(remap = false)
    public abstract List<Building> getBuildings(MillVillager villager);

    @Shadow(remap = false)
    public abstract boolean isDestPossible(MillVillager villager, Building dest) throws MillLog.MillenaireException;
}
