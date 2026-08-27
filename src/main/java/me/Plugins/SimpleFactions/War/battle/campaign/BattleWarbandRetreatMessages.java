package me.Plugins.SimpleFactions.War.battle.campaign;

import java.time.Instant;

import org.bukkit.entity.Player;

import me.Plugins.SimpleFactions.War.battle.campaign.BattleWarbandRetreatService.RetreatResult;
import me.Plugins.SimpleFactions.War.battle.engine.core.Battle;
import me.Plugins.SimpleFactions.War.battle.warband.Warband;
import me.Plugins.SimpleFactions.War.battle.warband.WarbandManager;

public final class BattleWarbandRetreatMessages {
	public static final String NOT_IN_WARBAND = "§cYou need to lead a warband to retreat.";
	public static final String NOT_LEADER = "§cOnly the warband leader can retreat.";
	public static final String PENDING_LEADER = "§cYour warband has no leader yet.";
	public static final String NOT_IN_BATTLE = "§cYou are not in an active campaign battle.";
	public static final String BATTLE_NOT_STARTED = "§cThe battle has not started yet.";
	public static final String NOT_CAMPAIGN = "§cYou can only retreat from campaign battles.";
	public static final String RAID = "§cYou cannot retreat from a raid.";
	public static final String WRONG_BATTLE_TYPE = "§cYou cannot retreat from this battle type.";
	public static final String WAR_INACTIVE = "§cWar not found.";
	public static final String NO_OPPONENT = "§cCould not resolve the opposing battle side.";
	public static final String SUCCESS = "§aYour warband has retreated. The battle is lost.";

	private BattleWarbandRetreatMessages() {
	}

	public static String messageForResult(RetreatResult result) {
		return messageForResult(result, null, null);
	}

	public static String messageForResult(RetreatResult result, Player player, Instant now) {
		if (result == null || result == RetreatResult.SUCCESS) {
			return result == RetreatResult.SUCCESS ? SUCCESS : null;
		}
		return switch (result) {
			case REJECTED_NOT_IN_WARBAND -> NOT_IN_WARBAND;
			case REJECTED_NOT_LEADER -> NOT_LEADER;
			case REJECTED_PENDING_LEADER -> PENDING_LEADER;
			case REJECTED_NOT_IN_BATTLE -> NOT_IN_BATTLE;
			case REJECTED_BATTLE_NOT_STARTED -> BATTLE_NOT_STARTED;
			case REJECTED_NOT_CAMPAIGN_BATTLE -> NOT_CAMPAIGN;
			case REJECTED_RAID -> RAID;
			case REJECTED_WRONG_BATTLE_TYPE -> WRONG_BATTLE_TYPE;
			case REJECTED_WAR_INACTIVE -> WAR_INACTIVE;
			case REJECTED_NO_OPPONENT -> NO_OPPONENT;
			case REJECTED_TOO_EARLY -> buildTooEarlyMessage(player, now);
			default -> null;
		};
	}

	private static String buildTooEarlyMessage(Player player, Instant now) {
		if (player == null || now == null) {
			return "§cYou cannot retreat yet.";
		}
		Warband warband = WarbandManager.getByLeader(player);
		if (warband == null) {
			return "§cYou cannot retreat yet.";
		}
		CampaignBattleJoinService.CampaignBattleContext ctx =
				CampaignBattleJoinService.findCampaignBattleForWarband(warband);
		if (ctx == null) {
			return "§cYou cannot retreat yet.";
		}
		Battle battle = ctx.battle();
		long remainingSeconds = BattleWarbandRetreatService.remainingSecondsUntilRetreat(battle, now);
		long remainingMinutes = (remainingSeconds + 59L) / 60L;
		if (remainingMinutes <= 1L) {
			return "§cYou cannot retreat for another minute.";
		}
		return "§cYou cannot retreat for another " + remainingMinutes + " minutes.";
	}
}
