package me.Plugins.SimpleFactions.War;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import me.Plugins.SimpleFactions.Diplomacy.Relation;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.RelationManager;
import me.Plugins.SimpleFactions.Objects.Faction;

public class Participant {
	private Faction leader;
	private HashMap<Faction, Boolean> allies = new HashMap<>();
	private List<Faction> subjects = new ArrayList<>();
	
	private HashMap<Faction, WarGoal> warGoals = new HashMap<>();

	private boolean civilWar;
	
	public Participant(Faction leader) {
		this.leader = leader;
		for(Faction s : RelationManager.getSubjects(leader)) {
			subjects.add(s);
		}
		for(Faction a : RelationManager.getAllies(leader)) {
			allies.put(a, false);
		}
		civilWar = false;
	}

	public Participant(Faction leader, boolean civilWar) {
		this.leader = leader;
		for(Faction s : RelationManager.getSubjects(leader)) {
			subjects.add(s);
		}
		for(Faction a : RelationManager.getAllies(leader)) {
			allies.put(a, false);
		}
		this.civilWar = civilWar;
	}

	public Participant(Faction leader, List<Faction> subjects, Map<Faction, Boolean> allies, Map<Faction, WarGoal> warGoals, boolean civilWar) {
		this.leader = leader;
		this.subjects = new ArrayList<>(subjects);
		this.allies = new HashMap<>(allies);
		this.warGoals = new HashMap<>(warGoals);
		this.civilWar = civilWar;
	}

	public void update(War w) {
		Iterator<Faction> iterator = subjects.iterator();
		while(iterator.hasNext()) {
			Faction subject = iterator.next();
			if(w.isMainParticipant(subject) || !RelationManager.getOverlord(subject).equalsIgnoreCase(leader.getId())) {
				iterator.remove();
			}
		}
		Iterator<Map.Entry<Faction, Boolean>> allyIterator = allies.entrySet().iterator();
		while (allyIterator.hasNext()) {
			Map.Entry<Faction, Boolean> entry = allyIterator.next();
			if(!entry.getKey().getRelation(leader.getId()).getType().getId().equalsIgnoreCase("ally")) {
				allyIterator.remove(); // Safe: modifies original map
			}
		}
		for(Map.Entry<String, Relation> entry : leader.getRelations().entrySet()){
			Faction ally = FactionManager.getByString(entry.getKey());
			if(!entry.getValue().getType().getId().equalsIgnoreCase("ally")) continue;
			if(ally == null) continue;
			if(allies.containsKey(ally)) continue;
			allies.put(ally, false);
		}
	}


	public boolean isCivilWar(){
		return civilWar;
	}

	public void setCivilWar(boolean b){
		civilWar = b;
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
