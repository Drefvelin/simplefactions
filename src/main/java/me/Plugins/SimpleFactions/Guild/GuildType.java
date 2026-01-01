package me.Plugins.SimpleFactions.Guild;

import org.bukkit.configuration.ConfigurationSection;

public class GuildType {
    private String id;
    private String name;
    private boolean base;

    public GuildType(String key, ConfigurationSection config) {
        id = key;
        name = config.getString("name", id);
        base = config.getBoolean("base", false);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public boolean isBase() { return base; }
}
