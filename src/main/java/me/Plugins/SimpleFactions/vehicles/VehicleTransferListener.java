package me.Plugins.SimpleFactions.vehicles;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import me.Plugins.SimpleFactions.Loaders.InstallationConfigLoader;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.installation.Installation;
import me.Plugins.SimpleFactions.installation.InstallationBounds;
import net.tfminecraft.VehicleFramework.Events.VehiclePreInteractEvent;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;

public final class VehicleTransferListener implements Listener {
    private final VehicleTransferSessionManager sessionManager;
    private final PlayerVehicleRegistry registry;
    private final InstallationVehicleService installationVehicleService;
    private final VehicleTransferConsentService consentService;

    public VehicleTransferListener(
            VehicleTransferSessionManager sessionManager,
            PlayerVehicleRegistry registry,
            InstallationVehicleService installationVehicleService,
            VehicleTransferConsentService consentService) {
        this.sessionManager = sessionManager;
        this.registry = registry;
        this.installationVehicleService = installationVehicleService;
        this.consentService = consentService;
    }

    @EventHandler
    public void onVehiclePreInteract(VehiclePreInteractEvent event) {
        Player leader = event.getPlayer();
        if (leader == null) {
            return;
        }

        VehicleTransferSession session = sessionManager.get(leader.getUniqueId());
        if (session == null) {
            return;
        }

        Faction faction = FactionManager.getByLeader(leader.getName());
        if (faction == null) {
            sessionManager.clear(leader.getUniqueId());
            leader.sendMessage(VehicleTransferMessages.notLeader());
            event.setCancelled(true);
            return;
        }

        Installation installation = faction.getInstallationHandler().getById(session.getInstallationId());
        if (installation == null) {
            sessionManager.clear(leader.getUniqueId());
            leader.sendMessage(VehicleTransferMessages.unknownInstallation());
            event.setCancelled(true);
            return;
        }

        ActiveVehicle vehicle = event.getVehicle();
        if (vehicle == null || vehicle.getUUID() == null) {
            return;
        }

        PlayerVehicleRecord record = registry.getByVehicleUuid(vehicle.getUUID()).orElse(null);
        if (record != null && !record.getPlayerUuid().equals(leader.getUniqueId())) {
            handleOtherOwnerTransfer(leader, faction, installation, vehicle, record, event);
            return;
        }

        CanRegisterResult result = installationVehicleService.canRegister(
                installation,
                vehicle,
                record);
        if (result != CanRegisterResult.OK) {
            String message = VehicleTransferMessages.forResult(result, installation, vehicle, record);
            if (message != null) {
                leader.sendMessage(message);
            }
            event.setCancelled(true);
            return;
        }

        installationVehicleService.register(installation, vehicle, record, faction);
        sessionManager.clear(leader.getUniqueId());
        leader.sendMessage(VehicleTransferMessages.berthSuccess(installation));
        event.setCancelled(true);
    }

    private void handleOtherOwnerTransfer(
            Player leader,
            Faction faction,
            Installation installation,
            ActiveVehicle vehicle,
            PlayerVehicleRecord record,
            VehiclePreInteractEvent event) {
        Player owner = Bukkit.getPlayer(record.getPlayerUuid());
        if (owner == null || !owner.isOnline()) {
            leader.sendMessage(VehicleTransferMessages.ownerOffline());
            event.setCancelled(true);
            return;
        }

        int proximityBlocks = InstallationConfigLoader.getConsentProximityBlocks();
        double distance = InstallationBounds.horizontalDistanceBlocks(
                owner.getLocation().getBlockX(),
                owner.getLocation().getBlockZ(),
                vehicle.getLocation());
        if (distance > proximityBlocks) {
            leader.sendMessage(VehicleTransferMessages.ownerTooFar(proximityBlocks));
            event.setCancelled(true);
            return;
        }

        CanRegisterResult result = installationVehicleService.canRegister(
                installation,
                vehicle,
                record);
        if (result != CanRegisterResult.OK) {
            String message = VehicleTransferMessages.forResult(result, installation, vehicle, record);
            if (message != null) {
                leader.sendMessage(message);
            }
            event.setCancelled(true);
            return;
        }

        consentService.sendConsentRequest(leader, owner, faction, installation, vehicle, record);
        sessionManager.clear(leader.getUniqueId());
        event.setCancelled(true);
    }
}
