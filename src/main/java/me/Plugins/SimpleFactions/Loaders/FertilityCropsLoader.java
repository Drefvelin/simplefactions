package me.Plugins.SimpleFactions.Loaders;

import java.io.File;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import me.Plugins.SimpleFactions.Map.fertility.FertilityCropRegistry;

public final class FertilityCropsLoader {
    private static boolean enabled;
    private static FertilityCropRegistry registry = FertilityCropRegistry.EMPTY;

    private FertilityCropsLoader() {}

    public static void load(File fertilityCropsYaml) {
        FileConfiguration config = new YamlConfiguration();
        try {
            config.load(fertilityCropsYaml);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
            fail("Failed to load fertility-crops.yml");
        }

        enabled = config.getBoolean("enabled", true);
        if (!enabled) {
            registry = FertilityCropRegistry.EMPTY;
            return;
        }

        FertilityCropRegistry.Builder builder = FertilityCropRegistry.builder();
        loadVanillaSection(config.getConfigurationSection("vanilla"), builder);
        loadCustomSection(config.getConfigurationSection("customcrops"), builder);
        registry = builder.build();
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static FertilityCropRegistry getRegistry() {
        return registry;
    }

    public static void resetForTests() {
        enabled = false;
        registry = FertilityCropRegistry.EMPTY;
    }

    private static void loadVanillaSection(ConfigurationSection section, FertilityCropRegistry.Builder builder) {
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            Material material = Material.matchMaterial(key);
            if (material == null) {
                try {
                    material = Material.valueOf(key.toUpperCase());
                } catch (IllegalArgumentException ignored) {
                    material = null;
                }
            }
            if (material == null) {
                fail("fertility-crops.yml vanilla." + key + " is not a valid Material");
            }
            try {
                builder.vanilla(material, section.getDouble(key));
            } catch (IllegalArgumentException e) {
                fail("fertility-crops.yml vanilla." + key + ": " + e.getMessage());
            }
        }
    }

    private static void loadCustomSection(ConfigurationSection section, FertilityCropRegistry.Builder builder) {
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            double weight = section.getDouble(key);
            try {
                builder.custom(key, weight);
            } catch (IllegalArgumentException e) {
                fail("fertility-crops.yml customcrops." + key + ": " + e.getMessage());
            }
        }
    }

    private static void fail(String message) {
        if (Bukkit.getServer() != null) {
            Bukkit.getLogger().severe("[SimpleFactions] " + message);
        }
        throw new IllegalStateException(message);
    }
}
