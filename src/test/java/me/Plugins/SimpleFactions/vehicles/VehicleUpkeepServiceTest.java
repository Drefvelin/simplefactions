package me.Plugins.SimpleFactions.vehicles;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import me.Plugins.SimpleFactions.Loaders.VehiclesConfigLoader;
import me.Plugins.SimpleFactions.player.PlayerEconomyManager;
import me.Plugins.SimpleFactions.player.income.PlayerCashflow;

class VehicleUpkeepServiceTest {
    private Path tempDir;
    private PlayerVehicleRegistry registry;
    private PlayerEconomyManager economyManager;
    private TestPlayerBank bank;
    private VehicleUpkeepService service;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("sf-vehicle-upkeep-");
        Path vehiclesYaml = tempDir.resolve("vehicles.yml");
        Files.writeString(vehiclesYaml, """
            personal-slot-limit: 1

            categories:
              ships:
                ironclad:
                  upkeep: 20
                  size: 1
              static_emplacements: {}
              aircraft: {}
            """);
        VehiclesConfigLoader.load(vehiclesYaml.toFile());

        registry = new PlayerVehicleRegistry();
        economyManager = new PlayerEconomyManager();
        bank = new TestPlayerBank();
        service = new VehicleUpkeepService(registry, economyManager, bank);
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
    void successfulUpkeepWithdrawsBankAndRecordsLedger() {
        UUID playerUuid = UUID.randomUUID();
        bank.setBalance(playerUuid, 100.0);
        registry.register(new PlayerVehicleRecord(
            playerUuid,
            "vehicle-1",
            "ironclad",
            OwnershipMode.PERSONAL,
            null
        ));

        service.processDailyUpkeep();

        assertEquals(80.0, bank.getBankBalance(playerUuid));
        assertEquals(-20.0, economyManager.getLedger(playerUuid).getAmount(PlayerCashflow.VEHICLE_UPKEEP));
    }

    @Test
    void insufficientBalanceSkipsCharge() {
        UUID playerUuid = UUID.randomUUID();
        bank.setBalance(playerUuid, 10.0);
        registry.register(new PlayerVehicleRecord(
            playerUuid,
            "vehicle-1",
            "ironclad",
            OwnershipMode.PERSONAL,
            null
        ));

        service.processDailyUpkeep();

        assertEquals(10.0, bank.getBankBalance(playerUuid));
        assertEquals(0.0, economyManager.getLedger(playerUuid).getAmount(PlayerCashflow.VEHICLE_UPKEEP));
    }

    @Test
    void skipsNonPersonalVehicles() {
        UUID playerUuid = UUID.randomUUID();
        bank.setBalance(playerUuid, 100.0);
        registry.register(new PlayerVehicleRecord(
            playerUuid,
            "vehicle-1",
            "ironclad",
            OwnershipMode.INSTALLATION,
            "installation-1"
        ));

        service.processDailyUpkeep();

        assertEquals(100.0, bank.getBankBalance(playerUuid));
        assertEquals(0.0, economyManager.getLedger(playerUuid).getNetDaily());
    }

    private static final class TestPlayerBank implements PlayerBank {
        private final Map<UUID, Double> balances = new HashMap<>();

        void setBalance(UUID playerUuid, double balance) {
            balances.put(playerUuid, balance);
        }

        @Override
        public double getBankBalance(UUID playerUuid) {
            return balances.getOrDefault(playerUuid, 0.0);
        }

        @Override
        public boolean withdrawFromBank(UUID playerUuid, double amount) {
            double balance = getBankBalance(playerUuid);
            if (balance < amount) {
                return false;
            }
            balances.put(playerUuid, balance - amount);
            return true;
        }
    }
}
