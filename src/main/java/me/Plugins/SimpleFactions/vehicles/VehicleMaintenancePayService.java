package me.Plugins.SimpleFactions.vehicles;

import java.util.UUID;

import me.Plugins.SimpleFactions.Loaders.VehiclesConfigLoader;

public final class VehicleMaintenancePayService {
    private final VehicleMaintenanceStore store;
    private final PlayerPouch playerPouch;

    public VehicleMaintenancePayService(VehicleMaintenanceStore store, PlayerPouch playerPouch) {
        this.store = store;
        this.playerPouch = playerPouch;
    }

    public double payAmount(String vehicleTypeId) {
        return VehiclesConfigLoader.getUpkeep(vehicleTypeId);
    }

    public VehicleMaintenancePayResult tryPay(UUID playerUuid, String vehicleUuid, String vehicleTypeId) {
        if (vehicleUuid == null || !store.isUnpaid(vehicleUuid)) {
            return VehicleMaintenancePayResult.NOT_UNPAID;
        }
        if (!VehiclesConfigLoader.isKnownType(vehicleTypeId)) {
            return VehicleMaintenancePayResult.UNKNOWN_TYPE;
        }
        double amount = payAmount(vehicleTypeId);
        if (amount > 0.0 && !playerPouch.withdrawFromPouch(playerUuid, amount)) {
            return VehicleMaintenancePayResult.INSUFFICIENT_POUCH;
        }
        store.clearUnpaid(vehicleUuid);
        return VehicleMaintenancePayResult.SUCCESS;
    }
}
