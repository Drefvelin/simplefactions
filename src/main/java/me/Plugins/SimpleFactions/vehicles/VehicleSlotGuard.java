package me.Plugins.SimpleFactions.vehicles;

import java.util.UUID;

import me.Plugins.SimpleFactions.Loaders.VehiclesConfigLoader;

public final class VehicleSlotGuard {
    private VehicleSlotGuard() {}

    public static CanBuildResult checkCanBuild(
            UUID playerUuid, String vehicleTypeId, PlayerVehicleRegistry registry) {
        if (playerUuid == null || registry == null) {
            return CanBuildResult.UNKNOWN_TYPE;
        }
        if (vehicleTypeId == null || vehicleTypeId.isEmpty()) {
            return CanBuildResult.UNKNOWN_TYPE;
        }
        if (!VehiclesConfigLoader.isKnownType(vehicleTypeId)) {
            return CanBuildResult.UNKNOWN_TYPE;
        }

        if (registry.countPersonalOfType(playerUuid, vehicleTypeId)
                >= VehiclesConfigLoader.getPerPersonLimit(vehicleTypeId)) {
            return CanBuildResult.PER_TYPE_LIMIT;
        }

        if (VehiclesConfigLoader.ignoresPersonalSlotLimit(vehicleTypeId)) {
            return CanBuildResult.OK;
        }

        int totalLimit = VehiclesConfigLoader.getPersonalSlotLimit();
        if (totalLimit <= 0) {
            return CanBuildResult.OK;
        }

        if (registry.countPersonalExcludingIgnoreLimit(playerUuid) >= totalLimit) {
            return CanBuildResult.TOTAL_LIMIT;
        }

        return CanBuildResult.OK;
    }
}
