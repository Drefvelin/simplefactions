package me.Plugins.SimpleFactions.Map.fertility;

import java.util.Locale;
import java.util.OptionalDouble;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import me.Plugins.SimpleFactions.Loaders.FertilityCropsLoader;

public final class FertilityCustomGrowth {
    private FertilityCustomGrowth() {}

    public static boolean allowsGrowth(String cropId, int fertility, OptionalDouble weightOverride) {
        return allowsGrowth(
                cropId,
                fertility,
                weightOverride,
                FertilityCropsLoader.isEnabled(),
                FertilityProvinceResolver.isActive(),
                FertilityCropsLoader.getRegistry(),
                ThreadLocalRandom.current());
    }

    static boolean allowsGrowth(
            String cropId,
            int fertility,
            OptionalDouble weightOverride,
            boolean featureEnabled,
            boolean provinceSystemActive,
            FertilityCropRegistry registry,
            Random random) {
        OptionalDouble weight = resolveWeight(cropId, weightOverride, registry);
        return FertilityGrowthRoll.allowsGrowth(
                fertility,
                weight,
                featureEnabled,
                provinceSystemActive,
                random);
    }

    static OptionalDouble resolveWeight(
            String cropId,
            OptionalDouble weightOverride,
            FertilityCropRegistry registry) {
        if (weightOverride != null && weightOverride.isPresent()) {
            return weightOverride;
        }
        if (cropId == null || cropId.isBlank() || registry == null) {
            return OptionalDouble.empty();
        }
        return registry.weightForCustom(cropId.toLowerCase(Locale.ROOT));
    }
}
