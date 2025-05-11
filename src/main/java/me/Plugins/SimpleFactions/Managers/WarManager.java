package me.Plugins.SimpleFactions.Managers;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Objects.Request.WarRequest;
import me.Plugins.SimpleFactions.War.Side;
import me.Plugins.SimpleFactions.War.War;

public class WarManager {
	private static List<War> wars = new ArrayList<>();
	
	public static void addWar(War w) {
		wars.add(w);
	}
	
	public static boolean exists(Faction attacker, Faction defender) {
		for(War w : wars) {
			if(w.isMainParticipant(attacker) && w.isMainParticipant(defender)) return true;
			Side s = w.getSide(attacker);
			if(s == null) continue;
			if(s.equals(w.getSide(defender))) return true;
		}
		return false;
	}
	
	public static boolean existsHostile(Faction attacker, Faction defender) {
		for(War w : wars) {
			Side s = w.getSide(attacker);
			if(s == null) continue;
			Side d = w.getSide(defender);
			if(d == null) continue;
		    if(!s.equals(d)) return true;
		}
		return false;
	}
	
	public static List<War> get(){
		return wars;
	}
	
	public static War getById(int i) {
		for(War w : wars) {
			if(w.getId() == i) return w;
		}
		return null;
	}
	
	public static int newId() {
		int i = 0;
		while(getById(i) != null) i++;
		return i;
	}
	
	public static War getByFaction(Faction f) {
		for(War w : wars) {
			if(w.getSide(f) != null) return w;
		}
		return null;
	}
	
	public static void sendRequest(Player sender, Faction origin, Faction target, War w) {
		if(!w.canBeCalled(target)) {
			sender.sendMessage("�cTarget faction is already part of the war");
			return;
		}
		Player p = Bukkit.getPlayerExact(target.getLeader());
		if(p == null || !p.isOnline()) {
			sender.sendMessage("�cCannot send request, target faction leader is not online!");
			return;
		}
		sender.sendMessage("�aSent a call to arms to "+target.getName());
		p.sendMessage(FactionManager.getByLeader(sender.getName()).getName()+" �7is requesting that you aid them in their war against "+w.getEnemy(origin).getName());
		p.sendMessage("�7Type �a/faction accept �7to accept");
		p.sendMessage("�7Request will time out in 60 seconds");
		RequestManager.addRequest(sender, p, new WarRequest(FactionManager.getByLeader(sender.getName()), w));
	}
	
	public static void acceptRequest(Player p) {
		WarRequest req = (WarRequest) RequestManager.getRequest(p);
		Faction reciever = FactionManager.getByLeader(p.getName());
		if(reciever == null) {
			p.sendMessage("�cYou do not have a faction");
			return;
		}
		Faction sender = req.getSender();
		Player sp = Bukkit.getPlayerExact(sender.getLeader());
		if(sp != null && sp.isOnline()) sp.sendMessage(reciever.getName()+" �aaccepted your call to arms");
		p.sendMessage("�aYour faction has joined the "+req.getWar().getName());
		req.getWar().call(sender, reciever);
	}
}
