package me.Plugins.SimpleFactions.Loaders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FertilityCropsLoaderTest {
    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        FertilityCropsLoader.resetForTests();
        tempDir = Files.createTempDirectory("sf-fertility-crops-");
    }

    @AfterEach
    void tearDown() throws IOException {
        FertilityCropsLoader.resetForTests();
        if (tempDir != null) {
            Files.walk(tempDir)
                    .sorted(java.util.Comparator.reverseOrder())
                    .forEach(path -> path.toFile().delete());
        }
    }

    @Test
    void load_readsVanillaAndCustom() throws IOException {
        Path file = writeConfig("""
                enabled: true
                vanilla:
                  WHEAT: 0.90
                customcrops:
                  tomato: 0.75
                """);

        FertilityCropsLoader.load(file.toFile());

        assertTrue(FertilityCropsLoader.isEnabled());
        assertEquals(0.90, FertilityCropsLoader.getRegistry().weightFor(Material.WHEAT).orElseThrow());
        assertEquals(0.75, FertilityCropsLoader.getRegistry().weightForCustom("tomato").orElseThrow());
    }

    @Test
    void load_disabled_returnsEmptyRegistry() throws IOException {
        Path file = writeConfig("""
                enabled: false
                vanilla:
                  WHEAT: 0.90
                """);

        FertilityCropsLoader.load(file.toFile());

        assertFalse(FertilityCropsLoader.isEnabled());
        assertTrue(FertilityCropsLoader.getRegistry().isEmpty());
    }

    @Test
    void load_unknownVanillaMaterial_fails() throws IOException {
        Path file = writeConfig("""
                enabled: true
                vanilla:
                  NOT_A_REAL_BLOCK: 0.50
                """);

        assertThrows(IllegalStateException.class, () -> FertilityCropsLoader.load(file.toFile()));
    }

    @Test
    void load_invalidWeight_fails() throws IOException {
        Path zeroWeight = writeConfig("""
                enabled: true
                vanilla:
                  WHEAT: 0.0
                """);
        assertThrows(IllegalStateException.class, () -> FertilityCropsLoader.load(zeroWeight.toFile()));

        Path highWeight = writeConfig("""
                enabled: true
                customcrops:
                  tomato: 1.5
                """);
        assertThrows(IllegalStateException.class, () -> FertilityCropsLoader.load(highWeight.toFile()));
    }

    @Test
    void resetForTests_clearsState() throws IOException {
        Path file = writeConfig("""
                enabled: true
                vanilla:
                  WHEAT: 0.90
                """);

        FertilityCropsLoader.load(file.toFile());
        assertTrue(FertilityCropsLoader.isEnabled());

        FertilityCropsLoader.resetForTests();

        assertFalse(FertilityCropsLoader.isEnabled());
        assertTrue(FertilityCropsLoader.getRegistry().isEmpty());
    }

    private Path writeConfig(String yaml) throws IOException {
        Path file = tempDir.resolve("fertility-crops.yml");
        Files.writeString(file, yaml);
        return file;
    }
}
