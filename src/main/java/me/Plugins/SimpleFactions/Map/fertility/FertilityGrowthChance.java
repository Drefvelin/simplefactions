package me.Plugins.SimpleFactions.Map.fertility;

import java.util.Random;

public final class FertilityGrowthChance {
    private FertilityGrowthChance() {}

    public static double growChance(int fertility, double weight) {
        validateWeight(weight);
        if (fertility <= 0) {
            return 0.0;
        }
        if (fertility >= 100) {
            return 1.0;
        }
        return Math.pow(fertility / 100.0, weight);
    }

    public static boolean rollGrows(int fertility, double weight, Random random) {
        double chance = growChance(fertility, weight);
        if (chance >= 1.0) {
            return true;
        }
        if (chance <= 0.0) {
            return false;
        }
        return random.nextDouble() < chance;
    }

    static void validateWeight(double weight) {
        if (weight <= 0.0 || weight > 1.0) {
            throw new IllegalArgumentException("weight must be in (0, 1], got " + weight);
        }
    }
}
