package me.Plugins.SimpleFactions.War.campaign.runtime;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.core.Side;
import me.Plugins.SimpleFactions.War.core.War;
import me.Plugins.SimpleFactions.War.campaign.runtime.InstallationPickResults.InstallationPickToggleResult;
import me.Plugins.SimpleFactions.installation.Installation;
import me.Plugins.SimpleFactions.installation.handler.InstallationHandler;

public final class BattleInstallationPickService {
	private BattleInstallationPickService() {}

	public static InstallationPickToggleResult togglePick(
			War war,
			Faction faction,
			String playerName,
			String installationId) {
		return togglePick(war, faction, playerName, installationId, CampaignClock.now());
	}

	public static InstallationPickToggleResult togglePick(
			War war,
			Faction faction,
			String playerName,
			String installationId,
			Instant now) {
		if (war == null || !war.isActive()) {
			return InstallationPickToggleResult.REJECTED_WAR_INACTIVE;
		}
		if (faction == null || war.getSide(faction) == null) {
			return InstallationPickToggleResult.REJECTED_NOT_PARTICIPANT;
		}
		if (playerName == null || !faction.isLeader(playerName)) {
			return InstallationPickToggleResult.REJECTED_NOT_LEADER;
		}
		if (installationId == null || installationId.isBlank()) {
			return InstallationPickToggleResult.REJECTED_INVALID_INSTALLATION;
		}
		if (isLocked(war, now)) {
			return InstallationPickToggleResult.REJECTED_LOCKED;
		}
		if (war.getBattleDay() == null) {
			return InstallationPickToggleResult.REJECTED_WAR_INACTIVE;
		}

		syncBattleDay(war);

		Installation installation = faction.getInstallationHandler().getById(installationId);
		if (installation == null) {
			return InstallationPickToggleResult.REJECTED_INVALID_INSTALLATION;
		}

		String factionId = faction.getId();
		Map<String, LinkedHashSet<String>> picks = war.getBattleInstallationPicks();
		LinkedHashSet<String> factionPicks = picks.computeIfAbsent(factionId, ignored -> new LinkedHashSet<>());
		if (factionPicks.contains(installationId)) {
			factionPicks.remove(installationId);
			if (factionPicks.isEmpty()) {
				picks.remove(factionId);
			}
			pruneEmptyPicks(war);
			return InstallationPickToggleResult.REMOVED;
		}

		if (!BattleInstallationPickEligibility.isPickable(war, faction, installation)) {
			return InstallationPickToggleResult.REJECTED_INVALID_INSTALLATION;
		}

		if (war.getBattleInstallationPicksBattleDay() == null) {
			war.setBattleInstallationPicksBattleDay(war.getBattleDay());
		}
		factionPicks.add(installationId);
		return InstallationPickToggleResult.ADDED;
	}

	public static Set<String> getPicks(War war, String factionId) {
		if (war == null || factionId == null || factionId.isBlank()) {
			return Set.of();
		}
		syncBattleDay(war);
		pruneIneligiblePicks(war);
		LinkedHashSet<String> picks = war.getBattleInstallationPicks().get(factionId);
		if (picks == null || picks.isEmpty()) {
			return Set.of();
		}
		return Collections.unmodifiableSet(new LinkedHashSet<>(picks));
	}

	public static Map<String, Set<String>> getAllPicks(War war) {
		if (war == null) {
			return Map.of();
		}
		syncBattleDay(war);
		pruneIneligiblePicks(war);
		Map<String, Set<String>> copy = new LinkedHashMap<>();
		for (Map.Entry<String, LinkedHashSet<String>> entry : war.getBattleInstallationPicks().entrySet()) {
			if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) {
				continue;
			}
			copy.put(entry.getKey(), Collections.unmodifiableSet(new LinkedHashSet<>(entry.getValue())));
		}
		return Collections.unmodifiableMap(copy);
	}

	public static Map<String, Set<String>> getVisibleEnemyPicks(War war, String viewerFactionId) {
		return getVisibleEnemyPicks(war, viewerFactionId, CampaignClock.now());
	}

	public static Map<String, Set<String>> getVisibleEnemyPicks(
			War war,
			String viewerFactionId,
			Instant now) {
		if (war == null || viewerFactionId == null || viewerFactionId.isBlank()) {
			return Map.of();
		}
		Faction viewer = FactionManager.getByString(viewerFactionId);
		if (viewer == null || !war.isParticipating(viewer)) {
			return Map.of();
		}
		if (!isLocked(war, now)) {
			return Map.of();
		}
		Side enemySide = war.getOppositeSide(viewer);
		if (enemySide == null) {
			return Map.of();
		}
		Map<String, Set<String>> result = new LinkedHashMap<>();
		for (Faction enemy : BattleSideMembers.collectParticipatingFactions(enemySide)) {
			if (enemy == null || enemy.getId() == null) {
				continue;
			}
			result.put(enemy.getId(), getPicks(war, enemy.getId()));
		}
		return Collections.unmodifiableMap(result);
	}

	public static boolean isLocked(War war, Instant now) {
		return BattleScheduleService.isVoteCloseDue(war, now);
	}

	public static void clearForNewBattleDay(War war) {
		if (war == null) {
			return;
		}
		war.getBattleInstallationPicks().clear();
		war.setBattleInstallationPicksBattleDay(null);
	}

	static void syncBattleDay(War war) {
		if (war == null) {
			return;
		}
		LocalDate battleDay = war.getBattleDay();
		LocalDate picksDay = war.getBattleInstallationPicksBattleDay();
		if (picksDay != null && battleDay != null && !picksDay.equals(battleDay)) {
			clearForNewBattleDay(war);
		}
	}

	private static void pruneIneligiblePicks(War war) {
		for (Map.Entry<String, LinkedHashSet<String>> entry : war.getBattleInstallationPicks().entrySet()) {
			String factionId = entry.getKey();
			LinkedHashSet<String> factionPicks = entry.getValue();
			if (factionId == null || factionPicks == null || factionPicks.isEmpty()) {
				continue;
			}
			Faction faction = FactionManager.getByString(factionId);
			if (faction == null) {
				continue;
			}
			InstallationHandler handler = faction.getInstallationHandler();
			if (handler == null) {
				continue;
			}
			factionPicks.removeIf(installationId -> {
				Installation installation = handler.getById(installationId);
				return installation == null || !BattleInstallationPickEligibility.isPickable(war, faction, installation);
			});
		}
		war.getBattleInstallationPicks().entrySet().removeIf(
				entry -> entry.getValue() == null || entry.getValue().isEmpty());
		pruneEmptyPicks(war);
	}

	private static void pruneEmptyPicks(War war) {
		if (war.getBattleInstallationPicks().isEmpty()) {
			war.setBattleInstallationPicksBattleDay(null);
		}
	}
}
