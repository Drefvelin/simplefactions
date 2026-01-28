package me.Plugins.SimpleFactions.Diplomacy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Objects.FactionModifier;

public class DiplomacyHandler {
    private Faction f;
    private HashMap<String, Relation> relations = new HashMap<>();

    public DiplomacyHandler(Faction f) {
        this.f = f;
    }

    public HashMap<String, Relation> getRelations(){
		return relations;
	}
	
	public Relation getRelation(String s) {
		if(relations.containsKey(s)) return relations.get(s);
		return new Relation();
	}

    public void setRelation(Faction f, Relation r) {
		//update
		relations.put(f.getId(), r);
	}
	
	public void updateRelations() {
		for(Map.Entry<String, Relation> entry : relations.entrySet()) {
			entry.getValue().tick();
		}
	}

    public List<FactionModifier> getModifiers() {
        List<FactionModifier> mods = new ArrayList<>();
        for(Map.Entry<String, Relation> entry : relations.entrySet()) {
            Relation r = entry.getValue();
            if(r.getType().hasRecieveModifiers()) {
                mods.addAll(r.getType().getRecieveModifiers());
            }
            Faction other = FactionManager.getByString(entry.getKey());
            Relation back = other.getRelation(f.getId());
            if(back.getType().hasGiveModifiers()) {
                mods.addAll(back.getType().getGiveModifiers());
            }
        }
        return mods;
    }
}
