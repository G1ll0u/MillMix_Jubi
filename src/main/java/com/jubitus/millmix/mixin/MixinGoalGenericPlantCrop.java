package com.jubitus.millmix.mixin;

import org.millenaire.common.goal.generic.GoalGenericPlantCrop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(GoalGenericPlantCrop.class)
public abstract class MixinGoalGenericPlantCrop extends MixinGoalGeneric {

    /**
     * idea: change millvillager's toggleDoor to use BlockDoor's toggleDoor
     * @author Jubitus
     * @reason Make villagers wait for crops to get ripe
     */
    @Overwrite(remap = false)
    public void applyDefaultSettings() {
        this.duration = 10;
        this.lookAtGoal = true;
        this.tags.add("tag_agriculture");
    }

}
