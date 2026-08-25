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

import me.Plugins.SimpleFactions.Loaders.VehiclesConfigLoader;

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
    void countPersonalOfType_countsOnlyMatchingPersonalRows() throws IOException {
        Path tempConfigDir = Files.createTempDirectory("sf-registry-of-type-");
        try {
            Path vehiclesYaml = tempConfigDir.resolve("vehicles.yml");
            Files.writeString(vehiclesYaml, """
                personal-slot-limit: 1

                categories:
                  ships:
                    ironclad:
                      upkeep: 20
                      size: 1
                    cruiser:
                      upkeep: 40
                      size: 2
                  static_emplacements: {}
                  aircraft: {}
                """);
            VehiclesConfigLoader.load(vehiclesYaml.toFile());

            UUID player = UUID.randomUUID();
            UUID otherPlayer = UUID.randomUUID();
            registry.register(new PlayerVehicleRecord(
                player, "vehicle-1", "ironclad", OwnershipMode.PERSONAL, null));
            registry.register(new PlayerVehicleRecord(
                player, "vehicle-2", "cruiser", OwnershipMode.PERSONAL, null));
            registry.register(new PlayerVehicleRecord(
                player, "vehicle-3", "ironclad", OwnershipMode.INSTALLATION, "port-1"));
            registry.register(new PlayerVehicleRecord(
                otherPlayer, "vehicle-4", "ironclad", OwnershipMode.PERSONAL, null));

            assertEquals(1, registry.countPersonalOfType(player, "ironclad"));
            assertEquals(1, registry.countPersonalOfType(player, "cruiser"));
            assertEquals(1, registry.countPersonalOfType(player, "Ironclad"));
            assertEquals(0, registry.countPersonalOfType(player, "unknown"));
            assertEquals(0, registry.countPersonalOfType(player, null));
        } finally {
            Files.walk(tempConfigDir)
                .sorted(java.util.Comparator.reverseOrder())
                .forEach(path -> path.toFile().delete());
        }
    }

    @Test
    void countPersonalExcludingIgnoreLimit_excludesIgnoreLimitTypes() throws IOException {
        Path tempConfigDir = Files.createTempDirectory("sf-registry-exclude-ignore-");
        try {
            Path vehiclesYaml = tempConfigDir.resolve("vehicles.yml");
            Files.writeString(vehiclesYaml, """
                personal-slot-limit: 3
                default-upkeep: 4

                categories:
                  ships:
                    ironclad:
                      upkeep: 20
                      size: 1
                  train:
                    coal_car:
                      upkeep: 1
                      size: 1
                      ignore-limit: true
                  static_emplacements: {}
                  aircraft: {}
                """);
            VehiclesConfigLoader.load(vehiclesYaml.toFile());

            UUID player = UUID.randomUUID();
            registry.register(new PlayerVehicleRecord(
                player, "ship-1", "ironclad", OwnershipMode.PERSONAL, null));
            registry.register(new PlayerVehicleRecord(
                player, "train-1", "coal_car", OwnershipMode.PERSONAL, null));
            registry.register(new PlayerVehicleRecord(
                player, "train-2", "coal_car", OwnershipMode.PERSONAL, null));
            registry.register(new PlayerVehicleRecord(
                player, "ship-2", "ironclad", OwnershipMode.INSTALLATION, "port-1"));

            assertEquals(1, registry.countPersonalExcludingIgnoreLimit(player));
        } finally {
            Files.walk(tempConfigDir)
                .sorted(java.util.Comparator.reverseOrder())
                .forEach(path -> path.toFile().delete());
        }
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

    @Test
    void getByInstallationId_returnsOnlyMatchingInstallationRows() {
        UUID player = UUID.randomUUID();
        registry.register(new PlayerVehicleRecord(
            player, "vehicle-1", "ironclad", OwnershipMode.INSTALLATION, "port-1"));
        registry.register(new PlayerVehicleRecord(
            player, "vehicle-2", "ironclad", OwnershipMode.INSTALLATION, "port-2"));
        registry.register(new PlayerVehicleRecord(
            player, "vehicle-3", "ironclad", OwnershipMode.PERSONAL, null));

        assertEquals(1, registry.getByInstallationId("port-1").size());
        assertEquals("vehicle-1", registry.getByInstallationId("port-1").get(0).getVehicleUuid());
        assertEquals(0, registry.getByInstallationId(null).size());
    }

    @Test
    void usedCategorySize_sumsSizesForInstallationAndCategory() throws IOException {
        Path tempConfigDir = Files.createTempDirectory("sf-registry-used-size-");
        try {
            Path vehiclesYaml = tempConfigDir.resolve("vehicles.yml");
            Files.writeString(vehiclesYaml, """
                personal-slot-limit: 1

                categories:
                  ships:
                    ironclad:
                      upkeep: 20
                      size: 1
                    cruiser:
                      upkeep: 40
                      size: 2
                  static_emplacements: {}
                  aircraft: {}
                """);
            VehiclesConfigLoader.load(vehiclesYaml.toFile());

            UUID player = UUID.randomUUID();
            registry.register(new PlayerVehicleRecord(
                player, "ship-1", "ironclad", OwnershipMode.INSTALLATION, "port-1"));
            registry.register(new PlayerVehicleRecord(
                player, "ship-2", "cruiser", OwnershipMode.INSTALLATION, "port-1"));
            registry.register(new PlayerVehicleRecord(
                player, "ship-3", "ironclad", OwnershipMode.INSTALLATION, "port-2"));
            registry.register(new PlayerVehicleRecord(
                player, "ship-4", "ironclad", OwnershipMode.PERSONAL, null));

            assertEquals(3, registry.usedCategorySize("port-1", "ships"));
            assertEquals(0, registry.usedCategorySize("port-1", "aircraft"));
            assertEquals(1, registry.usedCategorySize("port-2", "ships"));
        } finally {
            Files.walk(tempConfigDir)
                .sorted(java.util.Comparator.reverseOrder())
                .forEach(path -> path.toFile().delete());
        }
    }
}
