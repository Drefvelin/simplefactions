package me.Plugins.SimpleFactions.War.campaign;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.ProvinceManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.enums.BattleSchedulePhase;
import me.Plugins.SimpleFactions.War.enums.CampaignPhase;
import me.Plugins.SimpleFactions.War.enums.ObjectiveHolder;
import me.Plugins.SimpleFactions.War.progression.BelligerentRole;
import me.Plugins.SimpleFactions.War.progression.CampaignCoalition;
import me.Plugins.SimpleFactions.War.progression.CampaignCoalitionService;
import me.Plugins.SimpleFactions.War.progression.CampaignPushTarget;
import me.Plugins.SimpleFactions.War.progression.PostBattleChoicePhase;
import me.Plugins.SimpleFactions.War.enums.WarGoalType;
import me.Plugins.SimpleFactions.War.objective.ObjectiveProvincePicker;
import me.Plugins.SimpleFactions.War.pathfinder.BelligerentTerritory;
import me.Plugins.SimpleFactions.War.pathfinder.PathfinderResult;
import me.Plugins.SimpleFactions.War.pathfinder.ProvincePathfinder;
import me.Plugins.SimpleFactions.War.pathfinder.TitleManagerProvinceOwnerLookup;
import me.Plugins.SimpleFactions.War.schedule.BattleWindowService;
import me.Plugins.SimpleFactions.War.schedule.CampaignScheduleBuilder;
import me.Plugins.SimpleFactions.War.schedule.CampaignScheduleTrimmer;
import me.Plugins.SimpleFactions.War.schedule.FortControlService;
import me.Plugins.SimpleFactions.War.schedule.FortZocIndex;
import me.Plugins.SimpleFactions.War.schedule.PortSeaZocIndex;
import me.Plugins.SimpleFactions.War.schedule.ScheduledCampaignBattle;

public class WarCampaignService {
	private final ObjectiveProvincePicker picker;
	private final ProvincePathfinder pathfinder;

	public WarCampaignService(ProvinceManager pm) {
		this.picker = new ObjectiveProvincePicker(pm);
		this.pathfinder = new ProvincePathfinder(pm, new TitleManagerProvinceOwnerLookup());
	}

	public boolean populateCampaign(War war) {
		if (war == null || !war.isActive()) {
			return false;
		}

		Faction defender = war.getDefenders().getLeader();
		Faction attacker = war.getAttackers().getLeader();
		OptionalInt regionalObjective = picker.pickObjective(war, defender);
		if (regionalObjective.isEmpty()) {
			return false;
		}

		int attackerCapital = attacker.getCapital();
		if (attackerCapital <= 0) {
			return false;
		}

		PathfinderResult borderResult = pathfinder.computeCampaignLine(war, regionalObjective.getAsInt());
		if (!borderResult.isFound()) {
			return false;
		}

		int borderStart = borderResult.getStartProvinceId();
		BelligerentTerritory territory = BelligerentTerritory.fromWar(war, new TitleManagerProvinceOwnerLookup());
		int objective = resolveFinalObjective(
				war,
				defender,
				borderStart,
				regionalObjective.getAsInt(),
				territory);

		PathfinderResult rightSegment = pathfinder.findRouteWithFallback(borderStart, objective, territory);
		if (!rightSegment.isFound()) {
			return false;
		}

		PathfinderResult leftSegment = pathfinder.findRouteWithFallback(attackerCapital, borderStart, territory);
		if (!leftSegment.isFound()) {
			return false;
		}

		List<Integer> axis = mergeAxisPaths(leftSegment.getPath(), rightSegment.getPath());
		int cursorIndex = axis.indexOf(borderStart);
		if (cursorIndex < 0) {
			return false;
		}

		war.setObjectiveProvinceId(objective);
		war.setCampaignStartProvinceId(borderStart);
		war.setCampaignProvinces(axis);
		war.setCursorIndex(cursorIndex);

		int objectiveIndex = axis.indexOf(objective);
		if (objectiveIndex < 0) {
			return false;
		}
		FortControlService.initializeAtDeclare(war);
		List<ScheduledCampaignBattle> natural = CampaignScheduleBuilder.build(
				war,
				axis,
				cursorIndex,
				objectiveIndex,
				FortZocIndex.fromGameState(),
				PortSeaZocIndex.fromGameState());
		List<ScheduledCampaignBattle> trimmed = CampaignScheduleTrimmer.trim(
				natural,
				CampaignScheduleTrimmer.maxBattlesForGoal(war.getGoal()));
		war.setCampaignBattleSchedule(trimmed);
		war.setCampaignScheduleIndex(0);
		applyInitiativeFromSchedule(war, trimmed);

		initProgressionState(war);
		initScheduleState(war);
		return true;
	}

	static int resolveFinalObjective(
			War war,
			Faction defender,
			int borderStart,
			int regionalObjective,
			BelligerentTerritory territory,
			ProvincePathfinder pathfinder) {
		Faction targetFaction = resolveTargetFaction(war, defender);
		if (targetFaction == null) {
			return regionalObjective;
		}

		int factionCapital = targetFaction.getCapital();
		if (factionCapital <= 0 || factionCapital == regionalObjective) {
			return regionalObjective;
		}

		PathfinderResult toRegional = pathfinder.findRouteWithFallback(borderStart, regionalObjective, territory);
		PathfinderResult toCapital = pathfinder.findRouteWithFallback(borderStart, factionCapital, territory);
		if (!toRegional.isFound() || !toCapital.isFound()) {
			return regionalObjective;
		}
		if (toCapital.getTotalCost() < toRegional.getTotalCost()) {
			return factionCapital;
		}
		return regionalObjective;
	}

	private int resolveFinalObjective(
			War war,
			Faction defender,
			int borderStart,
			int regionalObjective,
			BelligerentTerritory territory) {
		return resolveFinalObjective(war, defender, borderStart, regionalObjective, territory, pathfinder);
	}

	static Faction resolveTargetFaction(War war, Faction defender) {
		if (war.getGoal() == WarGoalType.TRANSFER_SUBJECT) {
			String subjectId = war.getSubjectFactionId();
			if (subjectId != null && !subjectId.isBlank()) {
				Faction subject = FactionManager.getByString(subjectId);
				if (subject != null) {
					return subject;
				}
			}
		}
		return defender;
	}

	static List<Integer> mergeAxisPaths(List<Integer> leftPath, List<Integer> rightPath) {
		List<Integer> merged = new ArrayList<>(leftPath);
		if (rightPath.isEmpty()) {
			return merged;
		}
		int startIndex = 0;
		if (!merged.isEmpty() && merged.get(merged.size() - 1).equals(rightPath.get(0))) {
			startIndex = 1;
		}
		for (int i = startIndex; i < rightPath.size(); i++) {
			merged.add(rightPath.get(i));
		}
		return merged;
	}

	static void applyInitiativeFromSchedule(War war, List<ScheduledCampaignBattle> schedule) {
		int size = schedule == null ? 0 : schedule.size();
		int fuel = (int) Math.ceil(size * Cache.warInitiativeFactor);
		war.setInitiativeAttacker(fuel);
		war.setInitiativeDefender(fuel);
	}

	static void initProgressionState(War war) {
		CampaignCoalitionService.setInitiativeHolderCoalition(war, CampaignCoalition.AGGRESSOR);
		war.setPushTarget(CampaignPushTarget.TOWARD_OBJECTIVE);
		war.setPostBattleChoicePhase(PostBattleChoicePhase.NONE);
		war.setPostBattleChoiceResolved(true);
		war.setHoldPeaceProposalActive(false);
		war.setOccupiedByAttacker(new ArrayList<>());
		war.setOccupiedByDefender(new ArrayList<>());
		war.setLastBattleOccupied(new ArrayList<>());
		war.setCampaignPhase(CampaignPhase.INVASION);
		war.setObjectiveHeldBy(ObjectiveHolder.DEFENDER);
		war.setWhitePeaceProposedByAttacker(false);
		war.setWhitePeaceProposedByDefender(false);
		war.setCampaignBattlesFought(0);
		war.setSchemaVersion(CampaignCoalitionService.SCHEMA_VERSION);
	}

	static void initScheduleState(War war) {
		Instant started = war.getStartedAt() != null ? war.getStartedAt() : Instant.now();
		if (Cache.warFirstBattleDayAfterDeclare) {
			war.setBattleDay(started.atZone(BattleWindowService.SCHEDULE_ZONE).toLocalDate().plusDays(1));
		} else {
			war.setBattleDay(started.atZone(BattleWindowService.SCHEDULE_ZONE).toLocalDate());
		}
		war.setBattleSchedulePhase(BattleSchedulePhase.VOTING);
		war.setScheduledBattleAt(null);
		war.setScheduledBattleHour(0);
		war.setScheduledBattleProvinceId(null);
		war.getBattleVotes().clear();
		war.setAutoresolveProposedByAttacker(false);
		war.setAutoresolveProposedByDefender(false);
		war.setPostponementsThisCycle(0);
		war.setDefenderChoiceResolved(false);
	}
}
