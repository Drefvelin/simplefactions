package me.Plugins.SimpleFactions.Loaders;

import java.io.File;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.enums.Terrain;

public class ConfigLoader {
	public void loadConfig(File configFile) {
		FileConfiguration config = new YamlConfiguration();
        try {
        	config.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
		Cache.mapRef = config.getString("map-reference", "main");

		Cache.maxMembers = config.getInt("max-members", 64);
		Cache.maxWealthPrestige = config.getInt("max-prestige-from-wealth", 1000);
		Cache.bankBlock = config.getString("bank-block", "v.lodestone");
		Cache.maxExtraNodeCapacity = config.getInt("max-extra-node-capacity", 0);
		
		Cache.deJureRequirement = config.getDouble("de-jure-requirement", 100.0);
		Cache.maxUntitledProvinces = config.getInt("max-untitled-provinces", 5);
		Cache.maxFreeTitles = config.getInt("max-free-titles", 3);
		Cache.mapEnabled = config.getBoolean("enable-map", false);

		Cache.provinceCost = config.getInt("province-cost", 50);

		Cache.branchUpgradeCost = config.getDouble("branch-upgrade-cost", 100.0);
		Cache.branchUpgradeExponent = config.getDouble("branch-upgrade-exponent", 1.1);

		if(config.contains("terrain-modifiers")) {
			for(String s : config.getStringList("terrain-modifiers")) {
				String[] args = s.split("\\s+");
				if(args.length != 2) continue;
				try {
					Cache.tradeCarry.put(Terrain.valueOf(args[0].toUpperCase()), Double.parseDouble(args[1]));
				} catch (Exception e) {
					// TODO: handle exception
					Bukkit.getLogger().info("Could not parse "+s);
				}
			}
		}
		
		if(config.contains("icons")) {
			for(String s : config.getStringList("icons")) {
				String id = s.split("\\(")[0];
				String path = s.split("\\(")[1].replace(")", "");
				Cache.icons.put(id, path);
			}
		}
	}
}
