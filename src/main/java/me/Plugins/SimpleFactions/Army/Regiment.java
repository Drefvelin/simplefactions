package me.Plugins.SimpleFactions.Army;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Utils.Formatter;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public class Regiment {
	Formatter format = new Formatter();
	private String id;
	private String name;
	private int currentSlots;
	private int freeSlots;
	private int toOverlord;
	private double upkeep;
	private int expansionTime;
	
	private boolean levy;
	private boolean offense;
	
	private ItemStack icon;
	
	private List<String> description = new ArrayList<>();
	
	private List<LevyEntry> entries = new ArrayList<>();
	
	public Regiment(String key, ConfigurationSection config) {
		id = key;
		name = StringFormatter.formatHex(config.getString("name", "Regiment"));
		currentSlots = config.getInt("default-slots", 0);
		freeSlots = currentSlots;
		upkeep = config.getDouble("upkeep", 0);
		expansionTime = config.getInt("expansion-time", 21600);
		ConfigurationSection item = config.getConfigurationSection("item");
		icon = new ItemStack(Material.valueOf(item.getString("material").toUpperCase()), 1);
		ItemMeta m = icon.getItemMeta();
		if(item.contains("model-data")) {
			m.setCustomModelData(item.getInt("model-data"));
		}
		icon.setItemMeta(m);
		if(config.contains("description")) {
			for(String s : config.getStringList("description")) {
				description.add(StringFormatter.formatHex(s));
			}
		}
		levy = config.getBoolean("levy", false);
		offense = config.getBoolean("offense", false);
		toOverlord = 0;
	}
	
	public Regiment(Regiment another) {
		id = another.getId();
		name = another.getName();
		currentSlots = another.getCurrentSlots();
		freeSlots = another.getFreeSlots();
		upkeep = another.getUpkeep();
		expansionTime = another.getExpansionTime();
		icon = another.getIcon().clone();
		description = another.getDescription();
		levy = another.isLevy();
		toOverlord = another.sentToOverlord();
		offense = another.isOffensive();
	}
	
	public void setLevyEntries(List<LevyEntry> entries) {
		this.entries = entries;
	}
	
	public int getLevyTotal() {
		int total = 0;
		for(LevyEntry e : entries) {
			total += e.getAmount();
		}
		return total;
	}
	
	public LevyEntry getEntry(Faction f) {
		for(LevyEntry e : entries) {
			if(e.getFrom().getId().equalsIgnoreCase(f.getId())) return e;
		}
		return null;
	}
	
	public void addLevyEntry(LevyEntry e) {
		entries.add(e);
	}
	
	public List<LevyEntry> getEntries(){
		return entries;
	}
	
	public boolean isOffensive() {
		return offense;
	}
	
	public boolean isLevy() {
		return levy;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}
	
	public int sentToOverlord() {
		return toOverlord;
	}
	
	public void setSentToOverlord(int i) {
		toOverlord = i;
	}

	public void setCurrentSlots(int i){
		currentSlots = i;
	}

	public int getCurrentSlots() {
		return currentSlots;
	}

	public int getFreeSlots() {
		return freeSlots;
	}

	public double getUpkeep() {
		return upkeep;
	}
	
	public int getExpansionTime() {
		return expansionTime;
	}
	
	public ItemStack getIcon() {
		return icon;
	}
	
	public List<String> getDescription() {
		return description;
	}
	public void sizeIncrease() {
		currentSlots++;
	}
	
	public void sizeDecrease() {
		if(currentSlots == 0) return;
		currentSlots--;
	}
	
	public double getTotalUpkeep() {
		double d = 0;
		int paidSlots = currentSlots-freeSlots;
		if(paidSlots < 1) return 0;
		d += paidSlots*upkeep;
		return format.formatDouble(d);
	}
}
