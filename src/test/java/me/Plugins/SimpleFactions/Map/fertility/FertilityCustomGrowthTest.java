package me.Plugins.SimpleFactions.Map.fertility;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalDouble;
import java.util.Random;

import org.junit.jupiter.api.Test;

class FertilityCustomGrowthTest {
    private static final FertilityCropRegistry TOMATO_ONLY = FertilityCropRegistry.builder()
            .custom("tomato", 0.75)
            .build();

    @Test
    void featureDisabled_allowsGrowth() {
        assertTrue(FertilityCustomGrowth.allowsGrowth(
                "tomato", 0, OptionalDouble.empty(), false, true, TOMATO_ONLY, randomReturning(1.0)));
    }

    @Test
    void provinceSystemInactive_allowsGrowth() {
        assertTrue(FertilityCustomGrowth.allowsGrowth(
                "tomato", 0, OptionalDouble.empty(), true, false, TOMATO_ONLY, randomReturning(1.0)));
    }

    @Test
    void unregisteredCropId_allowsGrowth() {
        assertTrue(FertilityCustomGrowth.allowsGrowth(
                "rice", 0, OptionalDouble.empty(), true, true, TOMATO_ONLY, randomReturning(1.0)));
    }

    @Test
    void fertility100_allowsGrowth() {
        assertTrue(FertilityCustomGrowth.allowsGrowth(
                "tomato", 100, OptionalDouble.empty(), true, true, TOMATO_ONLY, randomReturning(1.0)));
    }

    @Test
    void fertility0_deniesGrowth() {
        assertFalse(FertilityCustomGrowth.allowsGrowth(
                "tomato", 0, OptionalDouble.empty(), true, true, TOMATO_ONLY, randomReturning(0.0)));
    }

    @Test
    void failedRoll_deniesGrowth() {
        assertFalse(FertilityCustomGrowth.allowsGrowth(
                "tomato", 50, OptionalDouble.empty(), true, true, TOMATO_ONLY, randomReturning(1.0)));
    }

    @Test
    void passedRoll_allowsGrowth() {
        assertTrue(FertilityCustomGrowth.allowsGrowth(
                "tomato", 50, OptionalDouble.empty(), true, true, TOMATO_ONLY, randomReturning(0.0)));
    }

    @Test
    void weightOverride_beatsRegistry() {
        FertilityCropRegistry registry = FertilityCropRegistry.builder()
                .custom("tomato", 0.90)
                .build();
        double roll = 0.80;
        assertFalse(FertilityCustomGrowth.allowsGrowth(
                "tomato",
                50,
                OptionalDouble.empty(),
                true,
                true,
                registry,
                randomReturning(roll)));
        assertTrue(FertilityCustomGrowth.allowsGrowth(
                "tomato",
                50,
                OptionalDouble.of(0.10),
                true,
                true,
                registry,
                randomReturning(roll)));
    }

    private static Random randomReturning(double value) {
        return new Random() {
            @Override
            public double nextDouble() {
                return value;
            }
        };
    }
}
