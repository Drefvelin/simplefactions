package me.Plugins.SimpleFactions.Map;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.Database.Database;
import me.Plugins.SimpleFactions.Loaders.TitleLoader;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.TitleManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.REST.RestServer;
import me.Plugins.SimpleFactions.Tiers.Title;

public class MapSystem {
	private Compiler compiler = new Compiler();
	
	private int lastUpdate = 300;
	private int fullUpdate = 0;
	
	private HashMap<String, List<String>> queues = new HashMap<>();
	
	public void tick() {
		lastUpdate++;
		fullUpdate++;
		if(lastUpdate > 300 && !queues.isEmpty()) {
			updateMap();
		} else if(fullUpdate > 3600) {
			queueAllNations();
		}
	}
	
	public void queueAllNations() {
		fullUpdate = 0;
		for(Faction f : FactionManager.factions) {
			enqueue("nation", f.getRGB());
		}
		updateMap();
	}
	
	public void updateMap() {
		lastUpdate = 0;
		compiler.exportQueue(queues);
		RestServer.upload("queue", new File("plugins/SimpleFactions/MapAPI/queue.json"));
		Database db = new Database();
		for(Faction fac : FactionManager.factions) {
			db.saveFaction(fac);
		}
		compiler.exportAllFactionsToNationJson();
		RestServer.upload("nation", new File("plugins/SimpleFactions/MapAPI/nation.json"));
		RestServer.upload("county", new File("plugins/SimpleFactions/Input/county.json"));
		RestServer.upload("duchy", new File("plugins/SimpleFactions/Input/duchy.json"));
		RestServer.upload("kingdom", new File("plugins/SimpleFactions/Input/kingdom.json"));
		RestServer.upload("empire", new File("plugins/SimpleFactions/Input/empire.json"));
		RestServer.commenceRegen("queued");
		clear();
	}
	
	public void fullRegen() {
		lastUpdate = 0;
		Database db = new Database();
		for(Faction fac : FactionManager.factions) {
			db.saveFaction(fac);
		}
		compiler.exportAllFactionsToNationJson();
		RestServer.upload("nation", new File("plugins/SimpleFactions/MapAPI/nation.json"));
		RestServer.upload("county", new File("plugins/SimpleFactions/Input/county.json"));
		RestServer.upload("duchy", new File("plugins/SimpleFactions/Input/duchy.json"));
		RestServer.upload("kingdom", new File("plugins/SimpleFactions/Input/kingdom.json"));
		RestServer.upload("empire", new File("plugins/SimpleFactions/Input/empire.json"));
		RestServer.commenceRegen("fullregen");
	}
	
	public void enqueue(String type, String value) {
		if(queues.containsKey(type)) {
			if(queues.get(type).contains(value)) return;
			queues.get(type).add(value);
			return;
		}
		queues.put(type, new ArrayList<>(Arrays.asList(value)));
	}
	
	public void clear() {
		queues.clear();
	}
	
	public void claim(Player p, Faction f, int province) {
		if(province == 0) {
			p.sendMessage("§cThis location has no province!");
			return;
		} else if(f.getProvinces().contains(province)) {
			p.sendMessage("§cYour faction already owns this province!");
			return;
		}
		Faction owner = FactionManager.getByProvince(province);
		boolean stolen = false;
		if (owner != null) {
			if(TitleManager.overProvinceCap(owner)) {
				stolen = true;
			} else {
				p.sendMessage("§cProvince already claimed by " + owner.getName() + "!");
		    	return;
			}
		}
		claimProvince(p, f, owner, province, stolen);
	}

	private void claimProvince(Player p, Faction f, Faction owner, int province, boolean stolen) {
		double cost = TitleManager.getClaimCost(f);
		if(f.getPrestige() < cost) {
			p.sendMessage("§cYou need at least §f" + cost + "§c prestige to claim a new province §7(currently " + f.getPrestige() + ")");
			return;
		}
		if(TitleLoader.getByProvince(province) == null && f.getUntitledProvinces().size() >= Cache.maxUntitledProvinces) {
			p.sendMessage("§cYou have too many untitled provinces, form a county first!");
		    return;
		}
		if(stolen) {
			Player leader = Bukkit.getPlayerExact(owner.getLeader());
			if(leader != null) leader.sendMessage(f.getName()+" §cclaimed one of your provinces since you lacked the prestige to hold it!");
			unclaim(null, owner, province);
			enqueue("nation", owner.getRGB());
		}
		p.sendMessage(stolen ? "§aSuccessfully  claimed province "+province+" §afrom "+owner.getName() : "§aSuccessfully  claimed province "+province);
		f.addProvince(province);
		enqueue("nation", f.getRGB());
	}
	
	public void unclaim(Player p, Faction f, int province) {
		if(province == 0) {
			if(p != null) p.sendMessage("§cThis location has no province!");
			return;
		} else if(!f.getProvinces().contains(province)) {
			if(p != null) p.sendMessage("§cYour faction does not own this province!");
			return;
		}
		if(f.getCapital() == province) {
			if(p != null) p.sendMessage("§cCannot unclaim the capital!");
			return;
		}
		if(p != null) p.sendMessage("§aSuccessfully  unclaimed province "+province);
		f.removeProvince(province);
		Title t = TitleLoader.getByProvince(province);
		if(t != null) {
			List<Integer> provinces = TitleManager.getProvinces(f);
			List<Title> titles = TitleManager.getTitles(f);
			t.destroy(f, provinces, titles);
		}
		enqueue("nation", f.getRGB());
	}
}
