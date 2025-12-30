package me.Plugins.SimpleFactions.Guild;

import org.bukkit.configuration.ConfigurationSection;

public class GuildType {
    private String id;
    private String name;

    public GuildType(String key, ConfigurationSection config) {
        id = key;
        name = config.getString("name", id);
    }

    public String getId() { return id; }
    public String getName() { return name; }
}
