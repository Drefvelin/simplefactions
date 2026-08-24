package me.Plugins.SimpleFactions.vehicles;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.Plugins.SimpleFactions.Loaders.VehiclesConfigLoader;
import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.player.PlayerEconomyManager;
import me.Plugins.SimpleFactions.player.income.PlayerCashflow;

public final class VehicleUpkeepService {
    private final PlayerVehicleRegistry registry;
    private final PlayerEconomyManager economyManager;
    private final PlayerBank playerBank;

    public VehicleUpkeepService(
            PlayerVehicleRegistry registry,
            PlayerEconomyManager economyManager) {
        this(registry, economyManager, DenarEconomyPlayerBank.INSTANCE);
    }

    public VehicleUpkeepService(
            PlayerVehicleRegistry registry,
            PlayerEconomyManager economyManager,
            PlayerBank playerBank) {
        this.registry = registry;
        this.economyManager = economyManager;
        this.playerBank = playerBank;
    }

    public void processDailyUpkeep() {
        for (PlayerVehicleRecord record : registry.getAll()) {
            if (record.getMode() != OwnershipMode.PERSONAL) {
                continue;
            }
            double upkeep = VehiclesConfigLoader.getUpkeep(record.getVehicleTypeId());
            if (upkeep <= 0.0) {
                continue;
            }
            chargePlayer(record.getPlayerUuid(), upkeep, record.getVehicleTypeId());
        }
    }

    private void chargePlayer(UUID playerUuid, double upkeep, String vehicleTypeId) {
        if (playerUuid == null || upkeep <= 0.0) {
            return;
        }
        if (!playerBank.withdrawFromBank(playerUuid, upkeep)) {
            SimpleFactions plugin = SimpleFactions.getInstance();
            if (plugin != null) {
                plugin.getLogger().info(
                    "Vehicle upkeep unpaid for player "
                    + playerUuid
                    + " vehicle "
                    + vehicleTypeId
                    + " amount "
                    + upkeep
                );
            }
            Player online = null;
            if (Bukkit.getServer() != null) {
                online = Bukkit.getPlayer(playerUuid);
            }
            if (online != null && online.isOnline()) {
                online.sendMessage(
                    "§cCould not pay vehicle upkeep ("
                    + vehicleTypeId
                    + "): insufficient bank balance."
                );
            }
            return;
        }
        economyManager.getLedger(playerUuid).add(PlayerCashflow.VEHICLE_UPKEEP, -upkeep);
    }
}
