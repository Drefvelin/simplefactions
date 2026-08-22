package me.Plugins.SimpleFactions.War.battle.campaign;

import java.util.UUID;

import org.bukkit.entity.Player;

import me.Plugins.SimpleFactions.War.battle.engine.Battle;
import me.Plugins.SimpleFactions.War.battle.engine.BattleManager;
import me.Plugins.SimpleFactions.War.battle.engine.BattleSide;
import me.Plugins.SimpleFactions.War.battle.engine.FieldWinService;
import me.Plugins.SimpleFactions.War.battle.engine.SiegeWinService;
import me.Plugins.SimpleFactions.War.battle.enums.BattleType;
import me.Plugins.SimpleFactions.War.battle.ui.BattleInventoryManager;
import me.Plugins.SimpleFactions.War.battle.warband.Warband;
import me.Plugins.SimpleFactions.War.battle.persistence.BattlePersistenceService;

public final class CampaignWarbandBattleService {
	private CampaignWarbandBattleService() {
	}

	public static String validateMidBattleJoin(
			me.Plugins.SimpleFactions.War.War war,
			Battle battle,
			String sideId,
			Warband warband,
			String joiningPlayerName,
			UUID playerId) {
		if (war == null || battle == null || warband == null || joiningPlayerName == null) {
			return "Invalid campaign warband join";
		}
		if (!battle.hasStarted()) {
			return null;
		}
		me.Plugins.SimpleFactions.Objects.Faction faction =
				me.Plugins.SimpleFactions.Managers.FactionManager.getByMember(joiningPlayerName);
		if (faction == null) {
			return "You must be in a faction to join this campaign battle";
		}
		me.Plugins.SimpleFactions.War.Side playerSide = war.getSide(faction);
		me.Plugins.SimpleFactions.War.Side battleSide = CampaignBattleJoinService.resolveWarSide(war, sideId);
		if (playerSide == null || battleSide == null || playerSide != battleSide) {
			return "Your faction is not on this battle side";
		}
		if (playerId != null
				&& CampaignWarbandLeaveBlock.isBlocked(battle.getId(), warband.getId(), playerId)) {
			return "You cannot rejoin this warband for this battle";
		}
		BattleSide battleSideEntity = battle.getSideById(sideId);
		if (battleSideEntity == null || battleSideEntity.getLives() <= 0) {
			return "Cannot join: this side has no lives remaining in the battle";
		}
		return null;
	}

	public static void onMemberJoined(Player player, Warband warband, CampaignBattleJoinService.CampaignBattleContext ctx) {
		if (player == null || warband == null || ctx == null) {
			return;
		}
		Battle battle = ctx.battle();
		if (!battle.hasStarted()) {
			return;
		}
		BattleSide side = battle.getSideById(ctx.sideId());
		if (side == null) {
			return;
		}
		side.tickLife();
		side.updateBossBar(battle.getAllParticipants());
		BattleManager.currentBattle.put(player, battle);
		side.addBossBarPlayer(player);
		if (battle.hasTeleport() && side.getSpawn() != null) {
			player.teleport(side.getSpawn());
		}
		if (battle.getBattleType() == BattleType.FIELD && battle.getPointManager().getPoints().size() > 0) {
			new org.bukkit.scheduler.BukkitRunnable() {
				@Override
				public void run() {
					BattleInventoryManager inv = new BattleInventoryManager();
					inv.spawnList(player, battle);
				}
			}.runTaskLater(me.Plugins.SimpleFactions.SimpleFactions.plugin, 2L);
		}
	}

	public static void processLeave(Player player, Warband warband, boolean voluntaryLeave) {
		if (player == null || warband == null) {
			return;
		}
		UUID playerId = player.getUniqueId();
		boolean wasLeader = playerId.equals(warband.getLeaderId());
		CampaignBattleJoinService.CampaignBattleContext ctx =
				CampaignBattleJoinService.findCampaignBattleForWarband(warband);

		if (ctx != null && ctx.battle().hasStarted() && voluntaryLeave) {
			CampaignWarbandLeaveBlock.block(ctx.battle().getId(), warband.getId(), playerId);
		}
		detachFromBattle(player, warband);
		warband.removeMember(playerId);

		if (wasLeader) {
			UUID nextLeader = warband.getOldestRealMemberId(null);
			if (nextLeader != null) {
				warband.setLeaderId(nextLeader);
			} else {
				warband.resetToPendingLeader();
			}
		}

		if (ctx != null && ctx.battle().hasStarted() && warband.getRealMemberCount() == 0) {
			checkSideAutoLose(ctx.battle(), ctx.sideId());
		}
		BattlePersistenceService.persistWarband(warband);
		if (ctx != null) {
			BattlePersistenceService.persistBattle(ctx.battle());
		}
	}

	private static void checkSideAutoLose(Battle battle, String sideId) {
		if (battle == null || !battle.hasStarted() || sideId == null) {
			return;
		}
		BattleSide side = battle.getSideById(sideId);
		if (side == null) {
			return;
		}
		side.setLives(0);
		if (battle.getBattleType() == BattleType.SIEGE) {
			SiegeWinService.checkSiegeWin(battle);
		} else if (battle.getBattleType() == BattleType.FIELD) {
			FieldWinService.checkFieldWin(battle);
		}
	}

	private static void detachFromBattle(Player player, Warband warband) {
		BattleManager.currentBattle.remove(player);
		BattleSide side = findSideForWarband(warband);
		if (side != null) {
			side.removeBossBarPlayer(player);
		}
	}

	private static BattleSide findSideForWarband(Warband warband) {
		for (Battle battle : BattleManager.get()) {
			for (BattleSide side : battle.getSides()) {
				if (side.getBands().contains(warband)) {
					return side;
				}
			}
		}
		return null;
	}

	public static boolean isWarSideMainLeader(
			me.Plugins.SimpleFactions.War.War war,
			Warband warband,
			String playerName) {
		if (war == null || warband == null || playerName == null || playerName.isBlank()) {
			return false;
		}
		String sideId = warband.getCampaignSideId();
		if (sideId == null) {
			return false;
		}
		me.Plugins.SimpleFactions.War.Side side = CampaignBattleJoinService.resolveWarSide(war, sideId);
		if (side == null) {
			return false;
		}
		for (me.Plugins.SimpleFactions.War.Participant par : side.getMainParticipants()) {
			if (par.getLeader() != null
					&& playerName.equalsIgnoreCase(par.getLeader().getLeader())) {
				return true;
			}
		}
		return false;
	}
}
