package me.Plugins.SimpleFactions.vehicles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import me.Plugins.SimpleFactions.Loaders.InstallationConfigLoader;
import me.Plugins.SimpleFactions.Loaders.VehiclesConfigLoader;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.installation.Installation;
import me.Plugins.SimpleFactions.installation.InstallationBounds;
import me.Plugins.SimpleFactions.installation.InstallationKind;
import me.Plugins.SimpleFactions.vehicles.InstallationVehicleService.VehicleBerthTarget;
import net.tfminecraft.VehicleFramework.Data.OwnerData;

class InstallationVehicleServiceTest {
    private Path tempDir;
    private PlayerVehicleRegistry registry;
    private InstallationVehicleService service;
    private Installation port;
    private Installation fort;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("sf-berth-service-");
        writeVehiclesFixture();
        InstallationConfigLoader.load(writeInstallationsFixture().toFile());

        registry = new PlayerVehicleRegistry();
        InstallationVehicleOwnerSync ownerSync = new InstallationVehicleOwnerSync(registry);
        service = new InstallationVehicleService(registry, ownerSync);
        port = new Installation("port-1", "Harbour", InstallationKind.PORT, 42, 0, 0, 0L);
        fort = new Installation("fort-1", "Redoubt", InstallationKind.FORT, 42, 0, 0, 0L);
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
    void canRegister_nullRecord_returnsNotInRegistry() {
        VehicleBerthTarget vehicle = berthTarget("vehicle-1", locationAt(0, 64, 0));

        assertEquals(
            CanRegisterResult.NOT_IN_REGISTRY,
            service.canRegister(port, vehicle, null));
    }

    @Test
    void canRegister_uuidMismatch_returnsNotInRegistry() {
        VehicleBerthTarget vehicle = berthTarget("vehicle-1", locationAt(0, 64, 0));
        PlayerVehicleRecord record = personalRecord("vehicle-2", "ironclad");

        assertEquals(
            CanRegisterResult.NOT_IN_REGISTRY,
            service.canRegister(port, vehicle, record));
    }

    @Test
    void canRegister_alreadyBerthed_returnsAlreadyBerthed() {
        VehicleBerthTarget vehicle = berthTarget("vehicle-1", locationAt(0, 64, 0));
        PlayerVehicleRecord record = new PlayerVehicleRecord(
            UUID.randomUUID(),
            "vehicle-1",
            "ironclad",
            OwnershipMode.INSTALLATION,
            "port-1");

        assertEquals(
            CanRegisterResult.ALREADY_BERTHED,
            service.canRegister(port, vehicle, record));
    }

    @Test
    void canRegister_unknownType_returnsUnknownType() {
        VehicleBerthTarget vehicle = berthTarget("vehicle-1", locationAt(0, 64, 0));
        PlayerVehicleRecord record = personalRecord("vehicle-1", "unknown-type");

        assertEquals(
            CanRegisterResult.UNKNOWN_TYPE,
            service.canRegister(port, vehicle, record));
    }

    @Test
    void canRegister_unsupportedCategory_returnsUnsupportedCategory() {
        VehicleBerthTarget vehicle = berthTarget("vehicle-1", locationAt(0, 64, 0));
        PlayerVehicleRecord record = personalRecord("vehicle-1", "ironclad");

        assertEquals(
            CanRegisterResult.UNSUPPORTED_CATEGORY,
            service.canRegister(fort, vehicle, record));
    }

    @Test
    void canRegister_noCapacity_returnsNoCapacity() {
        for (int i = 0; i < 8; i++) {
            registry.register(new PlayerVehicleRecord(
                UUID.randomUUID(),
                "berthed-" + i,
                "ironclad",
                OwnershipMode.INSTALLATION,
                port.getId()));
        }

        VehicleBerthTarget vehicle = berthTarget("vehicle-1", locationAt(0, 64, 0));
        PlayerVehicleRecord record = personalRecord("vehicle-1", "ironclad");

        try (MockedStatic<InstallationBounds> bounds = mockStatic(InstallationBounds.class)) {
            bounds.when(() -> InstallationBounds.isWithinRadius(eq(port), any())).thenReturn(true);
            bounds.when(() -> InstallationBounds.isCorrectProvince(eq(port), any())).thenReturn(true);

            assertEquals(
                CanRegisterResult.NO_CAPACITY,
                service.canRegister(port, vehicle, record));
        }
    }

    @Test
    void canRegister_outOfRadius_returnsOutOfRadius() {
        VehicleBerthTarget vehicle = berthTarget("vehicle-1", locationAt(0, 64, 0));
        PlayerVehicleRecord record = personalRecord("vehicle-1", "ironclad");

        try (MockedStatic<InstallationBounds> bounds = mockStatic(InstallationBounds.class)) {
            bounds.when(() -> InstallationBounds.isWithinRadius(eq(port), any())).thenReturn(false);

            assertEquals(
                CanRegisterResult.OUT_OF_RADIUS,
                service.canRegister(port, vehicle, record));
        }
    }

    @Test
    void canRegister_wrongProvince_returnsWrongProvince() {
        VehicleBerthTarget vehicle = berthTarget("vehicle-1", locationAt(0, 64, 0));
        PlayerVehicleRecord record = personalRecord("vehicle-1", "ironclad");

        try (MockedStatic<InstallationBounds> bounds = mockStatic(InstallationBounds.class)) {
            bounds.when(() -> InstallationBounds.isWithinRadius(eq(port), any())).thenReturn(true);
            bounds.when(() -> InstallationBounds.isCorrectProvince(eq(port), any())).thenReturn(false);

            assertEquals(
                CanRegisterResult.WRONG_PROVINCE,
                service.canRegister(port, vehicle, record));
        }
    }

    @Test
    void canRegister_allChecksPass_returnsOk() {
        VehicleBerthTarget vehicle = berthTarget("vehicle-1", locationAt(0, 64, 0));
        PlayerVehicleRecord record = personalRecord("vehicle-1", "ironclad");

        try (MockedStatic<InstallationBounds> bounds = mockStatic(InstallationBounds.class)) {
            bounds.when(() -> InstallationBounds.isWithinRadius(eq(port), any())).thenReturn(true);
            bounds.when(() -> InstallationBounds.isCorrectProvince(eq(port), any())).thenReturn(true);

            assertEquals(
                CanRegisterResult.OK,
                service.canRegister(port, vehicle, record));
        }
    }

    @Test
    void canRegister_landVehicleAtFort_returnsOk() {
        VehicleBerthTarget vehicle = berthTarget("vehicle-1", locationAt(0, 64, 0));
        PlayerVehicleRecord record = personalRecord("vehicle-1", "horse_cart");

        try (MockedStatic<InstallationBounds> bounds = mockStatic(InstallationBounds.class)) {
            bounds.when(() -> InstallationBounds.isWithinRadius(eq(fort), any())).thenReturn(true);
            bounds.when(() -> InstallationBounds.isCorrectProvince(eq(fort), any())).thenReturn(true);

            assertEquals(
                CanRegisterResult.OK,
                service.canRegister(fort, vehicle, record));
        }
    }

    @Test
    void canRegister_landVehicleAtFort_noCapacity() {
        registry.register(new PlayerVehicleRecord(
            UUID.randomUUID(),
            "berthed-1",
            "horse_cart",
            OwnershipMode.INSTALLATION,
            fort.getId()));
        registry.register(new PlayerVehicleRecord(
            UUID.randomUUID(),
            "berthed-2",
            "horse_cart",
            OwnershipMode.INSTALLATION,
            fort.getId()));

        VehicleBerthTarget vehicle = berthTarget("vehicle-1", locationAt(0, 64, 0));
        PlayerVehicleRecord record = personalRecord("vehicle-1", "horse_cart");

        try (MockedStatic<InstallationBounds> bounds = mockStatic(InstallationBounds.class)) {
            bounds.when(() -> InstallationBounds.isWithinRadius(eq(fort), any())).thenReturn(true);
            bounds.when(() -> InstallationBounds.isCorrectProvince(eq(fort), any())).thenReturn(true);

            assertEquals(
                CanRegisterResult.NO_CAPACITY,
                service.canRegister(fort, vehicle, record));
        }
    }

    @Test
    void canRegister_landVehicleAtPort_unsupportedCategory() {
        VehicleBerthTarget vehicle = berthTarget("vehicle-1", locationAt(0, 64, 0));
        PlayerVehicleRecord record = personalRecord("vehicle-1", "horse_cart");

        assertEquals(
            CanRegisterResult.UNSUPPORTED_CATEGORY,
            service.canRegister(port, vehicle, record));
    }

    @Test
    void register_updatesRegistryOwnerAndPersists() {
        UUID playerUuid = UUID.randomUUID();
        PlayerVehicleRecord record = new PlayerVehicleRecord(
            playerUuid,
            "vehicle-1",
            "ironclad",
            OwnershipMode.PERSONAL,
            null);
        registry.register(record);

        OwnerData ownerData = new OwnerData();
        VehicleBerthTarget vehicle = berthTarget("vehicle-1", locationAt(0, 64, 0), ownerData);

        Faction faction = mock(Faction.class);
        when(faction.getLeader()).thenReturn("Alice");

        SimpleFactions plugin = mock(SimpleFactions.class);
        try (MockedStatic<SimpleFactions> sf = mockStatic(SimpleFactions.class)) {
            sf.when(SimpleFactions::getInstance).thenReturn(plugin);

            service.register(port, vehicle, record, faction);

            PlayerVehicleRecord updated = registry.getByVehicleUuid("vehicle-1").orElseThrow();
            assertEquals(OwnershipMode.INSTALLATION, updated.getMode());
            assertEquals("port-1", updated.getInstallationId());
            assertEquals(playerUuid, updated.getPlayerUuid());
            assertEquals("player_Alice", ownerData.getOwner());
            verify(plugin).saveVehicleRegistry();
        }
    }

    private static PlayerVehicleRecord personalRecord(String vehicleUuid, String typeId) {
        return new PlayerVehicleRecord(
            UUID.randomUUID(),
            vehicleUuid,
            typeId,
            OwnershipMode.PERSONAL,
            null);
    }

    private static VehicleBerthTarget berthTarget(String uuid, Location location) {
        return berthTarget(uuid, location, new OwnerData());
    }

    private static VehicleBerthTarget berthTarget(String uuid, Location location, OwnerData ownerData) {
        return new VehicleBerthTarget() {
            @Override
            public String getVehicleUuid() {
                return uuid;
            }

            @Override
            public Location getLocation() {
                return location;
            }

            @Override
            public OwnerData getOwnerData() {
                return ownerData;
            }
        };
    }

    private static Location locationAt(int x, int y, int z) {
        World world = mock(World.class);
        return new Location(world, x, y, z);
    }

    private Path writeInstallationsFixture() throws IOException {
        Path installationsYaml = tempDir.resolve("installations.yml");
        Files.writeString(installationsYaml, """
            consent-proximity-blocks: 20
            transfer-request-timeout-seconds: 60

            fort:
              radius: 80
              daily-upkeep: 50
              construction-time: 10
              slots:
                static_emplacements: 8
                land_vehicles: 2
            port:
              radius: 80
              daily-upkeep: 20
              construction-time: 10
              slots:
                ships: 8
            airport:
              radius: 80
              daily-upkeep: 35
              construction-time: 10
              slots:
                aircraft: 10
            """);
        return installationsYaml;
    }

    private void writeVehiclesFixture() throws IOException {
        Path vehiclesYaml = tempDir.resolve("vehicles.yml");
        Files.writeString(vehiclesYaml, """
            personal-slot-limit: 1

            categories:
              ships:
                ironclad:
                  upkeep: 20
                  size: 1
              land_vehicles:
                horse_cart:
                  upkeep: 5
                  size: 1
              static_emplacements: {}
              aircraft: {}
            """);
        VehiclesConfigLoader.load(vehiclesYaml.toFile());
    }
}
