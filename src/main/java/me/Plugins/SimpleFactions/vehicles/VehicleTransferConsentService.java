package me.Plugins.SimpleFactions.vehicles;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.RequestManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Objects.Request.VehicleTransferConsentRequest;
import me.Plugins.SimpleFactions.installation.Installation;
import net.tfminecraft.VehicleFramework.VehicleFramework;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;

public final class VehicleTransferConsentService {
    private final InstallationVehicleService installationVehicleService;
    private final PlayerVehicleRegistry registry;
    private final VehicleTransferSessionManager sessionManager;

    public VehicleTransferConsentService(
            InstallationVehicleService installationVehicleService,
            PlayerVehicleRegistry registry,
            VehicleTransferSessionManager sessionManager) {
        this.installationVehicleService = installationVehicleService;
        this.registry = registry;
        this.sessionManager = sessionManager;
    }

    public void sendConsentRequest(
            Player leader,
            Player owner,
            Faction faction,
            Installation installation,
            ActiveVehicle vehicle,
            PlayerVehicleRecord record) {
        if (leader == null || owner == null || faction == null
                || installation == null || vehicle == null || record == null) {
            return;
        }

        RequestManager.addRequest(
                leader,
                owner,
                new VehicleTransferConsentRequest(
                        faction.getOrCreateMainGuild(),
                        installation.getId(),
                        installation.getName(),
                        vehicle.getUUID(),
                        record.getVehicleTypeId(),
                        owner.getUniqueId(),
                        leader.getUniqueId()));

        owner.sendMessage(VehicleTransferMessages.consentPrompt(
                leader.getName(),
                record.getVehicleTypeId(),
                installation.getName()));
        leader.sendMessage(VehicleTransferMessages.consentSent(owner.getName()));
    }

    public void acceptRequest(Player owner) {
        if (!(RequestManager.getRequest(owner) instanceof VehicleTransferConsentRequest req)) {
            return;
        }

        if (!owner.getUniqueId().equals(req.getOwnerUuid())) {
            owner.sendMessage("§cYou cannot accept this request.");
            return;
        }

        Faction faction = resolveProposerFaction(req);
        if (faction == null) {
            owner.sendMessage(VehicleTransferMessages.consentExpired());
            return;
        }

        Installation installation = faction.getInstallationHandler().getById(req.getInstallationId());
        if (installation == null) {
            owner.sendMessage(VehicleTransferMessages.unknownInstallation());
            return;
        }

        InstallationVehicleService.VehicleBerthTarget vehicle = resolveBerthTarget(req.getVehicleUuid());
        PlayerVehicleRecord record = registry.getByVehicleUuid(req.getVehicleUuid()).orElse(null);
        ActiveVehicle activeVehicle = resolveVehicle(req.getVehicleUuid());

        CanRegisterResult result = installationVehicleService.canRegister(
                installation,
                vehicle,
                record);
        if (result != CanRegisterResult.OK) {
            String message = VehicleTransferMessages.forResult(
                    result,
                    installation,
                    activeVehicle,
                    record);
            if (message != null) {
                owner.sendMessage(message);
            }
            notifyProposer(req, message);
            return;
        }

        installationVehicleService.register(installation, vehicle, record, faction);
        sessionManager.clear(req.getProposerLeaderUuid());

        String success = VehicleTransferMessages.berthSuccess(installation);
        owner.sendMessage(success);
        notifyProposer(req, success);
    }

    public void notifyExpired(VehicleTransferConsentRequest req, Player owner) {
        if (req == null) {
            return;
        }
        String message = VehicleTransferMessages.consentExpired();
        if (owner != null && owner.isOnline()) {
            owner.sendMessage(message);
        }
        notifyProposer(req, message);
    }

    InstallationVehicleService.VehicleBerthTarget resolveBerthTarget(String vehicleUuid) {
        ActiveVehicle vehicle = resolveVehicle(vehicleUuid);
        if (vehicle == null) {
            return null;
        }
        return new InstallationVehicleService.VehicleBerthTarget() {
            @Override
            public String getVehicleUuid() {
                return vehicle.getUUID();
            }

            @Override
            public org.bukkit.Location getLocation() {
                return vehicle.getLocation();
            }

            @Override
            public net.tfminecraft.VehicleFramework.Data.OwnerData getOwnerData() {
                return vehicle.getOwnerData();
            }
        };
    }

    ActiveVehicle resolveVehicle(String vehicleUuid) {
        if (vehicleUuid == null) {
            return null;
        }
        return VehicleFramework.getVehicleManager().get(vehicleUuid);
    }

    private static Faction resolveProposerFaction(VehicleTransferConsentRequest req) {
        Player proposer = Bukkit.getPlayer(req.getProposerLeaderUuid());
        if (proposer != null) {
            Faction faction = FactionManager.getByLeader(proposer.getName());
            if (faction != null) {
                return faction;
            }
        }
        for (Faction faction : FactionManager.factions) {
            if (faction.getLeader() != null) {
                Player leader = Bukkit.getPlayerExact(faction.getLeader());
                if (leader != null && leader.getUniqueId().equals(req.getProposerLeaderUuid())) {
                    return faction;
                }
            }
        }
        return null;
    }

    private static void notifyProposer(VehicleTransferConsentRequest req, String message) {
        if (message == null) {
            return;
        }
        Player proposer = Bukkit.getPlayer(req.getProposerLeaderUuid());
        if (proposer != null && proposer.isOnline()) {
            proposer.sendMessage(message);
        }
    }
}
