package me.Plugins.SimpleFactions.War.battle.engine.core;

import org.bukkit.Location;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.Map.ProvinceGrid;
import me.Plugins.SimpleFactions.SimpleFactions;

public final class BattleProvinceBlockProtectionService {
	public static final String BLOCKED =
			"§cBlock changes are disabled in the battle province while this battle is active.";

	private BattleProvinceBlockProtectionService() {}

	public static boolean isPlayerBlockChangeBlocked(Location location) {
		if (!Cache.battleProvinceBlockProtectionEnabled || location == null) {
			return false;
		}
		int provinceId = resolveProvinceId(location);
		if (provinceId <= 0) {
			return false;
		}
		for (Battle battle : BattleManager.get()) {
			if (!BattleBoundsService.applies(battle)) {
				continue;
			}
			Integer battleProvinceId = battle.getProvinceId();
			if (battleProvinceId != null && battleProvinceId == provinceId) {
				return true;
			}
		}
		return false;
	}

	static int resolveProvinceId(Location location) {
		if (!Cache.mapEnabled || location == null) {
			return -1;
		}
		SimpleFactions plugin = SimpleFactions.getInstance();
		if (plugin == null) {
			return -1;
		}
		ProvinceGrid grid = plugin.getProvinceGrid();
		if (grid == null) {
			return -1;
		}
		return grid.getAt(location.getBlockX(), location.getBlockZ());
	}
}
