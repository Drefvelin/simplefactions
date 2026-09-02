package me.Plugins.SimpleFactions.Map.fertility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class FertilityCropRegistryTest {
    @Test
    void emptyRegistry_hasNoEntries() {
        assertTrue(FertilityCropRegistry.EMPTY.isEmpty());
        assertTrue(FertilityCropRegistry.EMPTY.weightFor(Material.WHEAT).isEmpty());
        assertTrue(FertilityCropRegistry.EMPTY.weightForCustom("tomato").isEmpty());
    }

    @Test
    void builder_roundTripsVanillaAndCustomLookups() {
        FertilityCropRegistry registry = FertilityCropRegistry.builder()
                .vanilla(Material.WHEAT, 0.90)
                .custom("tomato", 0.75)
                .custom("Tomato", 0.75)
                .build();

        assertFalse(registry.isEmpty());
        assertEquals(0.90, registry.weightFor(Material.WHEAT).orElseThrow());
        assertEquals(0.75, registry.weightForCustom("tomato").orElseThrow());
        assertEquals(0.75, registry.weightForCustom("TOMATO").orElseThrow());
        assertTrue(registry.weightFor(Material.POTATOES).isEmpty());
        assertTrue(registry.weightForCustom("rice").isEmpty());
        assertTrue(registry.weightFor(null).isEmpty());
        assertTrue(registry.weightForCustom(null).isEmpty());
        assertTrue(registry.weightForCustom(" ").isEmpty());
    }

    @Test
    void builder_invalidWeight_throws() {
        FertilityCropRegistry.Builder builder = FertilityCropRegistry.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.vanilla(Material.WHEAT, 0.0));
        assertThrows(IllegalArgumentException.class, () -> builder.custom("tomato", 1.5));
        assertThrows(IllegalArgumentException.class, () -> builder.custom("", 0.5));
    }
}
