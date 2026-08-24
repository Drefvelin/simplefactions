package me.Plugins.SimpleFactions.War.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.Plugins.SimpleFactions.Loaders.TitleLoader;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.RelationManager;
import me.Plugins.SimpleFactions.Managers.TitleManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Tiers.Title;
import me.Plugins.SimpleFactions.War.enums.WarGoalType;
import me.Plugins.SimpleFactions.War.enums.WarType;

public final class WarDeclareHelper {
	private WarDeclareHelper() {}

	public static WarType warTypeForGoal(WarGoalType goal) {
		return switch (goal) {
			case DE_JURE_ANNEX -> WarType.DE_JURE;
			case SUBJUGATE -> WarType.SUBJUGATE;
			case TRANSFER_SUBJECT -> WarType.TRANSFER_SUBJECT;
		};
	}

	public static List<Title> eligibleDeJureTitles(Faction attacker, Faction defender) {
		List<Title> eligible = new ArrayList<>();
		if (attacker == null || defender == null) return eligible;

		Set<Integer> settlementProbes = collectSettlementProvinces();
		Set<Integer> capitals = collectCapitalProvinces();

		for (Title title : TitleLoader.getTitles()) {
			if (!canAnnexByRank(attacker.getTier().getTier(), title.getTier().getTier())) continue;

			Faction owner = TitleManager.getOwner(title);
			if (owner == null || !owner.getId().equalsIgnoreCase(defender.getId())) continue;

			List<Integer> titleProvinces = TitleManager.getProvinces(title);
			int ownedInTitle = title.nestedProvinceCheck(TitleManager.getProvinces(attacker), titleProvinces);
			if (ownedInTitle < 1) continue;
			if (title.canBeHeld(attacker)) continue;

			Set<Integer> provinceSet = new HashSet<>(titleProvinces);
			if (titleProvincesContainSettlement(provinceSet, settlementProbes, capitals)) continue;

			eligible.add(title);
		}
		return eligible;
	}

	public static List<Faction> defenderSubjects(Faction defender) {
		return RelationManager.getSubjects(defender);
	}

	static boolean canAnnexByRank(int attackerTierLevel, int titleTierLevel) {
		return attackerTierLevel >= titleTierLevel;
	}

	private static boolean titleProvincesContainSettlement(
			Set<Integer> titleProvinces,
			Set<Integer> settlementProbes,
			Set<Integer> capitals) {
		for (Integer province : settlementProbes) {
			if (titleProvinces.contains(province)) return true;
		}
		for (Integer capital : capitals) {
			if (capital != null && capital > 0 && titleProvinces.contains(capital)) return true;
		}
		return false;
	}

	private static Set<Integer> collectSettlementProvinces() {
		Set<Integer> provinces = new HashSet<>();
		for (Faction faction : FactionManager.factions) {
			faction.getSettlementHandler().getAll().forEach(s -> provinces.add(s.getCenterProvince()));
		}
		return provinces;
	}

	private static Set<Integer> collectCapitalProvinces() {
		Set<Integer> provinces = new HashSet<>();
		for (Faction faction : FactionManager.factions) {
			int capital = faction.getCapital();
			if (capital > 0) provinces.add(capital);
		}
		return provinces;
	}
}
