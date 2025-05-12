package me.Plugins.SimpleFactions.War;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import me.Plugins.SimpleFactions.Managers.WarManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public class War {
	private int id;
	private Side attackers;
	private Side defenders;
	
	public War(Faction attacker, Faction defender) {
		id = WarManager.newId();
		attackers = new Side(attacker);
		defenders = new Side(defender);
	}

	public War(int i, Faction attacker, Faction defender) {
		id = i;
		attackers = new Side(attacker);
		defenders = new Side(defender);
	}
	
	public int getId() {
		return id;
	}
	
	public Side getAttackers() {
		return attackers;
	}
	
	public Side getDefenders() {
		return defenders;
	}
	
	public boolean canSwitchSides(Faction f) {
		return true;
		/*
		if(isMainParticipant(f)) return false;
		if(RelationManager.getOverlord(f) == null) return false;
		Faction overlord = FactionManager.getByString(RelationManager.getOverlord(f));
		if(getSide(overlord) == null) return false;
		return true;
		*/
	}
	
	public List<Participant> getParticipants(){
		List<Participant> list = new ArrayList<>();
		list.addAll(attackers.getMainParticipants());
		list.addAll(defenders.getMainParticipants());
		return list;
	}
	
	public String getName() {
		Faction attacker = attackers.getLeader();
		Faction defender = defenders.getLeader();
		return StringFormatter.formatHex(attacker.getName()+" #a83116vs. "+defender.getName());
	}
	
	public Participant getParticipant(Faction f) {
		for(Participant p : attackers.getMainParticipants()) {
			if(p.getLeader().getId().equalsIgnoreCase(f.getId())) return p;
		}
		for(Participant p : defenders.getMainParticipants()) {
			if(p.getLeader().getId().equalsIgnoreCase(f.getId())) return p;
		}
		return null;
	}
	
	public Side getSide(Faction f) {
		for(Participant p : attackers.getMainParticipants()) {
			if(p.getLeader().getId().equalsIgnoreCase(f.getId())) return attackers;
			if(p.getSubjects().contains(f)) return attackers;
			if(p.getAllies().containsKey(f) && p.getAllies().get(f)) return attackers;
		}
		for(Participant p : defenders.getMainParticipants()) {
			if(p.getLeader().getId().equalsIgnoreCase(f.getId())) return defenders;
			if(p.getSubjects().contains(f)) return defenders;
			if(p.getAllies().containsKey(f) && p.getAllies().get(f)) return defenders;
		}
		return null;
	}
	public Side getSide(Participant p) {
		return getSide(p.getLeader());
	}
	
	public Side getOppositeSide(Faction f) {
		Side same = getSide(f);
		if(same.equals(attackers)) return defenders;
		if(same.equals(defenders)) return attackers;
		return null;
	}
	
	public HashMap<Faction, WarGoal> getWarGoalsOn(Faction p) {
		HashMap<Faction, WarGoal> map = new HashMap<>();
		List<Participant> list = new ArrayList<>();
		if(getSide(p).equals(defenders)) {
			list = attackers.getMainParticipants();
		} else {
			list = defenders.getMainParticipants();
		}
		for(Participant par : list) {
			if(par.hasWarGoal(p)) map.put(par.getLeader(), par.getWarGoal(p));
		}
		return map;
	}
	
	public boolean isMainParticipant(Faction f) {
		return getParticipant(f) != null;
	}
	
	public String getType(Faction f) {
		for(Participant p : attackers.getMainParticipants()) {
			if(p.getLeader().getId().equalsIgnoreCase(f.getId())) return "main_attacker";
		}
		for(Participant p : defenders.getMainParticipants()) {
			if(p.getLeader().getId().equalsIgnoreCase(f.getId())) return "main_defender";
		}
		return "secondary_participant";
	}
	
	public boolean canBeCalled(Faction f) {
		return !attackers.isParticipating(f) || !defenders.isParticipating(f);
	}
	
	public Faction getEnemy(Faction f) {
		if(attackers.isParticipating(f)) return defenders.getLeader();
		if(defenders.isParticipating(f)) return attackers.getLeader();
		return null;
	}
	
	public boolean call(Faction caller, Faction joiner) {
		Participant p = getParticipant(caller);
		if(p == null) return false;
		if(!p.getAllies().containsKey(joiner)) return false;
		if(p.getAllies().get(joiner) == true) return false;
		p.getAllies().put(joiner, true);
		return true;
	}
}
