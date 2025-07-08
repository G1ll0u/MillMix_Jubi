package com.jubitus.millmix.mixin;

import org.millenaire.common.config.MillConfigValues;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.utilities.MillCommonUtilities;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.village.Building;
import org.millenaire.common.village.BuildingLocation;
import org.millenaire.common.village.VillagerRecord;
import org.millenaire.common.world.MillWorldData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Mixin(Building.class)
public abstract class MixinBuilding {
    @Shadow(remap = false)
    public abstract Map<Long, VillagerRecord> getVillagerRecords();

    @Shadow(remap = false)
    public MillWorldData mw;

    @Shadow
    public abstract List<Building> getBuildings();

    @Shadow(remap = false)
    public BuildingLocation location;

    /**
     * idea: change millvillager's toggleDoor to use BlockDoor's toggleDoor
     *
     * @author Jubitus
     * @reason Make villagers wait for crops to get ripe
     */
    @Inject(
            method = "addAdult",
            at = @At(
                    value = "INVOKE",
                    target = "org/millenaire/common/utilities/MillLog.error(Ljava/lang/Object;Ljava/lang/String;)V",
                    ordinal = 0,
                    shift = At.Shift.BEFORE
            ),
            cancellable = true,
            remap = false
    )
    private void replaceErrorLog2(MillVillager child, CallbackInfo ci) {
        // Your condition:
        if (MillConfigValues.LogBuildingPlan >= 1) {
            MillLog.error(this, "Villager types: " + (child.gender == 1 ? MillCommonUtilities.flattenStrings(this.location.getMaleResidents()) : MillCommonUtilities.flattenStrings(this.location.getFemaleResidents())));
            MillLog.error(this, "Villager types: " + (child.gender == 1 ? MillCommonUtilities.flattenStrings(this.location.getMaleResidents()) : MillCommonUtilities.flattenStrings(this.location.getFemaleResidents())));
            String s = "";
            for (VillagerRecord vr : this.getVillagerRecords().values()) {
                s = s + vr.type + " ";
            }

            MillLog.error(this, "Current residents: " + s);
        } else {
            // Optionally, do something else or nothing
        }
        // Cancel original else block execution to avoid the original error logs
        ci.cancel();
    }

    @Mixin(Building.class)
    public interface BuildingAccessor {
        @Accessor("villagers")
        Set<MillVillager> getVillagers();
    }
}

