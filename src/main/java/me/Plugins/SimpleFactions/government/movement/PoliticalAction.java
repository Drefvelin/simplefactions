package me.Plugins.SimpleFactions.government.movement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import me.Plugins.TLibs.TLibs;

public class PoliticalAction {
    private Action action;
    private String name;
    private String icon;
    private List<String> description = new ArrayList<>();
    private Map<String, List<String>> pools = new LinkedHashMap<>();

    public PoliticalAction(String key, ConfigurationSection config) {
        try {
            action = Action.valueOf(key.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid political action key: " + key);
        }
        this.icon = config.getString("icon", "v.paper");
        this.name = config.getString("name", key);
        this.description = config.getStringList("description");
        for (String poolKey : config.getConfigurationSection("pools").getKeys(false)) {
            pools.put(poolKey, config.getStringList("pools." + poolKey));
        }
    }

    public Action getAction() {
        return action;
    }

    public String getName() {
        return name;
    }

    public String getIconString() {
        return icon;
    }

    public ItemStack getIcon() {
        return TLibs.getItemAPI().getCreator().getItemFromPath(icon);
    }

    public List<String> getDescription() {
        return description;
    }

    public Map<String, List<String>> getPools() {
        return pools;
    }

    public boolean allowMembers(String pool) {
        return pools.containsKey(pool) && pools.get(pool).contains("members");
    }

    public boolean allowGuilds(String pool) {
        return pools.containsKey(pool) && pools.get(pool).contains("guilds");
    }

    public boolean allowFactions(String pool) {
        return pools.containsKey(pool) && pools.get(pool).contains("factions");
    }
}
