package me.Plugins.SimpleFactions.vehicles;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.Plugins.SimpleFactions.Loaders.VehiclesConfigLoader;
import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.player.PlayerEconomyManager;
import me.Plugins.SimpleFactions.player.income.PlayerCashflow;
import net.tfminecraft.VehicleFramework.Data.OwnedVehicleSummary;

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
        for (OwnedVehicleSummary vehicle : VehicleOwnershipQueries.allPersonalVehicles(registry)) {
            double upkeep = VehiclesConfigLoader.getUpkeep(vehicle.getTypeId());
            if (upkeep <= 0.0) {
                continue;
            }
            String playerName = VehicleOwnershipQueries.playerNameFromOwner(vehicle.getOwner());
            UUID playerUuid = VehicleOwnershipQueries.resolvePlayerUuid(playerName);
            if (playerUuid == null) {
                continue;
            }
            chargePlayer(playerUuid, upkeep, vehicle.getTypeId());
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
