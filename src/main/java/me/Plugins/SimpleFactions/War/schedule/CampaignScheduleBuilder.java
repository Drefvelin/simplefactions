package me.Plugins.SimpleFactions.War.schedule;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.Managers.ProvinceManager;
import me.Plugins.SimpleFactions.Managers.TitleManager;
import me.Plugins.SimpleFactions.Map.Provinces.Province;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.War.Side;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.enums.CampaignBattleKind;
import me.Plugins.SimpleFactions.War.progression.CampaignCoalition;
import me.Plugins.SimpleFactions.War.progression.CampaignCoalitionService;
import me.Plugins.SimpleFactions.enums.Terrain;

public final class CampaignScheduleBuilder {
	private CampaignScheduleBuilder() {
	}

	public static List<ScheduledCampaignBattle> build(
			War war,
			List<Integer> axis,
			int borderStartIndex,
			int objectiveIndex,
			FortZocIndex fortIndex) {
		return build(war, axis, borderStartIndex, objectiveIndex, fortIndex, null);
	}

	public static List<ScheduledCampaignBattle> build(
			War war,
			List<Integer> axis,
			int borderStartIndex,
			int objectiveIndex,
			FortZocIndex fortIndex,
			PortSeaZocIndex portIndex) {
		if (war == null || axis == null || axis.isEmpty() || fortIndex == null) {
			return List.of();
		}
		if (borderStartIndex < 0
				|| borderStartIndex >= axis.size()
				|| objectiveIndex < 0
				|| objectiveIndex >= axis.size()) {
			return List.of();
		}

		ProvinceManager provinceManager = SimpleFactions.getInstance().getProvinceManager();
		List<ScheduledCampaignBattle> schedule = new ArrayList<>();
		Set<String> scheduledFortIds = new HashSet<>();
		Set<String> scheduledPortIds = new HashSet<>();
		int cadence = Math.max(1, Cache.warProvincesBetweenBattles);
		int step = Integer.compare(objectiveIndex, borderStartIndex);

		appendSeaCrossingSlots(
				war,
				fortIndex,
				portIndex,
				provinceManager,
				schedule,
				scheduledFortIds,
				scheduledPortIds,
				axis,
				borderStartIndex,
				objectiveIndex);

		for (int i = borderStartIndex; ; i += step) {
			appendAxisStep(
					war,
					fortIndex,
					provinceManager,
					schedule,
					scheduledFortIds,
					axis,
					borderStartIndex,
					cadence,
					i);
			if (i == objectiveIndex) {
				break;
			}
		}

		int objectiveProvince = axis.get(objectiveIndex);
		ensureObjectiveSlot(schedule, objectiveProvince);
		return List.copyOf(schedule);
	}

	private static void appendAxisStep(
			War war,
			FortZocIndex fortIndex,
			ProvinceManager provinceManager,
			List<ScheduledCampaignBattle> schedule,
			Set<String> scheduledFortIds,
			List<Integer> axis,
			int borderStartIndex,
			int cadence,
			int axisIndex) {
		int provinceId = axis.get(axisIndex);

		fortIndex.fortForProvince(provinceId).ifPresent(fort -> {
			addSiegeIfAbsent(war, schedule, scheduledFortIds, fort);
			if (scheduledFortIds.contains(fort.id())) {
				removeInvasionSlot(schedule, provinceId);
			}
		});

		if ((axisIndex - borderStartIndex) % cadence == 0) {
			addFieldIfAbsent(schedule, provinceId);
		}
	}

	private static void appendSeaCrossingSlots(
			War war,
			FortZocIndex fortIndex,
			PortSeaZocIndex portIndex,
			ProvinceManager provinceManager,
			List<ScheduledCampaignBattle> schedule,
			Set<String> scheduledFortIds,
			Set<String> scheduledPortIds,
			List<Integer> axis,
			int borderStartIndex,
			int objectiveIndex) {
		int rangeStart = 0;
		int rangeEnd = Math.max(borderStartIndex, objectiveIndex);
		int step = 1;
		Set<Integer> scheduledInvasionSeaStarts = new HashSet<>();

		for (int i = rangeStart; ; i += step) {
			if (!isSeaRunStart(axis, i, provinceManager)) {
				if (i == rangeEnd) {
					break;
				}
				continue;
			}

			List<Integer> seaRun = collectSeaRun(axis, i, rangeEnd, step, provinceManager);
			if (portIndex != null) {
				for (OperationalPort port : portIndex.portsCoveringSeaProvinces(seaRun)) {
					if (port == null || port.id() == null || scheduledPortIds.contains(port.id())) {
						continue;
					}
					if (!isEnemyPort(war, port)) {
						continue;
					}
					schedule.add(new ScheduledCampaignBattle(
							port.province(),
							CampaignBattleKind.NAVAL,
							false,
							null,
							port.id()));
					scheduledPortIds.add(port.id());
				}
			}

			if (scheduledInvasionSeaStarts.add(i)) {
				resolveInvasionLanding(war, axis, i + seaRun.size(), rangeEnd, provinceManager)
						.ifPresent(landing -> addInvasionSlot(schedule, landing));
			}

			if (i == rangeEnd) {
				break;
			}
		}
	}

	private static OptionalInt resolveInvasionLanding(
			War war,
			List<Integer> axis,
			int afterSeaIndex,
			int rangeEnd,
			ProvinceManager provinceManager) {
		for (int i = afterSeaIndex; i <= rangeEnd; i++) {
			int provinceId = axis.get(i);
			Province province = provinceManager.get(provinceId);
			if (province == null || !province.isValid()) {
				continue;
			}
			if (province.getTerrain() == Terrain.SEA) {
				break;
			}
			if (isDefenderOwned(war, provinceId)) {
				return OptionalInt.of(provinceId);
			}
		}
		return OptionalInt.empty();
	}

	private static void addSiegeIfAbsent(
			War war,
			List<ScheduledCampaignBattle> schedule,
			Set<String> scheduledFortIds,
			OperationalFort fort) {
		if (fort == null || fort.id() == null || scheduledFortIds.contains(fort.id())) {
			return;
		}
		if (!FortControlService.isEnemyControlled(war, fort.id(), CampaignCoalition.AGGRESSOR)) {
			return;
		}
		schedule.add(new ScheduledCampaignBattle(
				fort.province(),
				CampaignBattleKind.SIEGE,
				false,
				fort.id()));
		scheduledFortIds.add(fort.id());
	}

	private static void removeInvasionSlot(List<ScheduledCampaignBattle> schedule, int provinceId) {
		schedule.removeIf(slot -> slot.provinceId() == provinceId
				&& slot.kind() == CampaignBattleKind.NAVAL_INVASION
				&& !slot.required());
	}

	private static void addInvasionOrSiegeSlot(
			War war,
			FortZocIndex fortIndex,
			List<ScheduledCampaignBattle> schedule,
			Set<String> scheduledFortIds,
			int landingProvinceId) {
		if (fortIndex != null) {
			Optional<OperationalFort> fort = fortIndex.fortForProvince(landingProvinceId);
			if (fort.isPresent()) {
				addSiegeIfAbsent(war, schedule, scheduledFortIds, fort.get());
				if (scheduledFortIds.contains(fort.get().id())) {
					removeInvasionSlot(schedule, landingProvinceId);
					return;
				}
			}
		}
		addInvasionSlot(schedule, landingProvinceId);
	}

	private static void addInvasionSlot(List<ScheduledCampaignBattle> schedule, int provinceId) {
		for (int index = 0; index < schedule.size(); index++) {
			ScheduledCampaignBattle slot = schedule.get(index);
			if (slot.provinceId() != provinceId) {
				continue;
			}
			if (slot.kind() == CampaignBattleKind.NAVAL_INVASION) {
				return;
			}
			if (slot.kind() == CampaignBattleKind.FIELD && !slot.required()) {
				schedule.set(index, new ScheduledCampaignBattle(
						provinceId,
						CampaignBattleKind.NAVAL_INVASION,
						false,
						slot.fortInstallationId(),
						slot.portInstallationId()));
				return;
			}
		}
		schedule.add(new ScheduledCampaignBattle(
				provinceId,
				CampaignBattleKind.NAVAL_INVASION,
				false,
				null,
				null));
	}

	private static boolean isSeaRunStart(List<Integer> axis, int axisIndex, ProvinceManager provinceManager) {
		Province province = provinceManager.get(axis.get(axisIndex));
		if (province == null || province.getTerrain() != Terrain.SEA) {
			return false;
		}
		if (axisIndex == 0) {
			return true;
		}
		Province previous = provinceManager.get(axis.get(axisIndex - 1));
		return previous == null || previous.getTerrain() != Terrain.SEA;
	}

	private static List<Integer> collectSeaRun(
			List<Integer> axis,
			int startIndex,
			int objectiveIndex,
			int step,
			ProvinceManager provinceManager) {
		List<Integer> seaRun = new ArrayList<>();
		for (int i = startIndex; ; i += step) {
			Province province = provinceManager.get(axis.get(i));
			if (province == null || province.getTerrain() != Terrain.SEA) {
				break;
			}
			seaRun.add(axis.get(i));
			if (i == objectiveIndex) {
				break;
			}
		}
		return seaRun;
	}

	private static boolean isEnemyPort(War war, OperationalPort port) {
		if (war == null || port == null || port.owner() == null) {
			return false;
		}
		Side ownerSide = war.getSide(port.owner());
		if (ownerSide == null) {
			return false;
		}
		CampaignCoalition coalition = CampaignCoalitionService.coalitionOf(war, ownerSide);
		return coalition != null && coalition != CampaignCoalition.AGGRESSOR;
	}

	private static boolean isDefenderOwned(War war, int provinceId) {
		if (war == null) {
			return false;
		}
		Faction owner = TitleManager.getByProvince(provinceId);
		if (owner == null) {
			return false;
		}
		Side ownerSide = war.getSide(owner);
		if (ownerSide == null) {
			return false;
		}
		return CampaignCoalitionService.coalitionOf(war, ownerSide) == CampaignCoalition.DEFENDER;
	}

	private static void addFieldIfAbsent(List<ScheduledCampaignBattle> schedule, int provinceId) {
		for (ScheduledCampaignBattle slot : schedule) {
			if (slot.provinceId() == provinceId && slot.kind() == CampaignBattleKind.FIELD) {
				return;
			}
			if (slot.provinceId() == provinceId && slot.kind() == CampaignBattleKind.NAVAL_INVASION) {
				return;
			}
		}
		schedule.add(new ScheduledCampaignBattle(provinceId, CampaignBattleKind.FIELD, false, null));
	}

	private static void ensureObjectiveSlot(List<ScheduledCampaignBattle> schedule, int objectiveProvince) {
		for (int index = 0; index < schedule.size(); index++) {
			ScheduledCampaignBattle slot = schedule.get(index);
			if (slot.provinceId() != objectiveProvince) {
				continue;
			}
			if (slot.kind() == CampaignBattleKind.FIELD) {
				if (!slot.required()) {
					schedule.set(index, new ScheduledCampaignBattle(
							slot.provinceId(),
							CampaignBattleKind.FIELD,
							true,
							slot.fortInstallationId(),
							slot.portInstallationId()));
				}
				return;
			}
			if (slot.kind() == CampaignBattleKind.SIEGE
					|| slot.kind() == CampaignBattleKind.NAVAL
					|| slot.kind() == CampaignBattleKind.NAVAL_INVASION) {
				schedule.add(new ScheduledCampaignBattle(objectiveProvince, CampaignBattleKind.FIELD, true, null));
				return;
			}
		}
		schedule.add(new ScheduledCampaignBattle(objectiveProvince, CampaignBattleKind.FIELD, true, null));
	}
}
