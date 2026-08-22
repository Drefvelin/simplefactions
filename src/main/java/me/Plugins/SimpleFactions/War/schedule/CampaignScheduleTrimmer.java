package me.Plugins.SimpleFactions.War.schedule;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.War.enums.CampaignBattleKind;
import me.Plugins.SimpleFactions.War.enums.WarGoalType;

public final class CampaignScheduleTrimmer {
	private static final int DEFAULT_MAX_BATTLES = 4;
	private static final Logger LOGGER = Logger.getLogger(CampaignScheduleTrimmer.class.getName());

	private CampaignScheduleTrimmer() {
	}

	public static int maxBattlesForGoal(WarGoalType goal) {
		if (goal == null) {
			return DEFAULT_MAX_BATTLES;
		}
		Integer configured = Cache.warGoalMaxBattles.get(goal);
		return configured != null && configured > 0 ? configured : DEFAULT_MAX_BATTLES;
	}

	public static List<ScheduledCampaignBattle> trim(List<ScheduledCampaignBattle> schedule, int maxBattles) {
		if (schedule == null || schedule.isEmpty()) {
			return List.of();
		}
		if (maxBattles <= 0) {
			return List.of();
		}
		if (schedule.size() <= maxBattles) {
			return List.copyOf(schedule);
		}

		List<ScheduledCampaignBattle> working = new ArrayList<>(schedule);
		while (working.size() > maxBattles) {
			int dropIndex = findDropIndex(working);
			if (dropIndex < 0) {
				LOGGER.warning("Campaign schedule trim could not drop further slots; size="
						+ working.size() + " max=" + maxBattles);
				break;
			}
			working.remove(dropIndex);
		}
		return List.copyOf(working);
	}

	private static int findDropIndex(List<ScheduledCampaignBattle> schedule) {
		int fieldIndex = findDroppableFieldIndex(schedule);
		if (fieldIndex >= 0) {
			return fieldIndex;
		}
		int navalInvasionIndex = findKindIndex(schedule, CampaignBattleKind.NAVAL_INVASION);
		if (navalInvasionIndex >= 0) {
			return navalInvasionIndex;
		}
		int navalIndex = findKindIndex(schedule, CampaignBattleKind.NAVAL);
		if (navalIndex >= 0) {
			return navalIndex;
		}
		return findKindIndex(schedule, CampaignBattleKind.SIEGE);
	}

	private static int findDroppableFieldIndex(List<ScheduledCampaignBattle> schedule) {
		for (int index = 0; index < schedule.size(); index++) {
			ScheduledCampaignBattle slot = schedule.get(index);
			if (slot.kind() == CampaignBattleKind.FIELD && !slot.required()) {
				return index;
			}
		}
		return -1;
	}

	private static int findKindIndex(List<ScheduledCampaignBattle> schedule, CampaignBattleKind kind) {
		for (int index = 0; index < schedule.size(); index++) {
			ScheduledCampaignBattle slot = schedule.get(index);
			if (slot.required()) {
				continue;
			}
			if (slot.kind() == kind) {
				return index;
			}
		}
		return -1;
	}
}
