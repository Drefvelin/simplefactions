package me.Plugins.SimpleFactions.War.battle.engine;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import me.Plugins.SimpleFactions.Map.ProvinceGrid;
import me.Plugins.SimpleFactions.Map.Provinces.Province;
import me.Plugins.SimpleFactions.Managers.ProvinceManager;
import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.War.battle.enums.BattleType;
import me.Plugins.SimpleFactions.War.battle.template.BattleTemplate;
import me.Plugins.SimpleFactions.enums.Terrain;

public final class BattleBoundsService {
	private BattleBoundsService() {
	}

	public static boolean applies(Battle battle) {
		if (battle == null || !battle.hasStarted()) {
			return false;
		}
		BattleType type = battle.getBattleType();
		return type == BattleType.FIELD || type == BattleType.SIEGE;
	}

	public static void resolveAllowedProvinces(Battle battle) {
		resolveAllowedProvinces(battle, SimpleFactions.getInstance());
	}

	static void resolveAllowedProvinces(Battle battle, SimpleFactions plugin) {
		if (battle == null) {
			return;
		}
		if (battle.getBattleType() == BattleType.RAID) {
			battle.setAllowedProvinceIds(java.util.Collections.emptySet());
			return;
		}
		Set<Integer> allowed = new HashSet<>();
		Integer provinceId = battle.getProvinceId();
		if (provinceId == null || provinceId <= 0) {
			provinceId = resolveProvinceFromSpawn(battle, plugin);
			if (provinceId != null && provinceId > 0) {
				battle.setProvinceId(provinceId);
			}
		}
		if (provinceId != null && provinceId > 0) {
			allowed.add(provinceId);
		}
		if (battle.isNavalVariant() && provinceId != null && provinceId > 0 && plugin != null) {
			ProvinceManager pm = plugin.getProvinceManager();
			if (pm != null) {
				Integer seaNeighbour = findAdjacentSeaProvince(pm, provinceId);
				if (seaNeighbour != null) {
					allowed.add(seaNeighbour);
				}
			}
		}
		battle.setAllowedProvinceIds(allowed);
	}

	public static boolean isInBounds(Battle battle, Player player) {
		if (battle == null || player == null || !applies(battle)) {
			return true;
		}
		Set<Integer> allowed = battle.getAllowedProvinceIds();
		if (allowed == null || allowed.isEmpty()) {
			return true;
		}
		SimpleFactions plugin = SimpleFactions.getInstance();
		if (plugin == null) {
			return true;
		}
		ProvinceGrid grid = plugin.getProvinceGrid();
		if (grid == null) {
			return true;
		}
		Location loc = player.getLocation();
		int provinceId = grid.getAt(loc.getBlockX(), loc.getBlockZ());
		return isProvinceAllowed(battle, provinceId);
	}

	public static boolean isProvinceAllowed(Battle battle, int provinceId) {
		if (battle == null || provinceId <= 0) {
			return false;
		}
		Set<Integer> allowed = battle.getAllowedProvinceIds();
		return allowed != null && allowed.contains(provinceId);
	}

	private static Integer resolveProvinceFromSpawn(Battle battle, SimpleFactions plugin) {
		if (plugin == null) {
			return null;
		}
		ProvinceGrid grid = plugin.getProvinceGrid();
		if (grid == null) {
			return null;
		}
		BattleSide defender = battle.getSideById(BattleTemplate.DEFENDER_SIDE);
		Location spawn = defender != null ? defender.getSpawn() : null;
		if (spawn == null) {
			BattleSide attacker = battle.getSideById(BattleTemplate.ATTACKER_SIDE);
			spawn = attacker != null ? attacker.getSpawn() : null;
		}
		if (spawn == null || spawn.getWorld() == null) {
			return null;
		}
		int provinceId = grid.getAt(spawn.getBlockX(), spawn.getBlockZ());
		return provinceId > 0 ? provinceId : null;
	}

	static Integer findAdjacentSeaProvince(ProvinceManager pm, int provinceId) {
		if (pm == null) {
			return null;
		}
		Province province = pm.get(provinceId);
		if (province == null || province.getId() == 0) {
			return null;
		}
		for (int neighbourId : province.getNeighbours()) {
			Province neighbour = pm.get(neighbourId);
			if (neighbour != null && neighbour.getTerrain() == Terrain.SEA) {
				return neighbourId;
			}
		}
		return null;
	}
}
