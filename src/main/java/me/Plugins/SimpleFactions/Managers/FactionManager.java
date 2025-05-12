package me.Plugins.SimpleFactions.Managers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.Diplomacy.Attitude;
import me.Plugins.SimpleFactions.Diplomacy.Relation;
import me.Plugins.SimpleFactions.Diplomacy.RelationType;
import me.Plugins.SimpleFactions.Loaders.RelationLoader;
import me.Plugins.SimpleFactions.Map.MapSystem;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Objects.Modifier;
import me.Plugins.SimpleFactions.Objects.PrestigeRank;
import me.Plugins.SimpleFactions.Tiers.Title;
import me.Plugins.SimpleFactions.Utils.Database;
import me.Plugins.SimpleFactions.Utils.Formatter;

public class FactionManager implements Listener{
	public static int timer = 0;

	public static List<Faction> factions = new ArrayList<Faction>();
	
	public static HashMap<Faction, List<String>> dbRelations = new HashMap<>();

	public static int getTimer(){
		return timer;
	}
	
	public static void addDBRelation(Faction f, String s) {
		List<String> list = new ArrayList<>();
		if(dbRelations.containsKey(f)) {
			list = dbRelations.get(f);
		}
		list.add(s);
		dbRelations.put(f, list);
	}
	
	public static void loadRelations() {
		for(Map.Entry<Faction, List<String>> entry : dbRelations.entrySet()) {
			Faction f = entry.getKey();
			List<String> relations = entry.getValue();
			for(String s : relations) {
				Faction target = getByString(s.split("\\(")[0]);
				if(target == null) continue;
				String info = s.split("\\(")[1].replace(")", "");
				RelationType r = RelationLoader.getType(info.split("\\.")[0]);
				Attitude a = RelationLoader.getAttitude(info.split("\\.")[1]);
				if(r == null || a == null) continue;
				int opinion = Integer.parseInt(info.split("\\.")[2]);
				f.setRelation(target, new Relation(r, a, opinion));
				if(target.getRelation(f.getId()).isDefault() && r.isVassalage()) {
					target.setRelation(f, new Relation(r.getLink(), RelationLoader.getDefaultAttitude(), 0));
				}
			}
		}
		dbRelations.clear();
	}
	
	
	public static Double globalWealth = 0.0;
	
	public static MapSystem map = new MapSystem();
	
	public static InventoryManager inv = new InventoryManager();
	
	public static InventoryManager getInv() {
		return inv;
	}
	
	public static MapSystem getMap() {
		return map;
	}
	
	public static Faction getTitleOwner(Title t) {
		for(Faction f : factions) {
			if(f.hasTitle(t)) return f;
		}
		return null;
	}
	
	public static Faction getByProvince(int i) {
		for(Faction f : factions) {
			if(f.getProvinces().contains(i)) return f;
		}
		return null;
	}
	
	private void tickCycle() {
		new BukkitRunnable() {
			@Override
	        public void run() {
				for(Faction f : factions) {
					f.tick();
				}
				map.tick();
				RelationManager.tick();
				inv.getUpdater().updateInventory();
				time();
	        }
	    }.runTaskTimer(SimpleFactions.plugin, 0L, 20L);
	}

	public void time() {
		timer++;

		if (timer >= 86400) {
			for(Faction f : factions){
				f.newDay();
			}
			timer = 0;
		}
	}
	
	public void run() {
		loadRelations();
		tickCycle();		
		for(Faction f : factions) {
			f.updatePrestige();
		}
	}
	
	public void start(List<Faction> l) {
		factions = l;
	}
	public static void addFaction(Faction f) {
		factions.add(f);
	}
	public static void deleteFaction(Faction f){
		map.enqueue("nation", f.getRGB());
		factions.remove(f);
		Database db = new Database();
		db.deleteFaction(f);
	}
	public static void updateAllPrestige() {
		for(Faction f : factions) {
			f.updatePrestige();
		}
	}
	public static Double getRankUpAmount(PrestigeRank rank) {
		List<Faction> ranked = new ArrayList<Faction>();
		for(Faction f : factions) {
			if(f.getRank().getId().equalsIgnoreCase(rank.getId())) {
				ranked.add(f);
			}
		}
		if(ranked.size() < 1) {
			return rank.getMin();
		}
		Collections.sort(ranked, new Comparator<Faction>() {
		    @Override
		    public int compare(Faction c1, Faction c2) {
		        return Double.compare(c1.getPrestige(), c2.getPrestige());
		    }
		});
		Collections.reverse(ranked);
		Double amount = ranked.get(0).getPrestige()*(rank.getPercentage()/100);
		if(amount > rank.getMin()) {
			return amount;
		}
		return rank.getMin();
	}
	public static Double getGlobalWealth() {
		Formatter format = new Formatter();
		Double amount = 0.0;
		for(Faction f : factions) {
			amount = amount + f.getWealth();
		}
		return format.formatDouble(amount);
	}
	public static Double getGlobalLiquidWealth() {
		Formatter format = new Formatter();
		Double amount = 0.0;
		for(Faction f : factions) {
			for(Modifier m : f.getWealthModifiers()) {
				if(m.getType().equalsIgnoreCase("bank")) {
					amount = amount + m.getAmount();
				}
			}
		}
		return format.formatDouble(amount);
	}
	public static Double getGlobalNodeWealth() {
		Formatter format = new Formatter();
		Double amount = 0.0;
		for(Faction f : factions) {
			for(Modifier m : f.getWealthModifiers()) {
				if(m.getType().equalsIgnoreCase("nodes")) {
					amount = amount + m.getAmount();
				}
			}
		}
		return format.formatDouble(amount);
	}
	public static Faction getByString(String s) {
		for(Faction f : factions) {
			if(f.getId().equalsIgnoreCase(s)) return f;
		}
		return null;
	}
	public static List<Faction> getCopy(){
		List<Faction> c = new ArrayList<Faction>();
		for(Faction f : factions) {
			c.add(f);
		}
		return c;
	}
	public static Faction getByLeader(String name) {
		for(Faction f : factions) {
			if(f.getLeader().equalsIgnoreCase(name)) return f;
		}
		return null;
	}
	public static Faction getByMember(String name) {
		for(Faction f : factions) {
			if(f.getMembers().contains(name)) return f;
			if(f.getLeader().equalsIgnoreCase(name)) {
				f.addMember(name);
				return f;
			}
		}
		return null;
	}
	
	public static Faction getByRGB(String rgb) {
		for(Faction f : factions) {
			if(f.getRGB().equalsIgnoreCase(rgb)) return f;
		}
		return null;
	}
	
	/**
	 * Validates an RGB string in the format "R,G,B"
	 * 
	 * @param rgb The input RGB string
	 * @return 0 if valid, or error code:
	 *         1 - Incorrect number of components
	 *         2 - Component is not a number
	 *         3 - Component is out of range (not between 0-255)
	 */
	public static int validateRGB(String rgb) {
	    if (rgb == null) return 1;

	    String[] parts = rgb.trim().split(",");
	    if (parts.length != 3) return 1;

	    try {
	        for (String part : parts) {
	            int value = Integer.parseInt(part.trim());
	            if (value < 0 || value > 255) return 3;
	        }
	        return 0; // All good
	    } catch (NumberFormatException e) {
	        return 2;
	    }
	}
}
