package me.Plugins.SimpleFactions.Loaders;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import me.Plugins.SimpleFactions.War.WarGoal;

public class WarGoalLoader {
	public static List<WarGoal> oList = new ArrayList<>();
	public static List<WarGoal> get(){
		return oList;
	}
	public static WarGoal getByString(String id) {
		for(WarGoal g : oList) {
			if(g.getId().equalsIgnoreCase(id)) return g;
		}
		return null;
	}
	public void load(File configFile) {
		FileConfiguration config = new YamlConfiguration();
        try {
        	config.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
		Set<String> set = config.getKeys(false);

		List<String> list = new ArrayList<String>(set);
		
		for(String key : list) {
			WarGoal g = new WarGoal(key, config.getConfigurationSection(key));
			oList.add(g);
		}
	}
}
