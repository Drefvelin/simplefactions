package me.Plugins.SimpleFactions.Map;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.bukkit.entity.Player;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.Loaders.TitleLoader;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.TitleManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.REST.RestServer;
import me.Plugins.SimpleFactions.Tiers.Title;
import me.Plugins.SimpleFactions.Utils.Database;

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
			p.sendMessage("�cThis location has no province!");
			return;
		} else if(f.getProvinces().contains(province)) {
			p.sendMessage("�cYour faction already owns this province!");
			return;
		}
		Faction owner = FactionManager.getByProvince(province);
		if (owner != null) {
		    p.sendMessage("�cProvince already claimed by " + owner.getName() + "!");
		    return;
		}
		if(f.getPrestige() < TitleManager.getClaimCost(f)) {
			p.sendMessage("�cYou need at least �f"+TitleManager.getClaimCost(f)+"�c prestige to claim a new province �7(currently "+f.getPrestige()+")");
		    return;
		}
		if(TitleLoader.getByProvince(province) == null && f.getUntitledProvinces().size() >= Cache.maxUntitledProvinces) {
			p.sendMessage("�cYou have too many untitled provinces, form a county first!");
		    return;
		}
		p.sendMessage("�aSuccesfully claimed province "+province);
		f.addProvince(province);
		enqueue("nation", f.getRGB());
	}
	
	public void unclaim(Player p, Faction f, int province) {
		if(province == 0) {
			p.sendMessage("�cThis location has no province!");
			return;
		} else if(!f.getProvinces().contains(province)) {
			p.sendMessage("�cYour faction does not own this province!");
			return;
		}
		p.sendMessage("�aSuccesfully unclaimed province "+province);
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
