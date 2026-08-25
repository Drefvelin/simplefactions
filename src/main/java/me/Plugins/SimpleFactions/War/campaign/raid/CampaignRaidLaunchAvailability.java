package me.Plugins.SimpleFactions.War.campaign.raid;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.campaign.progression.CampaignCoalition;
import me.Plugins.SimpleFactions.War.campaign.raid.CampaignRaidResults.LaunchResult;
import me.Plugins.SimpleFactions.War.campaign.runtime.BattleScheduleService;
import me.Plugins.SimpleFactions.War.core.Side;
import me.Plugins.SimpleFactions.War.core.War;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;
import me.Plugins.SimpleFactions.War.campaign.ui.CampaignUiCopy;

public final class CampaignRaidLaunchAvailability {
	private CampaignRaidLaunchAvailability() {}

	public record LaunchAvailability(boolean enabled, List<String> loreLines) {}

	public static LaunchAvailability describe(War war, Faction faction, Instant now) {
		List<String> lore = new ArrayList<>();
		boolean windowOpen = war != null && faction != null && now != null
				&& BattleScheduleService.isRaidWindowOpen(war, now);
		if (windowOpen) {
			lore.add(StringFormatter.formatHex(CampaignUiCopy.SELECT + "Raid window open (19:00-20:00)"));
		} else {
			lore.add(StringFormatter.formatHex(CampaignUiCopy.REMOVE + "Outside raid call window"));
		}

		CampaignCoalition coalition = coalitionForFaction(war, faction);
		if (coalition != null) {
			boolean ownQuota = CampaignRaidService.isSideQuotaUsed(war, coalition);
			lore.add(StringFormatter.formatHex(
					CampaignUiCopy.LABEL + "Your raid quota: "
							+ (ownQuota ? CampaignUiCopy.REMOVE + "spent" : CampaignUiCopy.SELECT + "available")));
			CampaignCoalition enemy = coalition.opposing();
			boolean enemyQuota = CampaignRaidService.isSideQuotaUsed(war, enemy);
			String enemyLabel = enemy == CampaignCoalition.AGGRESSOR ? "Attacker" : "Defender";
			lore.add(StringFormatter.formatHex(
					CampaignUiCopy.LABEL + enemyLabel + " raid quota: "
							+ (enemyQuota ? CampaignUiCopy.REMOVE + "spent" : CampaignUiCopy.SELECT + "available")));
		}

		if (war != null && CampaignRaidService.getActive(war) != null) {
			lore.add(StringFormatter.formatHex(CampaignUiCopy.WARNING + "Campaign raid in progress"));
		}

		boolean enabled = war != null && faction != null && now != null
				&& CampaignRaidService.canLaunch(war, faction, now) == LaunchResult.STARTED;
		if (enabled) {
			lore.add(StringFormatter.formatHex(CampaignUiCopy.LABEL + "Leader only - click to launch"));
		}
		return new LaunchAvailability(enabled, List.copyOf(lore));
	}

	private static CampaignCoalition coalitionForFaction(War war, Faction faction) {
		if (war == null || faction == null) {
			return null;
		}
		Side side = war.getSide(faction);
		if (side == null) {
			return null;
		}
		if (side == war.getAttackers()) {
			return CampaignCoalition.AGGRESSOR;
		}
		if (side == war.getDefenders()) {
			return CampaignCoalition.DEFENDER;
		}
		return null;
	}
}
