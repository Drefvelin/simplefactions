package me.Plugins.SimpleFactions.War.schedule;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.enums.CampaignBattleKind;
import me.Plugins.SimpleFactions.War.progression.CampaignCapabilityService;
import me.Plugins.SimpleFactions.War.progression.CampaignCoalition;
import me.Plugins.SimpleFactions.War.progression.CampaignCoalitionService;
import me.Plugins.SimpleFactions.War.progression.CampaignPushTarget;

public final class CampaignScheduleService {
	private CampaignScheduleService() {
	}

	public static boolean hasSchedule(War war) {
		return war != null
				&& war.getCampaignBattleSchedule() != null
				&& !war.getCampaignBattleSchedule().isEmpty();
	}

	public static Optional<ScheduledCampaignBattle> slotAt(War war, int index) {
		if (war == null || war.getCampaignBattleSchedule() == null) {
			return Optional.empty();
		}
		List<ScheduledCampaignBattle> schedule = war.getCampaignBattleSchedule();
		if (index < 0 || index >= schedule.size()) {
			return Optional.empty();
		}
		return Optional.of(schedule.get(index));
	}

	public static Optional<ScheduledCampaignBattle> slotForProvince(War war, int provinceId) {
		if (!hasSchedule(war)) {
			return Optional.empty();
		}
		for (ScheduledCampaignBattle slot : war.getCampaignBattleSchedule()) {
			if (slot.provinceId() == provinceId) {
				return Optional.of(slot);
			}
		}
		return Optional.empty();
	}

	public static Optional<ScheduledCampaignBattle> currentSlot(War war) {
		if (!hasSchedule(war)) {
			return Optional.empty();
		}
		ensureReSiegeInsert(war);
		return slotAt(war, war.getCampaignScheduleIndex());
	}

	public static void advanceIndex(War war) {
		if (war == null) {
			return;
		}
		war.setCampaignScheduleIndex(war.getCampaignScheduleIndex() + 1);
	}

	public static void insertSiegeAtCurrentIndex(War war, OperationalFort fort) {
		if (war == null || fort == null || fort.id() == null) {
			return;
		}
		List<ScheduledCampaignBattle> schedule = new ArrayList<>(war.getCampaignBattleSchedule());
		int index = Math.max(0, Math.min(war.getCampaignScheduleIndex(), schedule.size()));
		schedule.add(index, new ScheduledCampaignBattle(
				fort.province(),
				CampaignBattleKind.SIEGE,
				false,
				fort.id()));
		war.setCampaignBattleSchedule(schedule);
	}

	public static void ensureReSiegeInsert(War war) {
		ensureReSiegeInsert(war, FortZocIndex.fromGameState());
	}

	static void ensureReSiegeInsert(War war, FortZocIndex fortIndex) {
		if (!hasSchedule(war) || !CampaignCapabilityService.isValidWar(war) || fortIndex == null) {
			return;
		}
		CampaignCoalition advancing = CampaignCoalitionService.getInitiativeHolderCoalition(war);
		if (advancing == null) {
			return;
		}

		int index = war.getCampaignScheduleIndex();
		Optional<ScheduledCampaignBattle> current = slotAt(war, index);

		for (int axisIndex : advancingAxisIndices(war)) {
			int provinceId = war.getCampaignProvinces().get(axisIndex);
			Optional<OperationalFort> fort = fortIndex.fortForProvince(provinceId);
			if (fort.isEmpty()) {
				continue;
			}
			OperationalFort operationalFort = fort.get();
			if (!FortControlService.isEnemyControlled(war, operationalFort.id(), advancing)) {
				continue;
			}
			if (current.isPresent() && isSiegeForFort(current.get(), operationalFort.id())) {
				return;
			}
			if (scheduleAlreadyHasSiegeForFort(war, index, operationalFort.id())) {
				return;
			}
			insertSiegeAtCurrentIndex(war, operationalFort);
			return;
		}
	}

	private static boolean scheduleAlreadyHasSiegeForFort(War war, int fromIndex, String fortInstallationId) {
		List<ScheduledCampaignBattle> schedule = war.getCampaignBattleSchedule();
		for (int i = fromIndex; i < schedule.size(); i++) {
			if (isSiegeForFort(schedule.get(i), fortInstallationId)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isSiegeForFort(ScheduledCampaignBattle slot, String fortInstallationId) {
		if (slot == null || slot.kind() != CampaignBattleKind.SIEGE) {
			return false;
		}
		if (fortInstallationId == null) {
			return slot.fortInstallationId() != null;
		}
		return fortInstallationId.equals(slot.fortInstallationId());
	}

	private static List<Integer> advancingAxisIndices(War war) {
		List<Integer> indices = new ArrayList<>();
		int cursor = war.getCursorIndex();
		CampaignPushTarget pushTarget = CampaignCapabilityService.effectivePushTarget(war);
		int objectiveIndex = CampaignCapabilityService.objectiveIndex(war);

		switch (pushTarget) {
			case TOWARD_OBJECTIVE -> {
				if (objectiveIndex < 0) {
					return indices;
				}
				int step = Integer.compare(objectiveIndex, cursor);
				if (step == 0) {
					indices.add(cursor);
					return indices;
				}
				for (int i = cursor + step; ; i += step) {
					indices.add(i);
					if (i == objectiveIndex) {
						break;
					}
				}
			}
			case TOWARD_AGGRESSOR_CAPITAL -> {
				for (int i = cursor - 1; i >= 0; i--) {
					indices.add(i);
				}
			}
			case RETAKE_OBJECTIVE -> {
				if (objectiveIndex >= 0) {
					indices.add(objectiveIndex);
				}
			}
		}
		return indices;
	}
}
