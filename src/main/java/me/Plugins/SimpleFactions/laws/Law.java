package me.Plugins.SimpleFactions.laws;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Objects.FactionModifier;
import me.Plugins.SimpleFactions.enums.FactionModifiers;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public class Law {
    private String id;
    private String name;
    private List<String> requirements = new ArrayList<>();
    private Map<FactionModifiers, FactionModifier> modifiers = new LinkedHashMap<>();
    private List<String> description = new ArrayList<>();

    public Law(String key, ConfigurationSection config) {
        id = key;
        name = StringFormatter.formatHex(config.getString("name", key));
        if(config.contains("requirements")) {
            for(String s : config.getStringList("requirements")) 
                requirements.add(s);
        }
        if(config.contains("description")) {
            for(String s : config.getStringList("description")) 
                description.add(StringFormatter.formatHex(s));
        }
        if(config.contains("modifiers")) {
            for(String s : config.getStringList("modifiers")) {
                try {
                    FactionModifier mod = new FactionModifier(s);
                    modifiers.put(mod.getType(), mod);
                } catch (Exception e) {
                    Bukkit.getLogger().info("[SimpleFactions] could not parse modifier for law "+s);
                    // TODO: handle exception
                }
            }
        }
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public List<String> getRequirements() { return requirements; }
    public List<FactionModifier> getModifiers() {
        return new ArrayList<>(modifiers.values());
    }
    public List<String> getDescription() { return description; }
    public boolean isAvailable(Faction f) {
        if(requirements.isEmpty()) return true;
        return true; //TODO add requirements
    }
}
