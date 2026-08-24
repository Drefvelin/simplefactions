package me.Plugins.SimpleFactions.Loaders;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public final class VehiclesConfigLoader {
    private static int personalSlotLimit = 1;
    private static Map<String, Double> upkeepByVehicleId = new HashMap<>();

    private VehiclesConfigLoader() {}

    public static void load(File vehiclesYaml) {
        FileConfiguration config = new YamlConfiguration();
        try {
            config.load(vehiclesYaml);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
            fail("Failed to load vehicles.yml");
        }

        personalSlotLimit = config.getInt("personal-slot-limit", 1);
        if (personalSlotLimit < 0) {
            fail("vehicles.yml personal-slot-limit must be >= 0");
        }

        Map<String, Double> upkeep = new HashMap<>();
        ConfigurationSection upkeepSection = config.getConfigurationSection("upkeep");
        if (upkeepSection != null) {
            for (String key : upkeepSection.getKeys(false)) {
                double value = upkeepSection.getDouble(key);
                if (value < 0) {
                    fail("vehicles.yml upkeep." + key + " must be >= 0");
                }
                upkeep.put(key.toLowerCase(), value);
            }
        }
        upkeepByVehicleId = upkeep;
    }

    public static int getPersonalSlotLimit() {
        return personalSlotLimit;
    }

    public static double getUpkeep(String vehicleTypeId) {
        if (vehicleTypeId == null || vehicleTypeId.isEmpty()) {
            return 0.0;
        }
        return upkeepByVehicleId.getOrDefault(vehicleTypeId.toLowerCase(), 0.0);
    }

    public static Map<String, Double> getAllUpkeep() {
        return Collections.unmodifiableMap(upkeepByVehicleId);
    }

    private static void fail(String message) {
        Bukkit.getLogger().severe("[SimpleFactions] " + message);
        throw new IllegalStateException(message);
    }
}
