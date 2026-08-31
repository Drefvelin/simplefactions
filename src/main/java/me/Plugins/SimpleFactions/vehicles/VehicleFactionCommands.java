package me.Plugins.SimpleFactions.vehicles;

import org.bukkit.entity.Player;

import me.Plugins.SimpleFactions.Loaders.InstallationConfigLoader;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.Utils.Permissions;
import me.Plugins.SimpleFactions.installation.Installation;

public final class VehicleFactionCommands {
    private VehicleFactionCommands() {}

    public static void armTransfer(Player player, String installationId) {
        Faction faction = FactionManager.getByLeader(player.getName());
        if (faction == null) {
            player.sendMessage(VehicleTransferMessages.notLeader());
            return;
        }
        if (installationId == null || installationId.isBlank()) {
            player.sendMessage(VehicleMaintenanceMessages.transferUsage());
            return;
        }
        Installation installation = faction.getInstallationHandler().getById(installationId);
        if (installation == null) {
            player.sendMessage(VehicleTransferMessages.unknownInstallation());
            return;
        }
        if (!Permissions.isAdmin(player)
                && VehicleInstallationLockService.isVehicleLocked(
                        installation.getId(), java.time.Instant.now())) {
            player.sendMessage(VehicleInstallationLockService.BERTH_BLOCKED);
            return;
        }
        SimpleFactions plugin = SimpleFactions.getInstance();
        long timeoutMillis = InstallationConfigLoader.getTransferRequestTimeoutSeconds() * 1000L;
        plugin.getVehicleMaintenancePaySessionManager().clear(player.getUniqueId());
        plugin.getVehicleTransferSessionManager().put(
                player.getUniqueId(),
                new VehicleTransferSession(
                        installation.getId(),
                        System.currentTimeMillis() + timeoutMillis));
        player.sendMessage(VehicleTransferMessages.commandArmed(installation));
    }

    public static void armMaintenancePay(Player player) {
        Faction faction = FactionManager.getByLeader(player.getName());
        if (faction == null) {
            player.sendMessage(VehicleMaintenanceMessages.notLeader());
            return;
        }
        SimpleFactions plugin = SimpleFactions.getInstance();
        long timeoutMillis = InstallationConfigLoader.getTransferRequestTimeoutSeconds() * 1000L;
        plugin.getVehicleTransferSessionManager().clear(player.getUniqueId());
        plugin.getVehicleMaintenancePaySessionManager().put(
                player.getUniqueId(),
                new VehicleMaintenancePaySession(System.currentTimeMillis() + timeoutMillis));
        player.sendMessage(VehicleMaintenanceMessages.payArmed());
    }
}
