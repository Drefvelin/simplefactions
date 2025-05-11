package me.Plugins.SimpleFactions.Diplomacy;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;

import me.Plugins.SimpleFactions.Loaders.RelationLoader;
import me.Plugins.SimpleFactions.Objects.FactionModifier;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public class RelationType {
	private String id;
	private String name;
	private String prefix;
	private int target;
	
	private boolean visible;
	private boolean def;
	
	private boolean settable;
	private boolean mutual;
	
	private boolean vassal;
	private boolean overlord;
	
	private boolean lock;
	
	private String link;
	
	private List<FactionModifier> giveModifiers = new ArrayList<>();
	private List<FactionModifier> recieveModifiers = new ArrayList<>();
	
	private Threshold threshold;
	
	public RelationType(String key, ConfigurationSection config) {
		id = key;
		name = StringFormatter.formatHex(config.getString("name", "None"));
		prefix = StringFormatter.formatHex(config.getString("prefix", "#a89977Our "));
		target = config.getInt("target", 0);
		def = config.getBoolean("default", false);
		visible = config.getBoolean("visible", true);
		settable = config.getBoolean("settable", true);
		mutual = config.getBoolean("mutual", false);
		link = config.getString("link", key);
		vassal = config.getBoolean("vassal", false);
		overlord = config.getBoolean("overlord", false);
		lock = config.getBoolean("lock", false);
		if(config.isConfigurationSection("threshold")) {
			threshold = new Threshold(config.getConfigurationSection("threshold"));
		}
		if(config.contains("give-modifiers")) {
			for(String s : config.getStringList("give-modifiers")) {
				giveModifiers.add(new FactionModifier(s));
			}
		}
		if(config.contains("recieve-modifiers")) {
			for(String s : config.getStringList("recieve-modifiers")) {
				recieveModifiers.add(new FactionModifier(s));
			}
		}
	}
	
	public boolean hasThreshold() {
		return threshold != null;
	}
	
	public Threshold getThreshold() {
		return threshold;
	}
	
	public boolean fulfilled(int opinion) {
		return threshold.fulfilled(opinion);
	}
	
	public String getFormattedThresholdType() {
		return threshold.getFormattedType();
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}
	
	public String getPrefix() {
		return prefix;
	}
	
	public String getFull() {
		return prefix+name;
	}

	public int getTarget() {
		return target;
	}
	
	public boolean isDefault() {
		return def;
	}
	
	public boolean isVisible() {
		return visible;
	}

	public boolean isSettable() {
		return settable;
	}

	public boolean isMutual() {
		return mutual;
	}
	
	public boolean isVassalage() {
		return vassal;
	}
	
	public boolean isOverlord() {
		return overlord;
	}
	
	public boolean hasLock() {
		return lock;
	}
	
	public boolean shouldUpdateMap() {
		return overlord || vassal;
	}
	
	public boolean willReset() {
		return mutual || overlord || vassal || hasLink();
	}
	
	public boolean hasGiveModifiers() {
		return giveModifiers.size() > 0;
	}
	
	public List<FactionModifier> getGiveModifiers() {
		return giveModifiers;
	}
	
	public boolean hasRecieveModifiers() {
		return recieveModifiers.size() > 0;
	}
	
	public List<FactionModifier> getRecieveModifiers() {
		return recieveModifiers;
	}
	
	public boolean hasLink() {
		return !id.equalsIgnoreCase(link);
	}

	public String getLinkString() {
		return link;
	}
	
	public RelationType getLink() {
		return RelationLoader.getType(link);
	}
}
