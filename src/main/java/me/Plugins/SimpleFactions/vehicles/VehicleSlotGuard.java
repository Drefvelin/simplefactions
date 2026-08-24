package me.Plugins.SimpleFactions.vehicles;

import java.util.UUID;

import me.Plugins.SimpleFactions.Loaders.VehiclesConfigLoader;

public final class VehicleSlotGuard {
    private VehicleSlotGuard() {}

    public static boolean isPersonalSlotAvailable(UUID playerUuid, PlayerVehicleRegistry registry) {
        if (playerUuid == null || registry == null) {
            return false;
        }
        int limit = VehiclesConfigLoader.getPersonalSlotLimit();
        if (limit <= 0) {
            return true;
        }
        return registry.countPersonal(playerUuid) < limit;
    }
}
