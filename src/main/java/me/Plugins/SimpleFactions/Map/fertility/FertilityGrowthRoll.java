package me.Plugins.SimpleFactions.Map.fertility;

import java.util.OptionalDouble;
import java.util.Random;

public final class FertilityGrowthRoll {
    private FertilityGrowthRoll() {}

    static boolean allowsGrowth(
            int fertility,
            OptionalDouble weight,
            boolean featureEnabled,
            boolean provinceSystemActive,
            Random random) {
        if (!featureEnabled || !provinceSystemActive) {
            return true;
        }
        if (weight == null || weight.isEmpty() || random == null) {
            return true;
        }
        if (fertility >= 100) {
            return true;
        }
        if (fertility <= 0) {
            return false;
        }
        return FertilityGrowthChance.rollGrows(fertility, weight.getAsDouble(), random);
    }
}
