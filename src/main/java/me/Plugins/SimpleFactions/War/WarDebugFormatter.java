package me.Plugins.SimpleFactions.War;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import me.Plugins.SimpleFactions.Managers.WarManager;
import me.Plugins.SimpleFactions.War.progression.CampaignProgressionService;

public final class WarDebugFormatter {
	private static final Gson GSON = new GsonBuilder().serializeNulls().create();

	private WarDebugFormatter() {}

	public static List<String> formatStatusLines(War war) {
		Map<String, Object> summary = new LinkedHashMap<>();
		summary.put("id", war.getId());
		summary.put("status", war.getStatus() != null ? war.getStatus().toJson() : null);
		summary.put("goal", war.getGoal() != null ? war.getGoal().toJson() : null);
		summary.put("warType", war.getWarType() != null ? war.getWarType().toJson() : null);
		summary.put("attackerLeaderId", war.getAttackerLeaderId());
		summary.put("defenderLeaderId", war.getDefenderLeaderId());
		summary.put("targetTitleId", war.getTargetTitleId());
		summary.put("subjectFactionId", war.getSubjectFactionId());
		summary.put("objectiveProvinceId", war.getObjectiveProvinceId());
		summary.put("campaignStartProvinceId", war.getCampaignStartProvinceId());
		summary.put("campaignProvinces", war.getCampaignProvinces());
		summary.put("cursorIndex", war.getCursorIndex());
		summary.put("initiativeAttacker", war.getInitiativeAttacker());
		summary.put("initiativeDefender", war.getInitiativeDefender());
		summary.put("campaignPhase", war.getCampaignPhase() != null ? war.getCampaignPhase().toJson() : null);
		summary.put("objectiveHeldBy", war.getObjectiveHeldBy() != null ? war.getObjectiveHeldBy().toJson() : null);
		summary.put("whitePeaceProposedByAttacker", war.isWhitePeaceProposedByAttacker());
		summary.put("whitePeaceProposedByDefender", war.isWhitePeaceProposedByDefender());
		summary.put("campaignBattlesFought", war.getCampaignBattlesFought());
		summary.put("occupiedByAttacker", war.getOccupiedByAttacker());
		summary.put("occupiedByDefender", war.getOccupiedByDefender());
		summary.put("lastBattleOccupied", war.getLastBattleOccupied());
		summary.put("cursorProvinceId", resolveCursorProvinceId(war));
		summary.put("nextBattleNodes", CampaignProgressionService.resolveNextBattleNodes(war));
		summary.put("battleSchedulePhase", war.getBattleSchedulePhase() != null ? war.getBattleSchedulePhase().toJson() : null);
		summary.put("battleDay", war.getBattleDay() != null ? war.getBattleDay().toString() : null);
		summary.put("scheduledBattleAt", war.getScheduledBattleAt() != null ? war.getScheduledBattleAt().toString() : null);
		summary.put("scheduledBattleHour", war.getScheduledBattleHour() > 0 ? war.getScheduledBattleHour() : null);
		summary.put("scheduledBattleProvinceId", war.getScheduledBattleProvinceId());
		summary.put("battleVoteCount", war.getBattleVotes() != null ? war.getBattleVotes().size() : 0);
		summary.put("autoresolveProposedByAttacker", war.isAutoresolveProposedByAttacker());
		summary.put("autoresolveProposedByDefender", war.isAutoresolveProposedByDefender());
		summary.put("postponementsThisCycle", war.getPostponementsThisCycle());
		summary.put("defenderChoiceResolved", war.isDefenderChoiceResolved());
		summary.put("startedAt", war.getStartedAt() != null ? war.getStartedAt().toString() : null);
		summary.put("commitments", WarManager.getCommitmentsForWar(war.getId()).size());
		return List.of("§7" + GSON.toJson(summary));
	}

	private static Integer resolveCursorProvinceId(War war) {
		List<Integer> axis = war.getCampaignProvinces();
		if (axis == null || axis.isEmpty()) {
			return null;
		}
		int index = war.getCursorIndex();
		if (index < 0 || index >= axis.size()) {
			return null;
		}
		return axis.get(index);
	}
}
