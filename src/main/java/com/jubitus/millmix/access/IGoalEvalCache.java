package com.jubitus.millmix.access;


import org.millenaire.common.goal.Goal;

public interface IGoalEvalCache {

    GoalEvalCacheEntry mill$getGoalEvalCacheEntry(Goal goal);

    final class GoalEvalCacheEntry {
        public long possibleTick = Long.MIN_VALUE;
        public boolean possibleValue;

        public long stillValidTick = Long.MIN_VALUE;
        public boolean stillValidValue;
    }
}
