package me.Plugins.SimpleFactions.Utils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.enums.RankType;

public class FactionRanker {
	public Integer getPrestigeRank(Faction f) {
		List<Faction> factions = getRankedList(RankType.PRESTIGE);
		Collections.reverse(factions);
		for(int i = 0; i<factions.size(); i++) {
			if(f.getId().equals(factions.get(i).getId())) return i+1;
		}
		return 0;
	}
	public Integer getWealthRank(Faction f) {
		List<Faction> factions = getRankedList(RankType.WEALTH);
		Collections.reverse(factions);
		for(int i = 0; i<factions.size(); i++) {
			if(f.getId().equals(factions.get(i).getId())) return i+1;
		}
		return 0;
	}
	public List<Faction> getRankedList(RankType t){
		List<Faction> f = FactionManager.getCopy();
		if(t.equals(RankType.PRESTIGE)) {
			Collections.sort(f, new Comparator<Faction>() {
			    @Override
			    public int compare(Faction f1, Faction f2) {
			        return Double.compare(f1.getPrestige(), f2.getPrestige());
			    }
			});
		} else if(t.equals(RankType.WEALTH)){
			Collections.sort(f, new Comparator<Faction>() {
			    @Override
			    public int compare(Faction f1, Faction f2) {
			        return Double.compare(f1.getWealth(), f2.getWealth());
			    }
			});
		} else if(t.equals(RankType.MEMBERS)){
			Collections.sort(f, new Comparator<Faction>() {
			    @Override
			    public int compare(Faction f1, Faction f2) {
			        return Integer.compare(f1.getMembers().size(), f2.getMembers().size());
			    }
			});
		}
		return f;
	}
}
