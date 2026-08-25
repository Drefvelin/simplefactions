package me.Plugins.SimpleFactions.vehicles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import me.Plugins.SimpleFactions.Loaders.VehiclesConfigLoader;

public final class PlayerVehicleRegistry {
    private final Map<String, PlayerVehicleRecord> byVehicleUuid = new HashMap<>();

    public int countPersonal(UUID playerUuid) {
        if (playerUuid == null) {
            return 0;
        }
        int count = 0;
        for (PlayerVehicleRecord record : byVehicleUuid.values()) {
            if (record.getMode() == OwnershipMode.PERSONAL
                    && record.getPlayerUuid().equals(playerUuid)) {
                count++;
            }
        }
        return count;
    }

    public int countPersonalOfType(UUID playerUuid, String vehicleTypeId) {
        if (playerUuid == null || vehicleTypeId == null || vehicleTypeId.isEmpty()) {
            return 0;
        }
        String normalizedTypeId = vehicleTypeId.toLowerCase();
        int count = 0;
        for (PlayerVehicleRecord record : byVehicleUuid.values()) {
            if (record.getMode() == OwnershipMode.PERSONAL
                    && record.getPlayerUuid().equals(playerUuid)
                    && normalizedTypeId.equalsIgnoreCase(record.getVehicleTypeId())) {
                count++;
            }
        }
        return count;
    }

    public int countPersonalExcludingIgnoreLimit(UUID playerUuid) {
        if (playerUuid == null) {
            return 0;
        }
        int count = 0;
        for (PlayerVehicleRecord record : byVehicleUuid.values()) {
            if (record.getMode() == OwnershipMode.PERSONAL
                    && record.getPlayerUuid().equals(playerUuid)
                    && !VehiclesConfigLoader.ignoresPersonalSlotLimit(record.getVehicleTypeId())) {
                count++;
            }
        }
        return count;
    }

    public void register(PlayerVehicleRecord record) {
        if (record == null || record.getVehicleUuid() == null) {
            return;
        }
        byVehicleUuid.put(record.getVehicleUuid(), record);
    }

    public boolean unregister(String vehicleUuid) {
        if (vehicleUuid == null) {
            return false;
        }
        return byVehicleUuid.remove(vehicleUuid) != null;
    }

    public Optional<PlayerVehicleRecord> getByVehicleUuid(String vehicleUuid) {
        if (vehicleUuid == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byVehicleUuid.get(vehicleUuid));
    }

    public List<PlayerVehicleRecord> getPersonalVehicles(UUID playerUuid) {
        List<PlayerVehicleRecord> out = new ArrayList<>();
        if (playerUuid == null) {
            return out;
        }
        for (PlayerVehicleRecord record : byVehicleUuid.values()) {
            if (record.getMode() == OwnershipMode.PERSONAL
                    && record.getPlayerUuid().equals(playerUuid)) {
                out.add(record);
            }
        }
        return out;
    }

    public List<PlayerVehicleRecord> getAll() {
        return new ArrayList<>(byVehicleUuid.values());
    }

    public List<PlayerVehicleRecord> getByInstallationId(String installationId) {
        List<PlayerVehicleRecord> out = new ArrayList<>();
        if (installationId == null) {
            return out;
        }
        for (PlayerVehicleRecord record : byVehicleUuid.values()) {
            if (record.getMode() == OwnershipMode.INSTALLATION
                    && installationId.equals(record.getInstallationId())) {
                out.add(record);
            }
        }
        return out;
    }

    public int usedCategorySize(String installationId, String categoryId) {
        if (installationId == null || categoryId == null || categoryId.isEmpty()) {
            return 0;
        }
        String normalizedCategoryId = categoryId.toLowerCase();
        int used = 0;
        for (PlayerVehicleRecord record : getByInstallationId(installationId)) {
            Optional<String> recordCategory =
                    VehiclesConfigLoader.getCategoryId(record.getVehicleTypeId());
            if (recordCategory.isPresent()
                    && recordCategory.get().equalsIgnoreCase(normalizedCategoryId)) {
                used += VehiclesConfigLoader.getSize(record.getVehicleTypeId());
            }
        }
        return used;
    }

    void replaceAll(List<PlayerVehicleRecord> records) {
        byVehicleUuid.clear();
        if (records == null) {
            return;
        }
        for (PlayerVehicleRecord record : records) {
            register(record);
        }
    }
}
