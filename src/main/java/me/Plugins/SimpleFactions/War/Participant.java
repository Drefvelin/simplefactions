package me.Plugins.SimpleFactions.War;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.Plugins.SimpleFactions.Managers.RelationManager;
import me.Plugins.SimpleFactions.Objects.Faction;

public class Participant {
	private Faction leader;
	private HashMap<Faction, Boolean> allies = new HashMap<>();
	private List<Faction> subjects = new ArrayList<>();
	
	private HashMap<Faction, WarGoal> warGoals = new HashMap<>();
	
	public Participant(Faction leader) {
		this.leader = leader;
		for(Faction s : RelationManager.getSubjects(leader)) {
			subjects.add(s);
		}
		for(Faction a : RelationManager.getAllies(leader)) {
			allies.put(a, false);
		}
	}

	public Participant(Faction leader, List<Faction> subjects, Map<Faction, Boolean> allies, Map<Faction, WarGoal> warGoals) {
		this.leader = leader;
		this.subjects = new ArrayList<>(subjects);
		this.allies = new HashMap<>(allies);
		this.warGoals = new HashMap<>(warGoals);
	}


	public Faction getLeader() {
		return leader;
	}

	public HashMap<Faction, Boolean> getAllies() {
		return allies;
	}

	public List<Faction> getSubjects() {
		return subjects;
	}
	
	public void clean(Faction f) {
		if(subjects.contains(f)) subjects.remove(f);
		if(allies.containsKey(f)) allies.remove(f);
	}
	
	public boolean hasWarGoal(Faction f) {
		return warGoals.containsKey(f);
	}
	
	public WarGoal getWarGoal(Faction f) {
		if(hasWarGoal(f)) return warGoals.get(f);
		return null;
	}
	public HashMap<Faction, WarGoal> getWarGoals() {
		return warGoals;
	}
	
	public void addWarGoal(Faction f, WarGoal goal) {
		warGoals.put(f, goal);
	}
	
	public List<Faction> getAllParticipatingFactions(){
		List<Faction> list = new ArrayList<>();
		list.add(leader);
		list.addAll(subjects);
		for(Map.Entry<Faction, Boolean> entry : allies.entrySet()) {
			if(entry.getValue()) list.add(entry.getKey());
		}
		return list;
	}
}
