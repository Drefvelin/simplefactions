package me.Plugins.SimpleFactions.War.campaign.raid;

import java.time.Instant;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.WarManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.campaign.raid.CampaignRaidResults.TransitionResult;
import me.Plugins.SimpleFactions.War.campaign.runtime.BattleSideMembers;
import me.Plugins.SimpleFactions.War.core.Side;
import me.Plugins.SimpleFactions.War.core.War;
import me.Plugins.SimpleFactions.installation.Installation;

public final class CampaignRaidLaunchService {
	private CampaignRaidLaunchService() {}

	public static void startFight(War war, Instant now) {
		if (war == null || now == null) {
			return;
		}
		TransitionResult result = CampaignRaidService.transitionToFighting(war, now);
		if (result != TransitionResult.OK) {
			return;
		}
		CampaignRaid raid = CampaignRaidService.getActive(war);
		if (raid != null) {
			CampaignRaidWarbandService.enrollOnlineDefenders(war, raid);
			CampaignRaidService.setRepairLockUntil(
					war,
					raid.getTargetInstallationId(),
					CampaignRaidService.repairLockUntilFromStart(now));
			CampaignRaidBattleService.createAndStart(war, raid, now);
			CampaignRaidFightScheduler.onFightStarted(war, now);
		}
		WarManager.persist(war);
		broadcastRaidStarted(war);
	}

	private static void broadcastRaidStarted(War war) {
		CampaignRaid raid = CampaignRaidService.getActive(war);
		if (raid == null) {
			return;
		}
		Installation target = resolveInstallation(raid.getTargetInstallationId());
		String message = CampaignRaidMessages.buildRaidStartedMessage(target);
		broadcastToSide(war.getAttackers(), message);
		broadcastToSide(war.getDefenders(), message);
	}

	private static Installation resolveInstallation(String installationId) {
		if (installationId == null || installationId.isBlank()) {
			return null;
		}
		for (Faction faction : FactionManager.factions) {
			if (faction == null || faction.getInstallationHandler() == null) {
				continue;
			}
			Installation installation = faction.getInstallationHandler().getById(installationId);
			if (installation != null) {
				return installation;
			}
		}
		return null;
	}

	private static void broadcastToSide(Side side, String message) {
		if (side == null || message == null) {
			return;
		}
		for (String memberName : BattleSideMembers.collectEligibleMemberNames(side)) {
			Player player = Bukkit.getPlayerExact(memberName);
			if (player != null && player.isOnline()) {
				player.sendMessage(message);
			}
		}
	}
}
