package me.Plugins.SimpleFactions.Loaders;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import me.Plugins.SimpleFactions.installation.InstallationKind;
import me.Plugins.SimpleFactions.installation.InstallationKindConfig;

public final class InstallationConfigLoader {
    private static final Map<InstallationKind, InstallationKindConfig> byKind =
            new EnumMap<>(InstallationKind.class);

    private InstallationConfigLoader() {}

    public static void load(ConfigurationSection installations) {
        byKind.clear();
        if (installations == null) {
            throw new IllegalStateException("config.yml missing installations section");
        }

        for (InstallationKind kind : InstallationKind.values()) {
            String key = kind.getCommandName();
            ConfigurationSection section = installations.getConfigurationSection(key);
            if (section == null) {
                fail("installations." + key + " section is required");
            }

            if (!section.contains("daily-upkeep")) {
                fail("installations." + key + ".daily-upkeep is required");
            }
            if (!section.contains("construction-time")) {
                fail("installations." + key + ".construction-time is required");
            }

            double dailyUpkeep = section.getDouble("daily-upkeep");
            int constructionTimeSeconds = section.getInt("construction-time");

            if (dailyUpkeep < 0) {
                fail("installations." + key + ".daily-upkeep must be >= 0");
            }
            if (constructionTimeSeconds <= 0) {
                fail("installations." + key + ".construction-time must be > 0");
            }

            byKind.put(kind, new InstallationKindConfig(dailyUpkeep, constructionTimeSeconds));
        }
    }

    public static double getDailyUpkeep(InstallationKind kind) {
        return require(kind).getDailyUpkeep();
    }

    public static int getConstructionTimeSeconds(InstallationKind kind) {
        return require(kind).getConstructionTimeSeconds();
    }

    public static Map<InstallationKind, InstallationKindConfig> getAll() {
        return Collections.unmodifiableMap(byKind);
    }

    private static InstallationKindConfig require(InstallationKind kind) {
        InstallationKindConfig config = byKind.get(kind);
        if (config == null) {
            throw new IllegalStateException(
                    "Installation config not loaded for kind " + kind.getCommandName());
        }
        return config;
    }

    private static void fail(String message) {
        Bukkit.getLogger().severe("[SimpleFactions] " + message);
        throw new IllegalStateException(message);
    }
}
