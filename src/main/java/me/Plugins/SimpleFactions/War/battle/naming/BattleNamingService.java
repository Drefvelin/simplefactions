package me.Plugins.SimpleFactions.War.battle.naming;

import me.Plugins.SimpleFactions.Loaders.TitleLoader;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Tiers.Title;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.battle.engine.Battle;
import me.Plugins.SimpleFactions.War.battle.enums.BattleType;
import me.Plugins.SimpleFactions.installation.Installation;
import me.Plugins.SimpleFactions.installation.InstallationKind;
import me.Plugins.SimpleFactions.settlement.Settlement;

public final class BattleNamingService {
	public static final String WILDERNESS = "Wilderness";

	private BattleNamingService() {
	}

	public static void applyCampaignName(Battle battle, War war, int provinceId, BattleType type) {
		if (battle == null || type == null) {
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
		if (war == null) {
			return;
		}
		war.recordLocationBattle(resolveLocation(provinceId).key());
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
