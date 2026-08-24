package me.Plugins.SimpleFactions.Managers;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.Objects.Request.AutoresolveRequest;
import me.Plugins.SimpleFactions.Objects.Request.ElevateRequest;
import me.Plugins.SimpleFactions.Objects.Request.MovementJoinRequest;
import me.Plugins.SimpleFactions.Objects.Request.RelationRequest;
import me.Plugins.SimpleFactions.Objects.Request.RelocateRequest;
import me.Plugins.SimpleFactions.Objects.Request.Request;
import me.Plugins.SimpleFactions.Objects.Request.WarRequest;
import me.Plugins.SimpleFactions.War.campaign.runtime.BattleAutoresolveService;

public class RequestManager {
	private static HashMap<Player, Request> requests = new HashMap<>();
	
	public static void start() {
		new BukkitRunnable() {
			@Override
	        public void run() {
				for(Map.Entry<Player, Request> entry : requests.entrySet()) {
					if(entry.getValue().timedOut()) requests.remove(entry.getKey());
				}
	        }
	    }.runTaskTimer(SimpleFactions.plugin, 0L, 20L);
	}
	
	public static boolean hasRequest(Player p) {
		return requests.containsKey(p);
	}
	
	public static Request getRequest(Player p) {
		return requests.get(p);
	}
	
	public static void addRequest(Player sender, Player p, Request r) {
		if(hasRequest(p)) {
			sender.sendMessage("§cThe target is already considering another request.");
			return;
		}
		requests.put(p, r);
	}
	
	public static void accept(Player p) {
		if(!hasRequest(p)) return;
		Request req = requests.get(p);
		if(req instanceof RelationRequest rreq) {
			if(rreq.isTrade()) {
				RelationManager.acceptTradeRequest(p);
			} else {
				RelationManager.acceptRequest(p);
			}
		} else if(req instanceof WarRequest){
			WarManager.acceptRequest(p);
		} else if(req instanceof AutoresolveRequest) {
			BattleAutoresolveService.acceptRequest(p);
		} else if(req instanceof RelocateRequest){
			FactionManager.acceptRelocateRequest(p);
		} else if(req instanceof ElevateRequest) {
			FactionManager.acceptElevationRequest(p);
		} else if(req instanceof MovementJoinRequest) {
			FactionManager.acceptMovementJoinRequest(p);
		}
		requests.remove(p);
	}
}
