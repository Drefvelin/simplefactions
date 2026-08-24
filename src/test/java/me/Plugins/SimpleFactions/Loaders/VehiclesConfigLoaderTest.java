package me.Plugins.SimpleFactions.Loaders;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VehiclesConfigLoaderTest {
    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("sf-vehicles-config-");
    }

    @AfterEach
    void tearDown() throws IOException {
        if (tempDir != null) {
            Files.walk(tempDir)
                .sorted(java.util.Comparator.reverseOrder())
                .forEach(path -> path.toFile().delete());
        }
    }

    @Test
    void load_readsSlotLimitAndUpkeep() throws IOException {
        Path vehiclesYaml = tempDir.resolve("vehicles.yml");
        Files.writeString(vehiclesYaml, """
            personal-slot-limit: 1

            upkeep:
              ironclad: 20
            """);

        VehiclesConfigLoader.load(vehiclesYaml.toFile());

        assertEquals(1, VehiclesConfigLoader.getPersonalSlotLimit());
        assertEquals(20.0, VehiclesConfigLoader.getUpkeep("ironclad"));
        assertEquals(0.0, VehiclesConfigLoader.getUpkeep("unknown"));
    }
}
