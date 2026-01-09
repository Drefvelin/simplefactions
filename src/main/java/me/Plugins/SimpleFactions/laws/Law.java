package me.Plugins.SimpleFactions.laws;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.enums.Scope;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;
import me.Plugins.TLibs.TLibs;

public class Law {
    private String id;
    private String name;
    private String icon;
    private List<String> requirements = new ArrayList<>();
    private List<String> description = new ArrayList<>();

    //Effects
    private Map<Scope, LawEffect> scopedEffects = new LinkedHashMap<>();

    public Law(String key, ConfigurationSection config) {
        id = key;
        icon = config.getString("icon", "v.book");
        name = StringFormatter.formatHex(config.getString("name", key));
        if(config.contains("requirements")) {
            for(String s : config.getStringList("requirements")) 
                requirements.add(s);
        }
        if(config.contains("description")) {
            for(String s : config.getStringList("description")) 
                description.add(StringFormatter.formatHex(s));
        }
        if(config.contains("effects")) {
            for(String s : config.getConfigurationSection("effects").getKeys(false)) {
                try {
                    Scope scope = Scope.valueOf(s.toUpperCase());
                    scopedEffects.put(scope, new LawEffect(scope, config.getConfigurationSection("effects."+s)));
                } catch (Exception e) {
                    Bukkit.getLogger().info("[SimpleFactions] could not parse modifier for law "+s);
                    // TODO: handle exception
                }
            }
        }
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getIconString() { return icon; }
    public ItemStack getIcon() {
        return TLibs.getItemAPI().getCreator().getItemFromPath(icon);
    }
    public List<String> getRequirements() { return requirements; }
    public boolean hasEffects() { return !scopedEffects.isEmpty(); }
    public Map<Scope, LawEffect> getScopedEffects() { return scopedEffects; }
    public List<String> getDescription() { return description; }
    public boolean hasDescription() { return !description.isEmpty(); }
    public boolean isAvailable(Faction f) {
        if(requirements.isEmpty()) return true;
        return true; //TODO add requirements
    }
}
