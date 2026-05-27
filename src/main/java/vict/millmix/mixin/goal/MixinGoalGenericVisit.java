package vict.millmix.mixin.goal;

import org.millenaire.common.annotedparameters.AnnotedParameter;
import org.millenaire.common.annotedparameters.ConfigAnnotations;
import org.millenaire.common.entity.MillVillager;
import org.millenaire.common.goal.Goal;
import org.millenaire.common.goal.generic.GoalGenericVisit;
import org.millenaire.common.utilities.MillCommonUtilities;
import org.millenaire.common.utilities.MillLog;
import org.millenaire.common.utilities.Point;
import org.millenaire.common.village.Building;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;


@Mixin(GoalGenericVisit.class)
public abstract class MixinGoalGenericVisit extends MixinGoalGeneric {
    @ConfigAnnotations.ConfigField(type = AnnotedParameter.ParameterType.INTEGER, defaultValue = "2")
    @ConfigAnnotations.FieldDocumentation(explanation = "Random offset from the destination point")
    public int randomOffset = 2;

    @Shadow(remap = false)
    public List<String> targetVillagerGoals;
    @Shadow(remap = false)
    public AnnotedParameter.PosType targetPosition;

    /**
     * idea: Add some offset to the destination point to make villager cluster less for visit goal, yes it was probably my first code, that's why I used @Overwrite
     *
     * @author Jubitus
     * @reason egg
     */

    @Overwrite(remap = false)
    public Goal.GoalInformation getDestination(MillVillager villager) throws Exception {
        if (this.targetVillagerGoals != null) {
            ArrayList<MillVillager> targets = new ArrayList<MillVillager>();
            for (MillVillager v : villager.getTownHall().getKnownVillagers()) {
                if (v == villager || v.goalKey == null || !this.isValidGoalForTargetting(v.goalKey)) continue;
                targets.add(v);
            }
            if (targets.isEmpty()) {
                return null;
            }
            return this.packDest(null, null, targets.get(MillCommonUtilities.randomInt(targets.size())));
        }
        List<Building> buildings = this.getBuildings(villager);
        for (Building dest : buildings) {
            if (!this.isDestPossible(villager, dest)) continue;
            Point basePos = this.targetPosition.getPosition(dest);
            int range = this.randomOffset * 2 + 1;
            int offsetX = basePos.getiX() + (MillCommonUtilities.randomInt(range) - this.randomOffset);
            int offsetZ = basePos.getiZ() + (MillCommonUtilities.randomInt(range) - this.randomOffset);
            Point offsetPos = new Point(offsetX, basePos.getiY(), offsetZ);
            return this.packDest(offsetPos, dest);
        }
        return null;
    }

    private boolean isValidGoalForTargetting(String goalKey) {
        if (this.targetVillagerGoals == null) {
            return false;
        }

        Goal goal = Goal.goals.get(goalKey);
        if (goal == null) {
            MillLog.error(this, "Villager had unknown goal: " + goalKey);
            return false;
        }

        for (String target : this.targetVillagerGoals) {
            if (goal.tags.contains(target)) {
                return true;
            }
        }

        return false;
    }
}