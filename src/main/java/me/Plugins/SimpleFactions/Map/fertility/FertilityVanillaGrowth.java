package me.Plugins.SimpleFactions.Map.fertility;

import java.util.OptionalDouble;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Material;

import me.Plugins.SimpleFactions.Loaders.FertilityCropsLoader;

public final class FertilityVanillaGrowth {
    private FertilityVanillaGrowth() {}

    public static boolean shouldCancelGrowth(Material material, int fertility) {
        return shouldCancelGrowth(
                material,
                fertility,
                FertilityCropsLoader.isEnabled(),
                FertilityProvinceResolver.isActive(),
                FertilityCropsLoader.getRegistry(),
                ThreadLocalRandom.current());
    }

    static boolean shouldCancelGrowth(
            Material material,
            int fertility,
            boolean featureEnabled,
            boolean provinceSystemActive,
            FertilityCropRegistry registry,
            Random random) {
        if (material == null || registry == null) {
            return false;
        }
        OptionalDouble weight = registry.weightFor(material);
        return !FertilityGrowthRoll.allowsGrowth(
                fertility,
                weight,
                featureEnabled,
                provinceSystemActive,
                random);
    }
}
