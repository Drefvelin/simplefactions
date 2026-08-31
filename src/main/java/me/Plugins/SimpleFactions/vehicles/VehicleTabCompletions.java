package me.Plugins.SimpleFactions.vehicles;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class VehicleTabCompletions {
    private VehicleTabCompletions() {}

    public static List<String> subcommands(String prefix) {
        return filter(List.of("transfer", "maintenance"), prefix);
    }

    public static List<String> maintenanceActions(String prefix) {
        return filter(List.of("pay"), prefix);
    }

    public static List<String> filter(List<String> options, String prefix) {
        List<String> completions = new ArrayList<>(options);
        if (prefix == null || prefix.isEmpty()) {
            return completions;
        }
        String normalized = prefix.toLowerCase(Locale.ROOT);
        completions.removeIf(option -> !option.toLowerCase(Locale.ROOT).startsWith(normalized));
        return completions;
    }
}
