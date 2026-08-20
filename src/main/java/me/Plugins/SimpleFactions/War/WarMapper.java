package me.Plugins.SimpleFactions.War;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.Database.ParticipantData;
import me.Plugins.SimpleFactions.Database.SideData;
import me.Plugins.SimpleFactions.Database.WarData;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.enums.BattleSchedulePhase;
import me.Plugins.SimpleFactions.War.enums.CampaignPhase;
import me.Plugins.SimpleFactions.War.enums.ObjectiveHolder;
import me.Plugins.SimpleFactions.War.enums.WarEndReason;
import me.Plugins.SimpleFactions.War.enums.WarGoalType;
import me.Plugins.SimpleFactions.War.enums.WarStatus;
import me.Plugins.SimpleFactions.War.enums.WarType;

public final class WarMapper {
	private WarMapper() {}

	public static WarData toData(War war) {
		WarData data = new WarData();
		data.schemaVersion = war.getSchemaVersion();
		data.id = war.getId();
		data.status = war.getStatus().toJson();
		if (war.getGoal() != null) {
			data.goal = war.getGoal().toJson();
		}
		if (war.getWarType() != null) {
			data.warType = war.getWarType().toJson();
		}
		data.attackerLeaderId = war.getAttackerLeaderId();
		data.defenderLeaderId = war.getDefenderLeaderId();
		data.targetTitleId = war.getTargetTitleId();
		data.subjectFactionId = war.getSubjectFactionId();
		data.objectiveProvinceId = war.getObjectiveProvinceId();
		data.campaignStartProvinceId = war.getCampaignStartProvinceId();
		data.campaignProvinces = war.getCampaignProvinces() == null
				? null
				: new ArrayList<>(war.getCampaignProvinces());
		data.cursorIndex = war.getCursorIndex();
		data.initiativeAttacker = war.getInitiativeAttacker();
		data.initiativeDefender = war.getInitiativeDefender();
		data.occupiedByAttacker = war.getOccupiedByAttacker() == null
				? new ArrayList<>()
				: new ArrayList<>(war.getOccupiedByAttacker());
		data.occupiedByDefender = war.getOccupiedByDefender() == null
				? new ArrayList<>()
				: new ArrayList<>(war.getOccupiedByDefender());
		data.lastBattleOccupied = war.getLastBattleOccupied() == null
				? new ArrayList<>()
				: new ArrayList<>(war.getLastBattleOccupied());
		data.campaignPhase = war.getCampaignPhase() != null ? war.getCampaignPhase().toJson() : CampaignPhase.INVASION.toJson();
		data.objectiveHeldBy = war.getObjectiveHeldBy() != null ? war.getObjectiveHeldBy().toJson() : ObjectiveHolder.DEFENDER.toJson();
		data.whitePeaceProposedByAttacker = war.isWhitePeaceProposedByAttacker();
		data.whitePeaceProposedByDefender = war.isWhitePeaceProposedByDefender();
		data.campaignBattlesFought = war.getCampaignBattlesFought();
		data.battleSchedulePhase = war.getBattleSchedulePhase() != null
				? war.getBattleSchedulePhase().toJson()
				: BattleSchedulePhase.IDLE.toJson();
		if (war.getBattleDay() != null) {
			data.battleDay = war.getBattleDay().toString();
		}
		if (war.getScheduledBattleAt() != null) {
			data.scheduledBattleAt = war.getScheduledBattleAt().toString();
		}
		data.scheduledBattleHour = war.getScheduledBattleHour() > 0 ? war.getScheduledBattleHour() : null;
		data.scheduledBattleProvinceId = war.getScheduledBattleProvinceId();
		data.battleVotes = serializeBattleVotes(war.getBattleVotes());
		data.autoresolveProposedByAttacker = war.isAutoresolveProposedByAttacker();
		data.autoresolveProposedByDefender = war.isAutoresolveProposedByDefender();
		data.postponementsThisCycle = war.getPostponementsThisCycle();
		data.defenderChoiceResolved = war.isDefenderChoiceResolved();
		if (war.getStartedAt() != null) {
			data.startedAt = war.getStartedAt().toString();
		}
		if (war.getEndedAt() != null) {
			data.endedAt = war.getEndedAt().toString();
		}
		if (war.getEndReason() != null) {
			data.endReason = war.getEndReason().toJson();
		}
		data.attackers = serializeSide(war.getAttackers());
		data.defenders = serializeSide(war.getDefenders());
		return data;
	}

	public static War fromData(WarData data) {
		if (data == null || data.attackers == null || data.defenders == null) return null;

		Faction atkLeader = FactionManager.getByString(data.attackers.leader);
		Faction defLeader = FactionManager.getByString(data.defenders.leader);
		if (atkLeader == null || defLeader == null) return null;

		War war = new War(data.id, atkLeader, defLeader);
		war.setSchemaVersion(data.schemaVersion > 0 ? data.schemaVersion : 2);

		war.setGoal(WarGoalType.fromJson(data.goal));
		war.setWarType(WarType.fromJson(data.warType));
		war.setStatus(WarStatus.fromJson(data.status));
		if (data.attackerLeaderId != null) {
			war.setAttackerLeaderId(data.attackerLeaderId);
		}
		if (data.defenderLeaderId != null) {
			war.setDefenderLeaderId(data.defenderLeaderId);
		}
		war.setTargetTitleId(data.targetTitleId);
		war.setSubjectFactionId(data.subjectFactionId);
		war.setObjectiveProvinceId(data.objectiveProvinceId);
		war.setCampaignStartProvinceId(data.campaignStartProvinceId);
		war.setCampaignProvinces(data.campaignProvinces == null ? null : new ArrayList<>(data.campaignProvinces));
		war.setCursorIndex(data.cursorIndex);
		war.setInitiativeAttacker(data.initiativeAttacker != null ? data.initiativeAttacker : Cache.warInitiativePerSide);
		war.setInitiativeDefender(data.initiativeDefender != null ? data.initiativeDefender : Cache.warInitiativePerSide);
		war.setOccupiedByAttacker(data.occupiedByAttacker == null ? new ArrayList<>() : new ArrayList<>(data.occupiedByAttacker));
		war.setOccupiedByDefender(data.occupiedByDefender == null ? new ArrayList<>() : new ArrayList<>(data.occupiedByDefender));
		war.setLastBattleOccupied(data.lastBattleOccupied == null ? new ArrayList<>() : new ArrayList<>(data.lastBattleOccupied));
		CampaignPhase phase = CampaignPhase.fromJson(data.campaignPhase);
		war.setCampaignPhase(phase != null ? phase : CampaignPhase.INVASION);
		ObjectiveHolder holder = ObjectiveHolder.fromJson(data.objectiveHeldBy);
		war.setObjectiveHeldBy(holder != null ? holder : ObjectiveHolder.DEFENDER);
		war.setWhitePeaceProposedByAttacker(data.whitePeaceProposedByAttacker);
		war.setWhitePeaceProposedByDefender(data.whitePeaceProposedByDefender);
		war.setCampaignBattlesFought(data.campaignBattlesFought != null ? data.campaignBattlesFought : 0);
		BattleSchedulePhase schedulePhase = BattleSchedulePhase.fromJson(data.battleSchedulePhase);
		war.setBattleSchedulePhase(schedulePhase != null ? schedulePhase : BattleSchedulePhase.IDLE);
		if (data.battleDay != null && !data.battleDay.isBlank()) {
			war.setBattleDay(LocalDate.parse(data.battleDay));
		}
		if (data.scheduledBattleAt != null && !data.scheduledBattleAt.isBlank()) {
			war.setScheduledBattleAt(Instant.parse(data.scheduledBattleAt));
		}
		war.setScheduledBattleHour(data.scheduledBattleHour != null ? data.scheduledBattleHour : 0);
		war.setScheduledBattleProvinceId(data.scheduledBattleProvinceId);
		war.setBattleVotes(deserializeBattleVotes(data.battleVotes));
		war.setAutoresolveProposedByAttacker(data.autoresolveProposedByAttacker);
		war.setAutoresolveProposedByDefender(data.autoresolveProposedByDefender);
		war.setPostponementsThisCycle(data.postponementsThisCycle != null ? data.postponementsThisCycle : 0);
		war.setDefenderChoiceResolved(data.defenderChoiceResolved);
		if (data.startedAt != null) {
			war.setStartedAt(Instant.parse(data.startedAt));
		}
		if (data.endedAt != null) {
			war.setEndedAt(Instant.parse(data.endedAt));
		}
		war.setEndReason(WarEndReason.fromJson(data.endReason));

		war.getAttackers().getMainParticipants().clear();
		war.getDefenders().getMainParticipants().clear();
		loadParticipants(data.attackers, war.getAttackers());
		loadParticipants(data.defenders, war.getDefenders());

		return war;
	}

	private static SideData serializeSide(Side side) {
		SideData data = new SideData();
		data.leader = side.getLeader().getId();
		for (Participant participant : side.getMainParticipants()) {
			data.participants.add(serializeParticipant(participant));
		}
		return data;
	}

	private static ParticipantData serializeParticipant(Participant participant) {
		ParticipantData data = new ParticipantData();
		data.leader = participant.getLeader().getId();
		data.civilWar = participant.isCivilWar();

		for (Faction subject : participant.getSubjects()) {
			data.subjects.add(subject.getId());
		}
		for (Map.Entry<Faction, Boolean> entry : participant.getAllies().entrySet()) {
			data.allies.put(entry.getKey().getId(), entry.getValue());
		}
		return data;
	}

	private static void loadParticipants(SideData data, Side side) {
		for (ParticipantData participantData : data.participants) {
			Faction leader = FactionManager.getByString(participantData.leader);
			if (leader == null) continue;

			List<Faction> subjects = new ArrayList<>();
			for (String id : participantData.subjects) {
				Faction faction = FactionManager.getByString(id);
				if (faction != null) subjects.add(faction);
			}

			Map<Faction, Boolean> allies = new HashMap<>();
			for (Map.Entry<String, Boolean> entry : participantData.allies.entrySet()) {
				Faction faction = FactionManager.getByString(entry.getKey());
				if (faction != null) allies.put(faction, entry.getValue());
			}

			Participant participant = new Participant(leader, subjects, allies, new HashMap<>(), participantData.civilWar);
			side.getMainParticipants().add(participant);
		}
	}

	private static Map<String, List<Integer>> serializeBattleVotes(Map<UUID, Set<Integer>> votes) {
		Map<String, List<Integer>> serialized = new LinkedHashMap<>();
		if (votes == null) {
			return serialized;
		}
		for (Map.Entry<UUID, Set<Integer>> entry : votes.entrySet()) {
			if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) {
				continue;
			}
			serialized.put(entry.getKey().toString(), new ArrayList<>(entry.getValue()));
		}
		return serialized;
	}

	private static Map<UUID, Set<Integer>> deserializeBattleVotes(Map<String, List<Integer>> votes) {
		Map<UUID, Set<Integer>> deserialized = new HashMap<>();
		if (votes == null) {
			return deserialized;
		}
		for (Map.Entry<String, List<Integer>> entry : votes.entrySet()) {
			if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null) {
				continue;
			}
			try {
				UUID playerId = UUID.fromString(entry.getKey());
				deserialized.put(playerId, new HashSet<>(entry.getValue()));
			} catch (IllegalArgumentException ignored) {
				// skip invalid UUID keys from corrupted saves
			}
		}
		return deserialized;
	}
}
