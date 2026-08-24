package me.Plugins.SimpleFactions.vehicles;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import me.Plugins.SimpleFactions.Loaders.VehiclesConfigLoader;

class VehicleSlotGuardTest {
    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("sf-vehicle-slot-guard-");
        Path vehiclesYaml = tempDir.resolve("vehicles.yml");
        Files.writeString(vehiclesYaml, """
            personal-slot-limit: 1

            upkeep:
              ironclad: 20
            """);
        VehiclesConfigLoader.load(vehiclesYaml.toFile());
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
    void availableWhenUnderLimit() {
        PlayerVehicleRegistry registry = new PlayerVehicleRegistry();
        UUID player = UUID.randomUUID();

        assertTrue(VehicleSlotGuard.isPersonalSlotAvailable(player, registry));
    }

    @Test
    void blockedWhenAtLimit() {
        PlayerVehicleRegistry registry = new PlayerVehicleRegistry();
        UUID player = UUID.randomUUID();
        registry.register(new PlayerVehicleRecord(
            player, "vehicle-1", "ironclad", OwnershipMode.PERSONAL, null));

        assertFalse(VehicleSlotGuard.isPersonalSlotAvailable(player, registry));
    }

    @Test
    void unlimitedWhenLimitZero() throws IOException {
        Path vehiclesYaml = tempDir.resolve("vehicles-unlimited.yml");
        Files.writeString(vehiclesYaml, "personal-slot-limit: 0\n");
        VehiclesConfigLoader.load(vehiclesYaml.toFile());

        PlayerVehicleRegistry registry = new PlayerVehicleRegistry();
        UUID player = UUID.randomUUID();
        registry.register(new PlayerVehicleRecord(
            player, "vehicle-1", "ironclad", OwnershipMode.PERSONAL, null));
        registry.register(new PlayerVehicleRecord(
            player, "vehicle-2", "ironclad", OwnershipMode.PERSONAL, null));

        assertTrue(VehicleSlotGuard.isPersonalSlotAvailable(player, registry));
    }
}
