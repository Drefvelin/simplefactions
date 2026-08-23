package me.Plugins.SimpleFactions.War.battle.naming;

import me.Plugins.SimpleFactions.Loaders.TitleLoader;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Tiers.Title;
import java.util.List;

import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.battle.engine.Battle;
import me.Plugins.SimpleFactions.War.battle.enums.BattleType;
import me.Plugins.SimpleFactions.War.enums.CampaignBattleKind;
import me.Plugins.SimpleFactions.War.schedule.CampaignScheduleService;
import me.Plugins.SimpleFactions.War.schedule.CampaignScheduleService.ScheduleLeg;
import me.Plugins.SimpleFactions.War.schedule.ScheduledCampaignBattle;
import me.Plugins.SimpleFactions.installation.Installation;
import me.Plugins.SimpleFactions.installation.InstallationKind;
import me.Plugins.SimpleFactions.settlement.Settlement;

public final class BattleNamingService {
	public static final String WILDERNESS = "Wilderness";

	private BattleNamingService() {
	}

	public static void applyCampaignName(Battle battle, War war, int provinceId, BattleType type) {
		applyCampaignName(battle, war, provinceId, type, null);
	}

	public static void applyCampaignName(
			Battle battle,
			War war,
			int provinceId,
			BattleType type,
			ScheduledCampaignBattle slot) {
		if (battle == null || type == null) {
			return;
		}
		if (slot != null && slot.kind() == CampaignBattleKind.SIEGE && slot.fortInstallationId() != null) {
			String fortName = CampaignScheduleService.resolveInstallationName(slot.fortInstallationId());
			String location = fortName != null && !fortName.isBlank()
					? fortName
					: resolveLocationDisplayName(provinceId);
			String key = locationKeyForSlot(slot, provinceId);
			int ordinal = war != null ? war.getLocationBattleCount(key) + 1 : 1;
			battle.setDisplayName(buildDisplayName(BattleType.SIEGE, location, ordinal));
			return;
		}
		LocationInfo location = resolveLocation(provinceId);
		int ordinal = war != null ? war.getLocationBattleCount(location.key()) + 1 : 1;
		battle.setDisplayName(buildDisplayName(type, location.displayName(), ordinal));
	}

	public static void applyManualName(Battle battle, Integer provinceId, BattleType type) {
		if (battle == null || type == null || provinceId == null) {
			return;
		}
		LocationInfo location = resolveLocation(provinceId);
		battle.setDisplayName(buildDisplayName(type, location.displayName(), 1));
	}

	public static void recordLocationBattle(War war, int provinceId) {
		recordLocationBattle(war, provinceId, null);
	}

	public static void recordLocationBattle(War war, int provinceId, ScheduledCampaignBattle slot) {
		if (war == null) {
			return;
		}
		war.recordLocationBattle(locationKeyForSlot(slot, provinceId));
	}

	public static String locationKeyForSlot(ScheduledCampaignBattle slot, int provinceId) {
		if (slot != null
				&& slot.kind() == CampaignBattleKind.SIEGE
				&& slot.fortInstallationId() != null) {
			return "fort:" + slot.fortInstallationId();
		}
		return resolveLocationKey(provinceId);
	}

	public static int resolveScheduledOrdinal(
			War war,
			ScheduleLeg leg,
			int slotIndex,
			ScheduledCampaignBattle slot) {
		if (war == null || leg == null || slot == null) {
			return 1;
		}
		String key = locationKeyForSlot(slot, slot.provinceId());
		int fought = war.getLocationBattleCount(key);
		int prior = 0;
		List<ScheduledCampaignBattle> schedule = CampaignScheduleService.scheduleListForLeg(war, leg);
		for (int i = 0; i < slotIndex && i < schedule.size(); i++) {
			ScheduledCampaignBattle priorSlot = schedule.get(i);
			if (key.equals(locationKeyForSlot(priorSlot, priorSlot.provinceId()))) {
				prior++;
			}
		}
		return fought + prior + 1;
	}

	public static String resolveScheduledDisplayName(
			War war,
			ScheduleLeg leg,
			int slotIndex,
			ScheduledCampaignBattle slot,
			int provinceId) {
		if (slot == null) {
			return buildDisplayName(BattleType.FIELD, resolveLocationDisplayName(provinceId), 1);
		}
		if (slot.kind() == CampaignBattleKind.SIEGE && slot.fortInstallationId() != null) {
			String fortName = CampaignScheduleService.resolveInstallationName(slot.fortInstallationId());
			String location = fortName != null && !fortName.isBlank()
					? fortName
					: resolveLocationDisplayName(provinceId);
			int ordinal = resolveScheduledOrdinal(war, leg, slotIndex, slot);
			return buildDisplayName(BattleType.SIEGE, location, ordinal);
		}
		String locationDisplay = resolveLocationDisplayName(provinceId);
		int ordinal = resolveScheduledOrdinal(war, leg, slotIndex, slot);
		return buildDisplayName(slot.battleType(), locationDisplay, ordinal);
	}

	public static String buildDisplayName(BattleType type, String locationName, int ordinal) {
		String safeLocation = locationName != null && !locationName.isBlank() ? locationName : WILDERNESS;
		if (type == BattleType.RAID) {
			return safeLocation + " Raid";
		}
		String prefix = ordinalPrefix(ordinal);
		if (type == BattleType.SIEGE) {
			return prefix + "Siege of " + safeLocation;
		}
		return prefix + "Battle of " + safeLocation;
	}

	public static String resolveLocationKey(int provinceId) {
		return resolveLocation(provinceId).key();
	}

	public static String resolveLocationDisplayName(int provinceId) {
		return resolveLocation(provinceId).displayName();
	}

	static LocationInfo resolveLocation(int provinceId) {
		Settlement settlement = findSettlement(provinceId);
		if (settlement != null && settlement.getName() != null && !settlement.getName().isBlank()) {
			return new LocationInfo("settlement:" + settlement.getName(), settlement.getName());
		}
		Installation fort = findInstallation(provinceId, InstallationKind.FORT);
		if (fort != null && fort.getName() != null && !fort.getName().isBlank()) {
			return new LocationInfo("fort:" + fort.getName(), fort.getName());
		}
		Title title = TitleLoader.getByProvince(provinceId);
		if (title != null && title.getName() != null && !title.getName().isBlank()) {
			return new LocationInfo("county:" + title.getName(), title.getName());
		}
		return new LocationInfo("wilderness:" + provinceId, WILDERNESS);
	}

	private static Settlement findSettlement(int provinceId) {
		for (Faction faction : FactionManager.getCopy()) {
			if (faction == null) {
				continue;
			}
			Settlement settlement = faction.getSettlementHandler().getByProvince(provinceId);
			if (settlement != null) {
				return settlement;
			}
		}
		return null;
	}

	private static Installation findInstallation(int provinceId, InstallationKind kind) {
		for (Faction faction : FactionManager.getCopy()) {
			if (faction == null) {
				continue;
			}
			Installation installation = faction.getInstallationHandler().getByProvince(kind, provinceId);
			if (installation != null) {
				return installation;
			}
		}
		return null;
	}

	static String ordinalPrefix(int ordinal) {
		if (ordinal <= 1) {
			return "";
		}
		return switch (ordinal) {
			case 2 -> "Second ";
			case 3 -> "Third ";
			default -> ordinal + "th ";
		};
	}

	record LocationInfo(String key, String displayName) {
	}
}
