package me.Plugins.SimpleFactions.vehicles;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    }

    @AfterEach
    void tearDown() throws IOException {
        if (tempDir != null) {
            Files.walk(tempDir)
                .sorted(java.util.Comparator.reverseOrder())
                .forEach(path -> path.toFile().delete());
        }
    }

    private void loadConfig(String yaml) throws IOException {
        Path vehiclesYaml = tempDir.resolve("vehicles.yml");
        Files.writeString(vehiclesYaml, yaml);
        VehiclesConfigLoader.load(vehiclesYaml.toFile());
    }

    @Test
    void checkCanBuild_okWhenUnderLimits() throws IOException {
        loadConfig("""
            personal-slot-limit: 3
            default-upkeep: 4

            categories:
              ships:
                ironclad:
                  upkeep: 20
                  size: 1
              static_emplacements: {}
              aircraft: {}
            """);

        PlayerVehicleRegistry registry = new PlayerVehicleRegistry();
        UUID player = UUID.randomUUID();

        assertEquals(CanBuildResult.OK, VehicleSlotGuard.checkCanBuild(player, "ironclad", registry));
    }

    @Test
    void checkCanBuild_unknownType() throws IOException {
        loadConfig("""
            personal-slot-limit: 1

            categories:
              ships:
                ironclad:
                  upkeep: 20
                  size: 1
              static_emplacements: {}
              aircraft: {}
            """);

        PlayerVehicleRegistry registry = new PlayerVehicleRegistry();
        UUID player = UUID.randomUUID();

        assertEquals(
            CanBuildResult.UNKNOWN_TYPE,
            VehicleSlotGuard.checkCanBuild(player, "unknown", registry));
    }

    @Test
    void checkCanBuild_perTypeLimit() throws IOException {
        loadConfig("""
            personal-slot-limit: 3
            default-upkeep: 4

            categories:
              aircraft:
                cloudskimmer:
                  size: 1
              ships: {}
              static_emplacements: {}
            """);

        PlayerVehicleRegistry registry = new PlayerVehicleRegistry();
        UUID player = UUID.randomUUID();
        registry.register(new PlayerVehicleRecord(
            player, "vehicle-1", "cloudskimmer", OwnershipMode.PERSONAL, null));

        assertEquals(
            CanBuildResult.PER_TYPE_LIMIT,
            VehicleSlotGuard.checkCanBuild(player, "cloudskimmer", registry));
    }

    @Test
    void checkCanBuild_landPerPersonThree() throws IOException {
        loadConfig("""
            personal-slot-limit: 3
            default-upkeep: 4

            categories:
              land_vehicles:
                horse_cart:
                  upkeep: 5
                  size: 1
                  per-person: 3
              ships: {}
              static_emplacements: {}
              aircraft: {}
            """);

        PlayerVehicleRegistry registry = new PlayerVehicleRegistry();
        UUID player = UUID.randomUUID();
        registry.register(new PlayerVehicleRecord(
            player, "vehicle-1", "horse_cart", OwnershipMode.PERSONAL, null));
        registry.register(new PlayerVehicleRecord(
            player, "vehicle-2", "horse_cart", OwnershipMode.PERSONAL, null));

        assertEquals(
            CanBuildResult.OK,
            VehicleSlotGuard.checkCanBuild(player, "horse_cart", registry));

        registry.register(new PlayerVehicleRecord(
            player, "vehicle-3", "horse_cart", OwnershipMode.PERSONAL, null));

        assertEquals(
            CanBuildResult.PER_TYPE_LIMIT,
            VehicleSlotGuard.checkCanBuild(player, "horse_cart", registry));
    }

    @Test
    void checkCanBuild_totalLimit() throws IOException {
        loadConfig("""
            personal-slot-limit: 3
            default-upkeep: 4

            categories:
              ships:
                ironclad:
                  upkeep: 20
                  size: 1
                  per-person: 3
                gunboat:
                  upkeep: 10
                  size: 1
                  per-person: 3
                cruiser:
                  upkeep: 40
                  size: 2
                  per-person: 3
              static_emplacements: {}
              aircraft: {}
            """);

        PlayerVehicleRegistry registry = new PlayerVehicleRegistry();
        UUID player = UUID.randomUUID();
        registry.register(new PlayerVehicleRecord(
            player, "vehicle-1", "ironclad", OwnershipMode.PERSONAL, null));
        registry.register(new PlayerVehicleRecord(
            player, "vehicle-2", "gunboat", OwnershipMode.PERSONAL, null));
        registry.register(new PlayerVehicleRecord(
            player, "vehicle-3", "cruiser", OwnershipMode.PERSONAL, null));

        assertEquals(
            CanBuildResult.TOTAL_LIMIT,
            VehicleSlotGuard.checkCanBuild(player, "ironclad", registry));
    }

    @Test
    void checkCanBuild_ignoreLimitSkipsTotal() throws IOException {
        loadConfig("""
            personal-slot-limit: 3
            default-upkeep: 4

            categories:
              ships:
                ironclad:
                  upkeep: 20
                  size: 1
                gunboat:
                  upkeep: 10
                  size: 1
                cruiser:
                  upkeep: 40
                  size: 2
              train:
                coal_car:
                  upkeep: 1
                  size: 1
                  ignore-limit: true
              static_emplacements: {}
              aircraft: {}
            """);

        PlayerVehicleRegistry registry = new PlayerVehicleRegistry();
        UUID player = UUID.randomUUID();
        registry.register(new PlayerVehicleRecord(
            player, "vehicle-1", "ironclad", OwnershipMode.PERSONAL, null));
        registry.register(new PlayerVehicleRecord(
            player, "vehicle-2", "gunboat", OwnershipMode.PERSONAL, null));
        registry.register(new PlayerVehicleRecord(
            player, "vehicle-3", "cruiser", OwnershipMode.PERSONAL, null));

        assertEquals(
            CanBuildResult.OK,
            VehicleSlotGuard.checkCanBuild(player, "coal_car", registry));
    }

    @Test
    void checkCanBuild_ignoreLimitStillPerType() throws IOException {
        loadConfig("""
            personal-slot-limit: 3
            default-upkeep: 4

            categories:
              train:
                coal_car:
                  upkeep: 1
                  size: 1
                  ignore-limit: true
                  per-person: 3
              ships: {}
              static_emplacements: {}
              aircraft: {}
            """);

        PlayerVehicleRegistry registry = new PlayerVehicleRegistry();
        UUID player = UUID.randomUUID();
        registry.register(new PlayerVehicleRecord(
            player, "vehicle-1", "coal_car", OwnershipMode.PERSONAL, null));
        registry.register(new PlayerVehicleRecord(
            player, "vehicle-2", "coal_car", OwnershipMode.PERSONAL, null));
        registry.register(new PlayerVehicleRecord(
            player, "vehicle-3", "coal_car", OwnershipMode.PERSONAL, null));

        assertEquals(
            CanBuildResult.PER_TYPE_LIMIT,
            VehicleSlotGuard.checkCanBuild(player, "coal_car", registry));
    }

    @Test
    void checkCanBuild_unlimitedTotalWhenLimitZero() throws IOException {
        loadConfig("""
            personal-slot-limit: 0
            default-upkeep: 4

            categories:
              ships:
                ironclad:
                  upkeep: 20
                  size: 1
                  per-person: 3
              static_emplacements: {}
              aircraft: {}
            """);

        PlayerVehicleRegistry registry = new PlayerVehicleRegistry();
        UUID player = UUID.randomUUID();
        registry.register(new PlayerVehicleRecord(
            player, "vehicle-1", "ironclad", OwnershipMode.PERSONAL, null));
        registry.register(new PlayerVehicleRecord(
            player, "vehicle-2", "ironclad", OwnershipMode.PERSONAL, null));

        assertEquals(
            CanBuildResult.OK,
            VehicleSlotGuard.checkCanBuild(player, "ironclad", registry));
    }

    @Test
    void checkCanBuild_perTypeCheckedBeforeTotal() throws IOException {
        loadConfig("""
            personal-slot-limit: 1
            default-upkeep: 4

            categories:
              ships:
                ironclad:
                  upkeep: 20
                  size: 1
              static_emplacements: {}
              aircraft: {}
            """);

        PlayerVehicleRegistry registry = new PlayerVehicleRegistry();
        UUID player = UUID.randomUUID();
        registry.register(new PlayerVehicleRecord(
            player, "vehicle-1", "ironclad", OwnershipMode.PERSONAL, null));

        assertEquals(
            CanBuildResult.PER_TYPE_LIMIT,
            VehicleSlotGuard.checkCanBuild(player, "ironclad", registry));
    }
}
