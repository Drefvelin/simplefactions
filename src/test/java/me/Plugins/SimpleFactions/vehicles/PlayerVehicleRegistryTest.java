package me.Plugins.SimpleFactions.vehicles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlayerVehicleRegistryTest {
    private Path tempDir;
    private PlayerVehicleRegistry registry;
    private VehicleRegistryPersistence persistence;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("sf-vehicle-registry-");
        registry = new PlayerVehicleRegistry();
        persistence = new VehicleRegistryPersistence(tempDir.toFile(), registry);
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
    void countPersonal_countsOnlyPersonalRowsForPlayer() {
        UUID player = UUID.randomUUID();

        registry.register(new PlayerVehicleRecord(
            player, "vehicle-1", "ironclad", OwnershipMode.PERSONAL, null));
        registry.register(new PlayerVehicleRecord(
            player, "vehicle-2", "ironclad", OwnershipMode.PERSONAL, null));
        registry.register(new PlayerVehicleRecord(
            player, "vehicle-3", "ironclad", OwnershipMode.INSTALLATION, "fort-1"));

        assertEquals(2, registry.countPersonal(player));
    }

    @Test
    void saveAndLoad_roundTripPreservesRecords() {
        UUID player = UUID.randomUUID();
        registry.register(new PlayerVehicleRecord(
            player, "vehicle-1", "ironclad", OwnershipMode.PERSONAL, null));
        registry.register(new PlayerVehicleRecord(
            player, "vehicle-2", "ironclad", OwnershipMode.INSTALLATION, "port-1"));

        persistence.save();

        PlayerVehicleRegistry loaded = new PlayerVehicleRegistry();
        VehicleRegistryPersistence loadedPersistence =
            new VehicleRegistryPersistence(tempDir.toFile(), loaded);
        loadedPersistence.load();

        assertEquals(1, loaded.countPersonal(player));
        assertTrue(loaded.getByVehicleUuid("vehicle-1").isPresent());
        assertTrue(loaded.getByVehicleUuid("vehicle-2").isPresent());
        assertEquals(OwnershipMode.INSTALLATION,
            loaded.getByVehicleUuid("vehicle-2").get().getMode());
        assertEquals("port-1",
            loaded.getByVehicleUuid("vehicle-2").get().getInstallationId());
    }

    @Test
    void register_replacesDuplicateVehicleUuid() {
        UUID player = UUID.randomUUID();
        registry.register(new PlayerVehicleRecord(
            player, "vehicle-1", "ironclad", OwnershipMode.PERSONAL, null));
        registry.register(new PlayerVehicleRecord(
            UUID.randomUUID(), "vehicle-1", "cruiser", OwnershipMode.PERSONAL, null));

        assertEquals(1, registry.getAll().size());
        assertEquals("cruiser", registry.getByVehicleUuid("vehicle-1").get().getVehicleTypeId());
    }

    @Test
    void save_emptyRegistryDoesNotCreateFile() {
        persistence.save();
        assertEquals(false, new File(tempDir.toFile(), "vehicles_registry.json").exists());
    }
}
