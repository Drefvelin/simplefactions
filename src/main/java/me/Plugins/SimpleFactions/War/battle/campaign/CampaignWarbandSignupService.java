package me.Plugins.SimpleFactions.War.battle.campaign;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.War.battle.campaign.CampaignBattleJoinService.CampaignBattleContext;
import me.Plugins.SimpleFactions.War.battle.dev.BattleDevMode;
import me.Plugins.SimpleFactions.War.battle.engine.core.Battle;
import me.Plugins.SimpleFactions.War.battle.engine.core.BattleManager;
import me.Plugins.SimpleFactions.War.battle.engine.core.BattleSide;
import me.Plugins.SimpleFactions.War.battle.engine.win.FieldWinService;
import me.Plugins.SimpleFactions.War.battle.engine.win.SiegeWinService;
import me.Plugins.SimpleFactions.War.battle.enums.BattleType;
import me.Plugins.SimpleFactions.War.battle.ui.BattleInventoryManager;
import me.Plugins.SimpleFactions.War.battle.warband.Warband;
import me.Plugins.SimpleFactions.War.battle.persistence.BattlePersistenceService;

public final class CampaignWarbandSignupService {
	private CampaignWarbandSignupService() {
	}

	public static String signup(Player player, Warband warband, Faction playerFaction) {
		if (player == null || warband == null) {
			return "Invalid warband signup";
		}
		return signupMember(player.getUniqueId(), player.getName(), warband, playerFaction, player);
	}

	static String signupMember(UUID playerId, String playerName, Warband warband, Faction playerFaction) {
		return signupMember(playerId, playerName, warband, playerFaction, null);
	}

	static String signupMember(
			UUID playerId,
			String playerName,
			Warband warband,
			Faction playerFaction,
			Player onlinePlayer) {
		if (playerId == null || playerName == null || warband == null) {
			return "Invalid warband signup";
		}
		if (warband.hasMember(playerId)) {
			return null;
		}

		CampaignBattleContext ctx = CampaignBattleJoinService.findCampaignBattleForWarband(warband);
		if (ctx != null) {
			String joinError = CampaignBattleJoinService.validateWarbandMemberJoin(
					ctx.war(), ctx.battle(), ctx.sideId(), warband, playerName, playerId);
			if (joinError != null) {
				return joinError;
			}
		}

		boolean firstSignup = warband.getRealMemberCount() == 0;
		applyLeaderRules(ctx, warband, playerId, playerName);
		warband.addMember(playerId);

		if (ctx != null && firstSignup) {
			BattleDevMode.seedPhantomsOnFirstSignupIfEnabled(
					warband, ctx.war(), ctx.battle(), ctx.sideId());
		}
		if (ctx != null && ctx.battle().hasStarted() && onlinePlayer != null) {
			CampaignWarbandBattleService.onMemberJoined(onlinePlayer, warband, ctx);
		}
		BattlePersistenceService.persistWarband(warband);
		return null;
	}

	private static void applyLeaderRules(
			CampaignBattleContext ctx,
			Warband warband,
			UUID playerId,
			String playerName) {
		if (warband.isPendingLeader()) {
			warband.setLeaderId(playerId);
			return;
		}
		if (ctx != null && CampaignWarbandBattleService.isWarSideMainLeader(ctx.war(), warband, playerName)) {
			warband.setLeaderId(playerId);
		}
	}
}
