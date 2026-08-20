package me.Plugins.SimpleFactions.War.battle.campaign;

import java.time.Instant;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.battle.engine.Battle;
import me.Plugins.SimpleFactions.War.battle.engine.BattleFactory;
import me.Plugins.SimpleFactions.War.battle.engine.BattleManager;
import me.Plugins.SimpleFactions.War.battle.enums.BattleType;
import me.Plugins.SimpleFactions.War.enums.BattleSchedulePhase;
import me.Plugins.SimpleFactions.War.schedule.BattleScheduleService;
import me.Plugins.SimpleFactions.War.schedule.BattleSideMembers;

public final class CampaignBattleLaunchService {
	private CampaignBattleLaunchService() {
	}

	public static Battle prepareScheduledBattle(War war) {
		if (war == null || !war.isActive()) {
			return null;
		}
		Battle existing = BattleManager.getByWarId(war.getId());
		if (existing != null) {
			return existing;
		}

		Integer provinceId = war.getScheduledBattleProvinceId();
		if (provinceId == null) {
			provinceId = BattleScheduleService.resolveScheduledProvinceId(war);
		}
		if (provinceId == null) {
			return null;
		}

		return createCampaignBattle(war, provinceId, false);
	}

	public static Battle launchAutoresolveBattle(War war) {
		if (war == null || !war.isActive()) {
			return null;
		}
		Battle existing = BattleManager.getByWarId(war.getId());
		if (existing != null) {
			if (!existing.hasStarted()) {
				existing.start();
			}
			return existing;
		}

		Integer provinceId = BattleScheduleService.resolveScheduledProvinceId(war);
		if (provinceId == null) {
			return null;
		}

		Battle battle = createCampaignBattle(war, provinceId, true);
		if (battle != null && !battle.hasStarted()) {
			battle.start();
		}
		return battle;
	}

	public static boolean tryStartScheduledBattle(War war, Instant now) {
		if (war == null || !war.isActive() || now == null) {
			return false;
		}
		if (war.getBattleSchedulePhase() != BattleSchedulePhase.SCHEDULED) {
			return false;
		}
		Instant scheduledAt = war.getScheduledBattleAt();
		if (scheduledAt == null || now.isBefore(scheduledAt)) {
			return false;
		}

		Battle battle = BattleManager.getByWarId(war.getId());
		if (battle == null) {
			battle = prepareScheduledBattle(war);
		}
		if (battle == null || battle.hasStarted()) {
			return false;
		}

		battle.start();
		return true;
	}

	private static Battle createCampaignBattle(War war, int provinceId, boolean immediateStart) {
		BattleType type = CampaignBattleTypeResolver.resolve(war, provinceId);
		String battleId = campaignBattleId(war.getId(), provinceId);

		Battle battle = BattleFactory.createBlank(type, battleId);
		battle.setWarId(war.getId());
		battle.setProvinceId(provinceId);
		battle.setLocked(false);
		battle.setTeleport(true);
		BattleFactory.applyCampaignDefault(battle);
		BattleManager.addBattle(battle);
		CampaignBattleRosterService.enrollWarbands(war, battle);
		if (!immediateStart) {
			broadcastJoinMessage(war, battleId);
		}
		return battle;
	}

	static String campaignBattleId(int warId, int provinceId) {
		return "campaign_w" + warId + "_p" + provinceId;
	}

	private static void broadcastJoinMessage(War war, String battleId) {
		String message = "§aCampaign battle ready. Join with §e/battle join "
				+ battleId + " attacker §7or §edefender";
		for (String memberName : BattleSideMembers.collectEligibleMemberNames(war.getAttackers())) {
			Player player = Bukkit.getPlayerExact(memberName);
			if (player != null && player.isOnline()) {
				player.sendMessage(message);
			}
		}
		for (String memberName : BattleSideMembers.collectEligibleMemberNames(war.getDefenders())) {
			Player player = Bukkit.getPlayerExact(memberName);
			if (player != null && player.isOnline()) {
				player.sendMessage(message);
			}
		}
	}
}
