package me.Plugins.SimpleFactions.War.battle.campaign;

import java.time.Duration;
import java.time.Instant;

import org.bukkit.entity.Player;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.War.battle.engine.core.Battle;
import me.Plugins.SimpleFactions.War.battle.engine.core.BattleEndSupport;
import me.Plugins.SimpleFactions.War.battle.engine.core.BattleSide;
import me.Plugins.SimpleFactions.War.battle.enums.BattleEndReason;
import me.Plugins.SimpleFactions.War.battle.enums.BattleType;
import me.Plugins.SimpleFactions.War.battle.template.BattleTemplate;
import me.Plugins.SimpleFactions.War.battle.warband.Warband;
import me.Plugins.SimpleFactions.War.battle.warband.WarbandManager;
import me.Plugins.SimpleFactions.War.core.War;

public final class BattleWarbandRetreatService {
	public enum RetreatResult {
		SUCCESS,
		REJECTED_NOT_IN_WARBAND,
		REJECTED_NOT_LEADER,
		REJECTED_PENDING_LEADER,
		REJECTED_NOT_IN_BATTLE,
		REJECTED_BATTLE_NOT_STARTED,
		REJECTED_NOT_CAMPAIGN_BATTLE,
		REJECTED_RAID,
		REJECTED_WRONG_BATTLE_TYPE,
		REJECTED_WAR_INACTIVE,
		REJECTED_TOO_EARLY,
		REJECTED_NO_OPPONENT
	}

	private BattleWarbandRetreatService() {
	}

	public static boolean canRetreat(Player player, Instant now) {
		return retreatRejection(player, now) == null;
	}

	public static RetreatResult retreatRejection(Player player, Instant now) {
		if (player == null) {
			return RetreatResult.REJECTED_NOT_IN_WARBAND;
		}
		Warband warband = WarbandManager.getByLeader(player);
		if (warband == null) {
			if (WarbandManager.getByMemberId(player.getUniqueId()) != null) {
				return RetreatResult.REJECTED_NOT_LEADER;
			}
			return RetreatResult.REJECTED_NOT_IN_WARBAND;
		}
		if (!player.getUniqueId().equals(warband.getLeaderId())) {
			return RetreatResult.REJECTED_NOT_LEADER;
		}
		if (warband.isPendingLeader()) {
			return RetreatResult.REJECTED_PENDING_LEADER;
		}

		CampaignBattleJoinService.CampaignBattleContext ctx =
				CampaignBattleJoinService.findCampaignBattleForWarband(warband);
		if (ctx == null) {
			return RetreatResult.REJECTED_NOT_IN_BATTLE;
		}

		War war = ctx.war();
		Battle battle = ctx.battle();
		if (war == null || !war.isActive()) {
			return RetreatResult.REJECTED_WAR_INACTIVE;
		}
		if (!battle.hasStarted()) {
			return RetreatResult.REJECTED_BATTLE_NOT_STARTED;
		}
		if (battle.getWarId() == null) {
			return RetreatResult.REJECTED_NOT_CAMPAIGN_BATTLE;
		}
		if (battle.isCampaignRaid() || battle.getBattleType() == BattleType.RAID) {
			return RetreatResult.REJECTED_RAID;
		}
		BattleType battleType = battle.getBattleType();
		if (battleType != BattleType.FIELD && battleType != BattleType.SIEGE) {
			return RetreatResult.REJECTED_WRONG_BATTLE_TYPE;
		}
		if (battle.getStartedAt() == null) {
			return RetreatResult.REJECTED_TOO_EARLY;
		}
		long elapsedSeconds = Duration.between(battle.getStartedAt(), now).getSeconds();
		if (elapsedSeconds < Cache.battleRetreatMinElapsedSeconds) {
			return RetreatResult.REJECTED_TOO_EARLY;
		}

		String opponentSideId = opponentSideId(battle, ctx.sideId());
		if (opponentSideId == null) {
			return RetreatResult.REJECTED_NO_OPPONENT;
		}
		return null;
	}

	public static RetreatResult retreat(Player player, Instant now) {
		RetreatResult rejection = retreatRejection(player, now);
		if (rejection != null) {
			return rejection;
		}

		Warband warband = WarbandManager.getByLeader(player);
		CampaignBattleJoinService.CampaignBattleContext ctx =
				CampaignBattleJoinService.findCampaignBattleForWarband(warband);
		Battle battle = ctx.battle();
		String opponentSideId = opponentSideId(battle, ctx.sideId());
		BattleEndSupport.endBattle(battle, opponentSideId, BattleEndReason.RETREAT);
		return RetreatResult.SUCCESS;
	}

	public static String opponentSideId(Battle battle, String retreatingSideId) {
		if (battle == null || retreatingSideId == null) {
			return null;
		}
		String opponentId = null;
		if (BattleTemplate.ATTACKER_SIDE.equalsIgnoreCase(retreatingSideId)) {
			opponentId = BattleTemplate.DEFENDER_SIDE;
		} else if (BattleTemplate.DEFENDER_SIDE.equalsIgnoreCase(retreatingSideId)) {
			opponentId = BattleTemplate.ATTACKER_SIDE;
		}
		if (opponentId == null) {
			return null;
		}
		BattleSide opponent = battle.getSideById(opponentId);
		return opponent != null ? opponentId : null;
	}

	public static long remainingSecondsUntilRetreat(Battle battle, Instant now) {
		if (battle == null || battle.getStartedAt() == null || now == null) {
			return Cache.battleRetreatMinElapsedSeconds;
		}
		long elapsedSeconds = Duration.between(battle.getStartedAt(), now).getSeconds();
		return Math.max(0L, Cache.battleRetreatMinElapsedSeconds - elapsedSeconds);
	}
}
