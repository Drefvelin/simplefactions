package me.Plugins.SimpleFactions.War.progression;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;

import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.Side;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.pathfinder.BelligerentTerritory;
import me.Plugins.SimpleFactions.War.pathfinder.ProvinceOwnerLookup;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public final class CampaignRouteRenderer {
	private CampaignRouteRenderer() {}

	public static List<Integer> actionProvinceIds(War war) {
		return CampaignProgressionService.resolveNextBattleNodes(war);
	}

	public static Material resolveMaterial(
			War war,
			Faction viewer,
			int provinceId,
			ProvinceOwnerLookup owners) {
		List<Integer> actions = actionProvinceIds(war);
		if (actions.size() == 1 && actions.get(0) == provinceId) {
			return Material.GREEN_CONCRETE;
		}
		if (actions.size() == 2 && actions.contains(provinceId)) {
			return Material.YELLOW_CONCRETE;
		}
		return resolveOwnershipMaterial(war, viewer, provinceId, owners);
	}

	public static Material resolveOwnershipMaterial(
			War war,
			Faction viewer,
			int provinceId,
			ProvinceOwnerLookup owners) {
		BelligerentTerritory territory = BelligerentTerritory.fromWar(war, owners);
		Side viewerSide = war.getSide(viewer);
		if (viewerSide == null) {
			return Material.RED_CONCRETE;
		}
		boolean viewerIsAttacker = viewerSide.equals(war.getAttackers());
		boolean provinceOwnedByAttacker = territory.isAttackerSide(provinceId);
		if (territory.isNeutral(provinceId)) {
			return Material.RED_CONCRETE;
		}
		if (viewerIsAttacker == provinceOwnedByAttacker) {
			return Material.BLUE_CONCRETE;
		}
		return Material.RED_CONCRETE;
	}

	public static boolean isCursorIndex(War war, int axisIndex) {
		return war != null && war.getCursorIndex() == axisIndex;
	}

	public static List<String> buildRouteLore(War war, int provinceId, int axisIndex) {
		List<String> lore = new ArrayList<>();
		lore.add(StringFormatter.formatHex("#a39ba8Province #d4c9ae" + provinceId));
		if (isCursorIndex(war, axisIndex)) {
			lore.add(StringFormatter.formatHex("#e6c84aCursor - current front"));
		}
		if (war.getObjectiveProvinceId() != null && war.getObjectiveProvinceId() == provinceId) {
			lore.add(StringFormatter.formatHex("#e6c84aObjective"));
		}
		if (war.getCampaignStartProvinceId() != null && war.getCampaignStartProvinceId() == provinceId) {
			lore.add(StringFormatter.formatHex("#e6c84aFirst battle province"));
		}
		Integer attackerCapital = war.getAttackers().getLeader().getCapital();
		if (attackerCapital != null && attackerCapital > 0 && attackerCapital == provinceId) {
			lore.add(StringFormatter.formatHex("#a39ba8Attacker capital"));
		}
		Integer defenderCapital = war.getDefenders().getLeader().getCapital();
		if (defenderCapital != null && defenderCapital > 0 && defenderCapital == provinceId) {
			lore.add(StringFormatter.formatHex("#a39ba8Defender capital"));
		}
		return lore;
	}
}
