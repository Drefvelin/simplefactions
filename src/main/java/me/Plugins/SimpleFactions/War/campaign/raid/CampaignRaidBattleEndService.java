package me.Plugins.SimpleFactions.War.campaign.raid;

import java.time.Instant;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.WarManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.battle.engine.core.Battle;
import me.Plugins.SimpleFactions.War.battle.engine.core.BattleManager;
import me.Plugins.SimpleFactions.War.battle.events.BattleEndedEvent;
import me.Plugins.SimpleFactions.War.battle.persistence.BattlePersistenceService;
import me.Plugins.SimpleFactions.War.campaign.runtime.BattleSideMembers;
import me.Plugins.SimpleFactions.War.core.Side;
import me.Plugins.SimpleFactions.War.core.War;
import me.Plugins.SimpleFactions.installation.Installation;

public class CampaignRaidBattleEndService implements Listener {
	@EventHandler
	public void onBattleEnded(BattleEndedEvent event) {
		handleBattleEnded(event);
	}

	static void handleBattleEnded(BattleEndedEvent event) {
		if (event == null || event.getWarId() == null) {
			return;
		}
		Battle battle = BattleManager.getByString(event.getBattleId());
		if (battle == null || !battle.isCampaignRaid()) {
			return;
		}
		War war = WarManager.getById(event.getWarId());
		if (war == null) {
			return;
		}
		CampaignRaid raid = CampaignRaidService.getActive(war);
		Installation target = raid != null ? resolveInstallation(raid.getTargetInstallationId()) : null;
		CampaignRaidService.endRaid(war, Instant.now());
		BattlePersistenceService.deleteRaidBattle(battle);
		broadcastRaidEnded(war, target);
	}

	private static void broadcastRaidEnded(War war, Installation target) {
		if (war == null) {
			return;
		}
		String message = CampaignRaidMessages.buildRaidEndedMessage(target);
		broadcastToSide(war.getAttackers(), message);
		broadcastToSide(war.getDefenders(), message);
	}

	private static void broadcastToSide(Side side, String message) {
		if (side == null || message == null || Bukkit.getServer() == null) {
			return;
		}
		for (String memberName : BattleSideMembers.collectEligibleMemberNames(side)) {
			Player player = Bukkit.getPlayerExact(memberName);
			if (player != null && player.isOnline()) {
				player.sendMessage(message);
			}
		}
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
}
