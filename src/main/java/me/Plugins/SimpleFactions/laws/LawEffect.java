package me.Plugins.SimpleFactions.laws;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import me.Plugins.SimpleFactions.Objects.FactionModifier;
import me.Plugins.SimpleFactions.enums.Region;
import me.Plugins.SimpleFactions.enums.Rules;
import me.Plugins.SimpleFactions.enums.Scope;

public class LawEffect {
    private Map<Rules, Boolean> rules = new LinkedHashMap<>();
    private List<FactionModifier> globalModifiers = new ArrayList<>();
    private Map<Region, List<FactionModifier>> regionModifiers = new LinkedHashMap<>();
    
    public LawEffect(Scope scope, ConfigurationSection config) {

        // ---- Rules ----
        if (config.contains("rules")) {
            for (String s : config.getStringList("rules")) {
                try {
                    String[] args = s.split("\\s+");
                    if (args.length != 2) continue;

                    Rules rule = Rules.valueOf(args[0].toUpperCase());
                    rules.put(rule, Boolean.parseBoolean(args[1]));
                } catch (Exception e) {
                    Bukkit.getLogger().warning(
                        "[SimpleFactions] Invalid rule in " + config.getCurrentPath()
                    );
                }
            }
        }

        // ---- Global modifiers ----
        if (config.contains("modifiers")) {
            for (String mod : config.getStringList("modifiers")) {
                try {
                    globalModifiers.add(new FactionModifier(mod));
                } catch (Exception ignored) {}
            }
        }

        // ---- Region modifiers ----
        for (String key : config.getKeys(false)) {

            // Skip known non-region sections
            if (key.equalsIgnoreCase("rules")
             || key.equalsIgnoreCase("modifiers")) continue;

            try {
                Region region = Region.valueOf(key.toUpperCase());

                for (String mod : config.getStringList(key)) {
                    try {
                        addModifier(region, new FactionModifier(mod));
                    } catch (Exception e) {
                        Bukkit.getLogger().warning(
                            "[SimpleFactions] Invalid modifier: " + mod
                        );
                    }
                }

            } catch (IllegalArgumentException ignored) {
                // Not a region key → safe to ignore
            }
        }
    }

    public void addModifier(Region region, FactionModifier mod) {
        regionModifiers
            .computeIfAbsent(region, r -> new ArrayList<>())
            .add(mod);
    }

    public List<FactionModifier> getGlobalModifiers() {
        return globalModifiers;
    }

    public Map<Region, List<FactionModifier>> getRegionModifiers() {
        return regionModifiers;
    }

    public Map<Rules, Boolean> getRules() {
        return rules;
    }

    public boolean hasGlobalModifiers() {
        return !globalModifiers.isEmpty();
    }

    public boolean hasRegionModifiers() {
        return !regionModifiers.isEmpty();
    }

    public boolean hasRules() {
        return !rules.isEmpty();
    }
}
