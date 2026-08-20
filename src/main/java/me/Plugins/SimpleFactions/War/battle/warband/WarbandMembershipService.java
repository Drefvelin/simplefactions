package me.Plugins.SimpleFactions.War.battle.warband;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.battle.engine.Battle;
import me.Plugins.SimpleFactions.War.battle.engine.BattleManager;
import me.Plugins.SimpleFactions.War.battle.engine.BattleSide;
import me.Plugins.SimpleFactions.War.battle.enums.LifeType;

public class WarbandMembershipService {
	private static WarbandMembershipService instance;

	private final Map<UUID, WarbandRejoinState> pendingRejoin = new HashMap<>();

	public static WarbandMembershipService getInstance() {
		if (instance == null) {
			instance = new WarbandMembershipService();
		}
		return instance;
	}

	public static void resetForTests() {
		instance = new WarbandMembershipService();
	}

	public void handleQuit(UUID playerId) {
		Warband warband = WarbandManager.getByMemberId(playerId);
		if (warband == null) {
			return;
		}
		Faction faction = resolveFactionForSlot(warband, playerId);
		detachFromBattle(playerId, warband);
		pendingRejoin.put(playerId, new WarbandRejoinState(warband.getId(), faction));
		warband.removeMember(playerId);
		if (faction != null) {
			WarbandSlot slot = warband.getSlot(faction);
			if (slot != null) {
				slot.change(-1);
			}
		}
	}

	public boolean handleJoin(Player player) {
		return attemptRejoin(player.getUniqueId(), player.getName(), player);
	}

	boolean attemptRejoin(UUID playerId, String playerName, Player onlinePlayer) {
		WarbandRejoinState state = pendingRejoin.remove(playerId);
		if (state == null) {
			return false;
		}
		Warband warband = WarbandManager.getByString(state.getWarbandId());
		if (warband == null) {
			return false;
		}
		Faction playerFaction = warband.isFaction() ? FactionManager.getByMember(playerName) : null;
		if (!evaluateRejoin(warband, playerId, playerFaction, state)) {
			return false;
		}
		warband.addMember(playerId);
		if (state.hasFaction()) {
			Faction faction = findFactionInWarband(warband, state.getFactionId());
			if (faction != null) {
				WarbandSlot slot = warband.getSlot(faction);
				if (slot != null) {
					slot.change(1);
				}
			}
		}
		if (onlinePlayer != null) {
			restoreBattleState(onlinePlayer);
		}
		return true;
	}

	public WarbandRejoinState getPendingRejoin(UUID playerId) {
		return pendingRejoin.get(playerId);
	}

	public void clearPendingRejoin(UUID playerId) {
		pendingRejoin.remove(playerId);
	}

	public boolean evaluateRejoin(Warband warband, UUID playerId, Faction playerFaction, WarbandRejoinState state) {
		if (warband.isFaction()) {
			if (!state.hasFaction()) {
				return false;
			}
			Faction faction = findFactionInWarband(warband, state.getFactionId());
			if (faction == null) {
				return false;
			}
			if (playerFaction == null || !playerFaction.equals(faction)) {
				return false;
			}
			WarbandSlot slot = warband.getSlot(faction);
			return slot != null && !slot.isFull();
		}
		if (warband.isLocked()) {
			Player player = getOnlinePlayer(playerId);
			return player != null && warband.isInvited(player);
		}
		return true;
	}

	private Player getOnlinePlayer(UUID playerId) {
		if (Bukkit.getServer() == null) {
			return null;
		}
		return Bukkit.getPlayer(playerId);
	}

	private Faction resolveFactionForSlot(Warband warband, UUID playerId) {
		if (!warband.isFaction()) {
			return null;
		}
		Player player = getOnlinePlayer(playerId);
		if (player == null) {
			return null;
		}
		Faction faction = FactionManager.getByMember(player.getName());
		if (faction == null || !warband.hasSlot(faction)) {
			return null;
		}
		return faction;
	}

	private Faction findFactionInWarband(Warband warband, String factionId) {
		for (Faction faction : warband.getSlots().keySet()) {
			if (faction.getId().equals(factionId)) {
				return faction;
			}
		}
		return null;
	}

	private void detachFromBattle(UUID playerId, Warband warband) {
		Player player = getOnlinePlayer(playerId);
		if (player == null) {
			return;
		}
		BattleManager.currentBattle.remove(player);
		BattleSide side = findSideForWarband(warband);
		if (side != null) {
			side.removeBossBarPlayer(player);
		}
	}

	private BattleSide findSideForWarband(Warband warband) {
		for (Battle battle : BattleManager.get()) {
			for (BattleSide side : battle.getSides()) {
				if (side.getBands().contains(warband)) {
					return side;
				}
			}
		}
		return null;
	}

	private void restoreBattleState(Player player) {
		Battle battle = BattleManager.getBattleByMemberId(player.getUniqueId());
		if (battle == null || !battle.hasStarted()) {
			return;
		}
		BattleSide side = battle.getSideByMemberId(player.getUniqueId());
		if (side == null) {
			return;
		}
		side.addBossBarPlayer(player);
		if (battle.getLifeType().equals(LifeType.PER_PLAYER) && !side.hasRecord(player)) {
			side.addRecord(player, battle.getLives());
		}
	}
}
