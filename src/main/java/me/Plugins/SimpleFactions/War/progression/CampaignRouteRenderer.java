package me.Plugins.SimpleFactions.War.progression;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.bukkit.Material;

import me.Plugins.SimpleFactions.Managers.TitleManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.Side;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.enums.ObjectiveHolder;
import me.Plugins.SimpleFactions.War.pathfinder.BelligerentTerritory;
import me.Plugins.SimpleFactions.War.pathfinder.ProvinceOwnerLookup;
import me.Plugins.SimpleFactions.War.schedule.CampaignScheduleService;
import me.Plugins.SimpleFactions.War.schedule.CampaignUiCopy;
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

	public static List<String> buildRouteLore(War war, int provinceId, ProvinceOwnerLookup owners) {
		List<String> lore = new ArrayList<>();
		if (war == null) {
			return lore;
		}

		String objectiveLine = resolveObjectiveLine(war, provinceId);
		if (objectiveLine != null) {
			lore.add(StringFormatter.formatHex(CampaignUiCopy.OBJECTIVE + objectiveLine));
		}

		appendBattleKindLine(lore, war, provinceId);

		appendRealmLines(lore, war, provinceId, owners);

		List<Integer> actions = actionProvinceIds(war);
		if (actions.size() == 1 && actions.get(0) == provinceId) {
			lore.add(StringFormatter.formatHex(CampaignUiCopy.NEXT_BATTLE + "Next battle"));
		}
		return lore;
	}

	private static void appendBattleKindLine(List<String> lore, War war, int provinceId) {
		CampaignScheduleService.slotForProvince(war, provinceId).ifPresent(slot -> {
			String label = CampaignUiCopy.formatBattleKind(slot.kind());
			if (label != null) {
				lore.add(StringFormatter.formatHex(CampaignUiCopy.BATTLE_KIND + label));
			}
		});
	}

	private static String resolveObjectiveLine(War war, int provinceId) {
		Integer attackerCapital = war.getAttackers().getLeader().getCapital();
		if (attackerCapital != null && attackerCapital > 0 && attackerCapital == provinceId) {
			return "Attacker Capital";
		}
		Integer defenderCapital = war.getDefenders().getLeader().getCapital();
		if (defenderCapital != null && defenderCapital > 0 && defenderCapital == provinceId) {
			return "Defender Capital";
		}
		Integer objectiveId = war.getObjectiveProvinceId();
		if (objectiveId == null || objectiveId != provinceId) {
			return null;
		}
		ObjectiveHolder holder = war.getObjectiveHeldBy();
		if (holder == ObjectiveHolder.ATTACKER) {
			return "Attacker Target Region";
		}
		return "Defender Target Region";
	}

	private static void appendRealmLines(
			List<String> lore,
			War war,
			int provinceId,
			ProvinceOwnerLookup owners) {
		BelligerentTerritory territory = BelligerentTerritory.fromWar(war, owners);
		Faction owner = TitleManager.getByProvince(provinceId);
		if (owner == null) {
			return;
		}

		boolean attackerSide = territory.isAttackerSide(provinceId);
		boolean defenderSide = territory.isDefenderSide(provinceId);
		if (!attackerSide && !defenderSide) {
			return;
		}

		Faction sideLeader = attackerSide
				? war.getAttackers().getLeader()
				: war.getDefenders().getLeader();
		String leaderName = sideLeader.getName();
		lore.add(StringFormatter.formatHex(
				CampaignUiCopy.LABEL + "Part of " + CampaignUiCopy.VALUE + leaderName + CampaignUiCopy.LABEL + " Realm"));

		if (!normalizeId(owner.getId()).equals(normalizeId(sideLeader.getId()))) {
			lore.add(StringFormatter.formatHex(CampaignUiCopy.MUTED + "(Owned by " + owner.getName() + ")"));
		}
	}

	private static String normalizeId(String id) {
		return id == null ? null : id.toLowerCase(Locale.ROOT);
	}
}
