package me.Plugins.SimpleFactions.Diplomacy;

import org.bukkit.configuration.ConfigurationSection;

import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public class Attitude {
	private String id;
	private String name;
	private int target;
	
	private boolean def;
	
	public Attitude(String key, ConfigurationSection config) {
		id = key;
		name = StringFormatter.formatHex(config.getString("name", "None"));
		target = config.getInt("target", 0);
		def = config.getBoolean("default", false);
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public int getTarget() {
		return target;
	}
	
	public boolean isDefault() {
		return def;
	}
}
