package me.Plugins.SimpleFactions.War.battle.campaign;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import me.Plugins.SimpleFactions.Managers.WarManager;
import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.battle.engine.Battle;
import me.Plugins.SimpleFactions.War.battle.engine.BattleManager;
import me.Plugins.SimpleFactions.War.battle.events.BattleEndedEvent;
import me.Plugins.SimpleFactions.War.battle.template.BattleTemplate;
import me.Plugins.SimpleFactions.War.progression.BelligerentRole;
import me.Plugins.SimpleFactions.War.progression.CampaignProgressionService;
import me.Plugins.SimpleFactions.War.progression.OccupationService;
import me.Plugins.SimpleFactions.War.pathfinder.TitleManagerProvinceOwnerLookup;
import me.Plugins.SimpleFactions.War.schedule.BattleScheduleService;
import me.Plugins.SimpleFactions.War.schedule.BattleSideMembers;

public class CampaignBattleOutcomeService implements Listener {
	@EventHandler
	public void onBattleEnded(BattleEndedEvent event) {
		handleBattleEnded(event);
	}

	static void handleBattleEnded(BattleEndedEvent event) {
		if (event == null || event.getWarId() == null) {
			return;
		}

		War war = WarManager.getById(event.getWarId());
		if (war == null || !war.isActive()) {
			return;
		}

		BelligerentRole winnerRole = mapWinningSide(event.getWinningSideId());
		Battle battle = BattleManager.getByString(event.getBattleId());
		Integer battleProvinceId = battle != null && battle.getProvinceId() != null
				? battle.getProvinceId()
				: war.getScheduledBattleProvinceId();

		if (winnerRole != null && battleProvinceId != null) {
			BelligerentRole offensiveSide = CampaignProgressionService.getOffensiveSide(war);
			boolean offensiveSideWon = offensiveSide == winnerRole;
			CampaignProgressionService.applyFoughtBattleOutcome(war, offensiveSideWon);
			occupationService().applyBattleWin(war, battleProvinceId, winnerRole);
		}

		BattleScheduleService.openVote(war);

		if (battle != null) {
			BattleManager.deleteBattle(battle);
		}

		WarManager.persist(war);
		broadcastResult(war, event.getWinningSideId());
	}

	private static BelligerentRole mapWinningSide(String winningSideId) {
		if (winningSideId == null || winningSideId.isBlank()) {
			return null;
		}
		if (BattleTemplate.ATTACKER_SIDE.equalsIgnoreCase(winningSideId)) {
			return BelligerentRole.ATTACKER;
		}
		if (BattleTemplate.DEFENDER_SIDE.equalsIgnoreCase(winningSideId)) {
			return BelligerentRole.DEFENDER;
		}
		return null;
	}

	private static OccupationService occupationService() {
		if (SimpleFactions.plugin != null) {
			return new OccupationService(
					SimpleFactions.plugin.getProvinceManager(),
					new TitleManagerProvinceOwnerLookup());
		}
		return new OccupationService(null, new TitleManagerProvinceOwnerLookup());
	}

	private static void broadcastResult(War war, String winningSideId) {
		String result = winningSideId == null || winningSideId.isBlank()
				? "§7Campaign battle ended with no winner. Voting reopened."
				: "§aCampaign battle ended. Winner: §e" + winningSideId + "§7. Voting reopened.";
		for (String memberName : BattleSideMembers.collectEligibleMemberNames(war.getAttackers())) {
			Player player = Bukkit.getPlayerExact(memberName);
			if (player != null && player.isOnline()) {
				player.sendMessage(result);
			}
		}
		for (String memberName : BattleSideMembers.collectEligibleMemberNames(war.getDefenders())) {
			Player player = Bukkit.getPlayerExact(memberName);
			if (player != null && player.isOnline()) {
				player.sendMessage(result);
			}
		}
	}
}
