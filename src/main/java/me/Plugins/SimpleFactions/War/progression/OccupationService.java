package me.Plugins.SimpleFactions.War.progression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.Managers.ProvinceManager;
import me.Plugins.SimpleFactions.Map.Provinces.Province;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.pathfinder.BelligerentTerritory;
import me.Plugins.SimpleFactions.War.pathfinder.ProvinceOwnerLookup;

public class OccupationService {
	private final ProvinceManager provinceManager;
	private final ProvinceOwnerLookup owners;

	public OccupationService(ProvinceManager provinceManager, ProvinceOwnerLookup owners) {
		this.provinceManager = provinceManager;
		this.owners = owners;
	}

	public OccupationZone computeOccupationZone(War war, int battleProvinceId, BelligerentRole winner) {
		if (!isValidInput(war, battleProvinceId, winner)) {
			return OccupationZone.of(List.of());
		}

		BelligerentTerritory territory = BelligerentTerritory.fromWar(war, owners);
		List<Integer> zone = new ArrayList<>();
		zone.add(battleProvinceId);

		Province battleProvince = provinceManager.get(battleProvinceId);
		if (battleProvince == null || !battleProvince.isValid()) {
			return OccupationZone.of(zone);
		}

		List<Integer> neighbors = new ArrayList<>();
		for (int neighborId : battleProvince.getNeighbours()) {
			if (qualifiesNeighbor(war, battleProvinceId, neighborId, winner, territory)) {
				neighbors.add(neighborId);
			}
		}
		Collections.sort(neighbors);
		zone.addAll(neighbors);
		return OccupationZone.of(zone);
	}

	public boolean applyBattleWin(War war, int battleProvinceId, BelligerentRole winner) {
		if (!isValidInput(war, battleProvinceId, winner)) {
			return false;
		}

		OccupationZone zone = computeOccupationZone(war, battleProvinceId, winner);
		if (zone.provinceIds().isEmpty()) {
			return false;
		}

		if (winner == BelligerentRole.ATTACKER) {
			List<Integer> existing = copyList(war.getOccupiedByAttacker());
			List<Integer> newlyAdded = mergeOccupation(existing, zone);
			war.setOccupiedByAttacker(existing);
			war.setLastBattleOccupied(newlyAdded);
		} else {
			List<Integer> existing = copyList(war.getOccupiedByDefender());
			List<Integer> newlyAdded = mergeOccupation(existing, zone);
			war.setOccupiedByDefender(existing);
			war.setLastBattleOccupied(newlyAdded);
		}
		return true;
	}

	static boolean qualifiesNeighbor(
			War war,
			int battleProvinceId,
			int neighborId,
			BelligerentRole winner,
			BelligerentTerritory territory) {
		if (neighborId == battleProvinceId) {
			return false;
		}
		if (isOnCampaignLine(war, neighborId)) {
			return true;
		}
		if (isAlreadyOccupied(war, neighborId)) {
			return true;
		}
		if (Cache.warOccupationIncludeEnemyNeighbors && isEnemyOwned(neighborId, winner, territory)) {
			return true;
		}
		return false;
	}

	static List<Integer> mergeOccupation(List<Integer> existing, OccupationZone zone) {
		Set<Integer> seen = new HashSet<>(existing);
		List<Integer> newlyAdded = new ArrayList<>();
		for (int provinceId : zone.provinceIds()) {
			if (seen.add(provinceId)) {
				existing.add(provinceId);
				newlyAdded.add(provinceId);
			}
		}
		return newlyAdded;
	}

	private static boolean isValidInput(War war, int battleProvinceId, BelligerentRole winner) {
		if (war == null || !war.isActive() || winner == null || battleProvinceId <= 0) {
			return false;
		}
		List<Integer> campaign = war.getCampaignProvinces();
		return campaign != null && !campaign.isEmpty();
	}

	private static boolean isOnCampaignLine(War war, int provinceId) {
		List<Integer> campaign = war.getCampaignProvinces();
		return campaign != null && campaign.contains(provinceId);
	}

	private static boolean isAlreadyOccupied(War war, int provinceId) {
		return containsProvince(war.getOccupiedByAttacker(), provinceId)
				|| containsProvince(war.getOccupiedByDefender(), provinceId);
	}

	private static boolean containsProvince(List<Integer> provinces, int provinceId) {
		return provinces != null && provinces.contains(provinceId);
	}

	private static boolean isEnemyOwned(int provinceId, BelligerentRole winner, BelligerentTerritory territory) {
		return switch (winner) {
			case ATTACKER -> territory.isDefenderSide(provinceId);
			case DEFENDER -> territory.isAttackerSide(provinceId);
		};
	}

	private static List<Integer> copyList(List<Integer> source) {
		return source == null ? new ArrayList<>() : new ArrayList<>(source);
	}
}
