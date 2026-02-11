package me.Plugins.SimpleFactions.Diplomacy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.RelationManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Objects.FactionModifier;
import me.Plugins.SimpleFactions.enums.FactionModifiers;
import me.Plugins.SimpleFactions.enums.GuildModifier;

public class DiplomacyHandler {
    private Faction f;
    private HashMap<String, Relation> relations = new HashMap<>();

    public DiplomacyHandler(Faction f) {
        this.f = f;
    }

    public double getDiplomaticCapacity() {
        double base = f.getOrCreateMainGuild().getModifier(GuildModifier.DIPLOMATIC_CAPACITY);
        base *= 1+f.getModifier(FactionModifiers.DIPLOMATIC_CAPACITY_MULTIPLIER).getAmount()/100.0;
        base *= f.getGovernment().getStability()/100.0;
        return base;
    }

    public double getUsedDiplomaticCapacity() {
        double used = 0;
        for(Map.Entry<String, Relation> entry : relations.entrySet()) {
            Relation r = entry.getValue();
            Faction from = FactionManager.getByString(entry.getKey());
            if(from == null) continue;
            used += RelationManager.getDiplomaticCost(from, f, r.getType());
        }
        return used;
    }

    public double getAvailableCapacity() {
        return getDiplomaticCapacity() - getUsedDiplomaticCapacity();
    }

    public HashMap<String, Relation> getRelations(){
		return relations;
	}
	
	public Relation getRelation(String s) {
		if(relations.containsKey(s)) return relations.get(s);
		return new Relation();
	}

    public void setRelation(Faction f, Relation r) {
		relations.put(f.getId(), r);
	}
	
	public void updateRelations() {
		for(Map.Entry<String, Relation> entry : relations.entrySet()) {
			entry.getValue().tick();
		}
	}

    public void removeRelation(String s) {
        relations.remove(s);
    }

    public List<FactionModifier> getModifiers() {
        List<FactionModifier> mods = new ArrayList<>();
        for(Map.Entry<String, Relation> entry : relations.entrySet()) {
            Relation r = entry.getValue();
            if(r.getType().hasRecieveModifiers()) {
                for(FactionModifier mod : r.getType().getRecieveModifiers()) {
                    mods.add(new FactionModifier(FactionManager.getByString(entry.getKey()), mod));
                }
            }
            Faction other = FactionManager.getByString(entry.getKey());
            if(other == null) continue;
            Relation back = other.getRelation(f.getId());
            if(back.getType().hasGiveModifiers()) {
                for(FactionModifier mod : back.getType().getGiveModifiers()) {
                    mods.add(new FactionModifier(FactionManager.getByString(entry.getKey()), mod));
                }
            }
        }
        return mods;
    }
}
