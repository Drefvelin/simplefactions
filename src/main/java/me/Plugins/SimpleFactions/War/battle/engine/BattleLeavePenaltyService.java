package me.Plugins.SimpleFactions.War.battle.engine;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.Events.PlayerProvinceEnterEvent;
import me.Plugins.SimpleFactions.Events.PlayerProvinceLeaveEvent;
import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.War.battle.enums.BattleType;

public class BattleLeavePenaltyService implements Listener {
	private static final Set<UUID> PENALTY_RESPAWNS = ConcurrentHashMap.newKeySet();

	private final Map<UUID, BukkitTask> countdowns = new ConcurrentHashMap<>();
	private final Map<UUID, Integer> remainingSeconds = new ConcurrentHashMap<>();

	public static boolean isPenaltyRespawn(UUID playerId) {
		return playerId != null && PENALTY_RESPAWNS.contains(playerId);
	}

	public static void clearPenaltyRespawn(UUID playerId) {
		if (playerId != null) {
			PENALTY_RESPAWNS.remove(playerId);
		}
	}

	public static void resetForTests() {
		PENALTY_RESPAWNS.clear();
	}

	@EventHandler
	public void onProvinceLeave(PlayerProvinceLeaveEvent event) {
		Player player = event.getPlayer();
		Battle battle = BattleManager.getBattleByPlayer(player);
		if (!applies(battle)) {
			return;
		}
		if (!BattleBoundsService.isProvinceAllowed(battle, event.getProvinceId())) {
			return;
		}
		Integer nextProvinceId = event.getNextProvinceId();
		if (nextProvinceId != null && BattleBoundsService.isProvinceAllowed(battle, nextProvinceId)) {
			return;
		}
		startCountdown(player);
	}

	@EventHandler
	public void onProvinceEnter(PlayerProvinceEnterEvent event) {
		Player player = event.getPlayer();
		Battle battle = BattleManager.getBattleByPlayer(player);
		if (!applies(battle)) {
			return;
		}
		if (BattleBoundsService.isProvinceAllowed(battle, event.getProvinceId())) {
			cancelCountdown(player);
		}
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		cancelCountdown(event.getPlayer());
	}

	private static boolean applies(Battle battle) {
		if (battle == null || !battle.hasStarted()) {
			return false;
		}
		BattleType type = battle.getBattleType();
		return type == BattleType.FIELD || type == BattleType.SIEGE;
	}

	static boolean shouldStartCountdown(Battle battle, int leftProvinceId, Integer nextProvinceId) {
		if (!applies(battle)) {
			return false;
		}
		if (!BattleBoundsService.isProvinceAllowed(battle, leftProvinceId)) {
			return false;
		}
		return nextProvinceId == null || !BattleBoundsService.isProvinceAllowed(battle, nextProvinceId);
	}

	private void startCountdown(Player player) {
		if (player == null) {
			return;
		}
		cancelCountdown(player);
		int seconds = Math.max(1, Cache.battleProvinceLeaveCountdownSeconds);
		remainingSeconds.put(player.getUniqueId(), seconds);
		BukkitTask task = Bukkit.getScheduler().runTaskTimer(SimpleFactions.plugin, () -> {
			Integer left = remainingSeconds.get(player.getUniqueId());
			if (left == null) {
				return;
			}
			if (!player.isOnline()) {
				cancelCountdown(player);
				return;
			}
			if (left <= 0) {
				cancelCountdown(player);
				PENALTY_RESPAWNS.add(player.getUniqueId());
				player.setHealth(0.0);
				return;
			}
			player.sendTitle("§cReturn to battle!", "§e" + left + "s until elimination", 0, 25, 0);
			player.sendMessage("§cYou left the battle area! Return within §e" + left + "s§c or you will be eliminated.");
			remainingSeconds.put(player.getUniqueId(), left - 1);
		}, 0L, 20L);
		countdowns.put(player.getUniqueId(), task);
	}

	private void cancelCountdown(Player player) {
		if (player == null) {
			return;
		}
		UUID playerId = player.getUniqueId();
		BukkitTask task = countdowns.remove(playerId);
		if (task != null) {
			task.cancel();
		}
		remainingSeconds.remove(playerId);
	}
}
