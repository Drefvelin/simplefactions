package me.Plugins.SimpleFactions.Map.fertility;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class FertilityVanillaGrowthTest {
    private static final FertilityCropRegistry WHEAT_ONLY = FertilityCropRegistry.builder()
            .vanilla(Material.WHEAT, 0.90)
            .build();

    @Test
    void featureDisabled_neverCancels() {
        assertFalse(FertilityVanillaGrowth.shouldCancelGrowth(
                Material.WHEAT, 0, false, true, WHEAT_ONLY, randomReturning(1.0)));
    }

    @Test
    void provinceSystemInactive_neverCancels() {
        assertFalse(FertilityVanillaGrowth.shouldCancelGrowth(
                Material.WHEAT, 0, true, false, WHEAT_ONLY, randomReturning(1.0)));
    }

    @Test
    void unregisteredMaterial_neverCancels() {
        assertFalse(FertilityVanillaGrowth.shouldCancelGrowth(
                Material.POTATOES, 0, true, true, WHEAT_ONLY, randomReturning(1.0)));
    }

    @Test
    void fertility100_neverCancels() {
        assertFalse(FertilityVanillaGrowth.shouldCancelGrowth(
                Material.WHEAT, 100, true, true, WHEAT_ONLY, randomReturning(1.0)));
    }

    @Test
    void fertility0_alwaysCancels() {
        assertTrue(FertilityVanillaGrowth.shouldCancelGrowth(
                Material.WHEAT, 0, true, true, WHEAT_ONLY, randomReturning(0.0)));
    }

    @Test
    void failedRoll_cancels() {
        assertTrue(FertilityVanillaGrowth.shouldCancelGrowth(
                Material.WHEAT, 50, true, true, WHEAT_ONLY, randomReturning(1.0)));
    }

    @Test
    void passedRoll_doesNotCancel() {
        assertFalse(FertilityVanillaGrowth.shouldCancelGrowth(
                Material.WHEAT, 50, true, true, WHEAT_ONLY, randomReturning(0.0)));
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
