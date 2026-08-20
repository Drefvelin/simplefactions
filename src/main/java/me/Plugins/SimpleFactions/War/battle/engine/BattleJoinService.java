package me.Plugins.SimpleFactions.War.battle.engine;

import org.bukkit.entity.Player;

import me.Plugins.SimpleFactions.War.battle.warband.Warband;
import me.Plugins.SimpleFactions.War.battle.warband.WarbandManager;

public final class BattleJoinService {
	private BattleJoinService() {
	}

	/**
	 * @return null on success, or a player-facing error message
	 */
	public static String join(Player leader, Battle battle, String sideId) {
		if (leader == null) {
			return "Only players can join battles";
		}
		return join(WarbandManager.getByLeader(leader), battle, sideId);
	}

	/**
	 * @return null on success, or a player-facing error message
	 */
	public static String join(Warband warband, Battle battle, String sideId) {
		if (battle == null) {
			return "Battle not found";
		}
		if (sideId == null || sideId.isBlank()) {
			return "Side is required (attacker or defender)";
		}
		if (warband == null) {
			return "You need to lead a warband to join a battle";
		}
		if (battle.hasStarted()) {
			return "Battle has started";
		}
		if (battle.isLocked()) {
			return "Battle is locked";
		}
		if (BattleManager.getBattleByMemberId(warband.getLeaderId()) != null) {
			return "Already signed up for a battle";
		}
		BattleSide side = battle.getSideById(sideId);
		if (side == null) {
			return "No side with id " + sideId;
		}
		for (Warband band : side.getBands()) {
			if (band.getId().equalsIgnoreCase(warband.getId())) {
				return "Already signed up for this battle";
			}
		}
		side.addBand(warband);
		return null;
	}
}
