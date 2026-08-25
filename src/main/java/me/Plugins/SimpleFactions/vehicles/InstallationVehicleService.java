package me.Plugins.SimpleFactions.vehicles;

import java.util.Optional;

import org.bukkit.Location;

import me.Plugins.SimpleFactions.Loaders.InstallationConfigLoader;
import me.Plugins.SimpleFactions.Loaders.VehiclesConfigLoader;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.installation.Installation;
import me.Plugins.SimpleFactions.installation.InstallationBounds;
import net.tfminecraft.VehicleFramework.Data.OwnerData;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;

public final class InstallationVehicleService {
    private final PlayerVehicleRegistry registry;
    private final InstallationVehicleOwnerSync ownerSync;

    public InstallationVehicleService(
            PlayerVehicleRegistry registry,
            InstallationVehicleOwnerSync ownerSync) {
        this.registry = registry;
        this.ownerSync = ownerSync;
    }

    public CanRegisterResult canRegister(
            Installation installation,
            ActiveVehicle vehicle,
            PlayerVehicleRecord record) {
        return canRegister(installation, adapt(vehicle), record);
    }

    CanRegisterResult canRegister(
            Installation installation,
            VehicleBerthTarget vehicle,
            PlayerVehicleRecord record) {
        if (record == null || vehicle == null
                || !record.getVehicleUuid().equals(vehicle.getVehicleUuid())) {
            return CanRegisterResult.NOT_IN_REGISTRY;
        }

        if (record.getMode() == OwnershipMode.INSTALLATION) {
            return CanRegisterResult.ALREADY_BERTHED;
        }

        String vehicleTypeId = record.getVehicleTypeId();
        Optional<String> categoryId = VehiclesConfigLoader.getCategoryId(vehicleTypeId);
        if (categoryId.isEmpty()) {
            return CanRegisterResult.UNKNOWN_TYPE;
        }

        int capacity = InstallationConfigLoader.getCategorySlotCapacity(
                installation.getKind(),
                categoryId.get());
        if (capacity <= 0) {
            return CanRegisterResult.UNSUPPORTED_CATEGORY;
        }

        int vehicleSize = VehiclesConfigLoader.getSize(vehicleTypeId);
        int used = registry.usedCategorySize(installation.getId(), categoryId.get());
        if (used + vehicleSize > capacity) {
            return CanRegisterResult.NO_CAPACITY;
        }

        Location vehicleLocation = vehicle.getLocation();
        if (!InstallationBounds.isWithinRadius(installation, vehicleLocation)) {
            return CanRegisterResult.OUT_OF_RADIUS;
        }

        if (!InstallationBounds.isCorrectProvince(installation, vehicleLocation)) {
            return CanRegisterResult.WRONG_PROVINCE;
        }

        return CanRegisterResult.OK;
    }

    public void register(
            Installation installation,
            ActiveVehicle vehicle,
            PlayerVehicleRecord record,
            Faction faction) {
        register(installation, adapt(vehicle), record, faction);
    }

    void register(
            Installation installation,
            VehicleBerthTarget vehicle,
            PlayerVehicleRecord record,
            Faction faction) {
        registry.register(new PlayerVehicleRecord(
                record.getPlayerUuid(),
                record.getVehicleUuid(),
                record.getVehicleTypeId(),
                OwnershipMode.INSTALLATION,
                installation.getId()));
        ownerSync.applyLeaderOwner(vehicle.getOwnerData(), faction);
        SimpleFactions.getInstance().saveVehicleRegistry();
    }

    private static VehicleBerthTarget adapt(ActiveVehicle vehicle) {
        if (vehicle == null) {
            return null;
        }
        return new VehicleBerthTarget() {
            @Override
            public String getVehicleUuid() {
                return vehicle.getUUID();
            }

            @Override
            public Location getLocation() {
                return vehicle.getLocation();
            }

            @Override
            public OwnerData getOwnerData() {
                return vehicle.getOwnerData();
            }
        };
    }

    interface VehicleBerthTarget {
        String getVehicleUuid();

        Location getLocation();

        OwnerData getOwnerData();
    }
}
