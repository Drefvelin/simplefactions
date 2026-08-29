package me.Plugins.SimpleFactions.War.pathfinder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import me.Plugins.SimpleFactions.Map.Provinces.Province;
import me.Plugins.SimpleFactions.Managers.ProvinceManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.core.Participant;
import me.Plugins.SimpleFactions.War.core.Side;
import me.Plugins.SimpleFactions.War.core.War;
import me.Plugins.SimpleFactions.enums.Terrain;

public class BelligerentTerritory {
	private final Set<String> attackerIds;
	private final Set<String> defenderIds;
	private final Set<String> mainDefenderRealmIds;
	private final Set<String> allBelligerentIds;
	private final ProvinceOwnerLookup owners;

	public BelligerentTerritory(
			Set<String> attackerIds,
			Set<String> defenderIds,
			ProvinceOwnerLookup owners) {
		this(attackerIds, defenderIds, defenderIds, owners);
	}

	public BelligerentTerritory(
			Set<String> attackerIds,
			Set<String> defenderIds,
			Set<String> mainDefenderRealmIds,
			ProvinceOwnerLookup owners) {
		this.attackerIds = Set.copyOf(attackerIds);
		this.defenderIds = Set.copyOf(defenderIds);
		this.mainDefenderRealmIds = mainDefenderRealmIds == null || mainDefenderRealmIds.isEmpty()
				? Set.copyOf(defenderIds)
				: Set.copyOf(mainDefenderRealmIds);
		this.allBelligerentIds = union(attackerIds, defenderIds);
		this.owners = owners;
	}

	public static BelligerentTerritory fromWar(War war, ProvinceOwnerLookup owners) {
		Set<String> attackers = collectSideFactionIds(war.getAttackers());
		Set<String> defenders = collectSideFactionIds(war.getDefenders());
		Set<String> mainDefender = collectLeaderParticipantIds(war.getDefenders());
		return new BelligerentTerritory(attackers, defenders, mainDefender, owners);
	}

	private static Set<String> collectSideFactionIds(Side side) {
		Set<String> ids = new HashSet<>();
		if (side == null) {
			return ids;
		}
		for (Participant participant : side.getMainParticipants()) {
			addParticipantIds(ids, participant);
		}
		return ids;
	}

	private static Set<String> collectLeaderParticipantIds(Side side) {
		Set<String> ids = new HashSet<>();
		if (side == null || side.getLeader() == null) {
			return ids;
		}
		String leaderId = normalizeId(side.getLeader().getId());
		Participant leaderParticipant = null;
		for (Participant participant : side.getMainParticipants()) {
			if (participant.getLeader() != null
					&& leaderId.equals(normalizeId(participant.getLeader().getId()))) {
				leaderParticipant = participant;
				break;
			}
		}
		if (leaderParticipant == null && !side.getMainParticipants().isEmpty()) {
			leaderParticipant = side.getMainParticipants().get(0);
		}
		addParticipantIds(ids, leaderParticipant);
		return ids;
	}

	private static void addParticipantIds(Set<String> ids, Participant participant) {
		if (participant == null || participant.getLeader() == null) {
			return;
		}
		ids.add(normalizeId(participant.getLeader().getId()));
		for (Faction subject : participant.getSubjects()) {
			ids.add(normalizeId(subject.getId()));
		}
		for (Map.Entry<Faction, Boolean> ally : participant.getAllies().entrySet()) {
			if (Boolean.TRUE.equals(ally.getValue())) {
				ids.add(normalizeId(ally.getKey().getId()));
			}
		}
	}

	private static Set<String> union(Set<String> a, Set<String> b) {
		Set<String> merged = new HashSet<>(a);
		merged.addAll(b);
		return Set.copyOf(merged);
	}

	private static String normalizeId(String id) {
		return id == null ? null : id.toLowerCase(Locale.ROOT);
	}

	public boolean isAttackerSide(int provinceId) {
		String ownerId = owners.getOwnerFactionId(provinceId);
		return ownerId != null && attackerIds.contains(normalizeId(ownerId));
	}

	public boolean isDefenderSide(int provinceId) {
		String ownerId = owners.getOwnerFactionId(provinceId);
		return ownerId != null && defenderIds.contains(normalizeId(ownerId));
	}

	public boolean isMainDefenderRealm(int provinceId) {
		String ownerId = owners.getOwnerFactionId(provinceId);
		return ownerId != null && mainDefenderRealmIds.contains(normalizeId(ownerId));
	}

	public boolean isWilderness(int provinceId) {
		return owners.getOwnerFactionId(provinceId) == null;
	}

	public boolean isForeignNation(int provinceId) {
		String ownerId = owners.getOwnerFactionId(provinceId);
		if (ownerId == null) {
			return false;
		}
		return !allBelligerentIds.contains(normalizeId(ownerId));
	}

	public boolean isNeutral(int provinceId) {
		return isWilderness(provinceId) || isForeignNation(provinceId);
	}

	public List<Integer> findInvasionEntryProvinces(ProvinceManager pm) {
		List<Integer> entries = new ArrayList<>();
		for (Province province : pm.getProvinces()) {
			if (!isMainDefenderRealm(province.getId())) {
				continue;
			}
			for (int neighbourId : province.getNeighbours()) {
				if (isAttackerSide(neighbourId)) {
					entries.add(province.getId());
					break;
				}
			}
		}
		return entries;
	}

	public List<Integer> findDefenderProvinces(ProvinceManager pm) {
		List<Integer> candidates = new ArrayList<>();
		for (Province province : pm.getProvinces()) {
			if (isMainDefenderRealm(province.getId())) {
				candidates.add(province.getId());
			}
		}
		return candidates;
	}

	public List<Integer> findSeaInvasionEntryProvinces(ProvinceManager pm) {
		List<Integer> entries = new ArrayList<>();
		for (Province province : pm.getProvinces()) {
			if (!isMainDefenderRealm(province.getId())) {
				continue;
			}
			if (isAdjacentToSea(pm, province.getId())) {
				entries.add(province.getId());
			}
		}
		return entries;
	}

	public boolean isAdjacentToSea(ProvinceManager pm, int provinceId) {
		Province province = pm.get(provinceId);
		if (province == null || province.getId() == 0) {
			return false;
		}
		for (int neighbourId : province.getNeighbours()) {
			Province neighbour = pm.get(neighbourId);
			if (neighbour != null && neighbour.getTerrain() == Terrain.SEA) {
				return true;
			}
		}
		return false;
	}
}
