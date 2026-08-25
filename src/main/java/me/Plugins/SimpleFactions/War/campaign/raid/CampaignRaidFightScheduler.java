package me.Plugins.SimpleFactions.War.campaign.raid;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.scheduler.BukkitRunnable;

import me.Plugins.SimpleFactions.Managers.WarManager;
import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.War.battle.engine.core.Battle;
import me.Plugins.SimpleFactions.War.battle.engine.core.BattleEndSupport;
import me.Plugins.SimpleFactions.War.battle.engine.core.BattleManager;
import me.Plugins.SimpleFactions.War.battle.enums.BattleEndReason;
import me.Plugins.SimpleFactions.War.core.War;

public final class CampaignRaidFightScheduler {
	private static final Map<Integer, Integer> scheduledWarTasks = new ConcurrentHashMap<>();

	private CampaignRaidFightScheduler() {}

	static void resetForTests() {
		scheduledWarTasks.clear();
	}

	public static void onFightStarted(War war, Instant now) {
		if (war == null || now == null) {
			return;
		}
		CampaignRaid raid = CampaignRaidService.getActive(war);
		if (raid == null || raid.getState() != CampaignRaidState.FIGHTING || raid.getFightEndsAt() == null) {
			return;
		}
		cancelScheduled(war.getId());
		long delayTicks = delayTicksUntil(raid.getFightEndsAt(), now);
		if (delayTicks <= 0L) {
			onFightEnd(war, now);
			return;
		}
		if (!canScheduleTasks()) {
			return;
		}
		int warId = war.getId();
		int taskId = new BukkitRunnable() {
			@Override
			public void run() {
				scheduledWarTasks.remove(warId);
				War current = WarManager.getById(warId);
				if (current != null) {
					onFightEnd(current, Instant.now());
				}
			}
		}.runTaskLater(SimpleFactions.plugin, delayTicks).getTaskId();
		scheduledWarTasks.put(warId, taskId);
	}

	public static boolean processOverdue(War war, Instant now) {
		if (war == null || now == null) {
			return false;
		}
		CampaignRaid raid = CampaignRaidService.getActive(war);
		if (raid == null || raid.getState() != CampaignRaidState.FIGHTING) {
			return false;
		}
		if (raid.getFightEndsAt() == null || now.isBefore(raid.getFightEndsAt())) {
			return false;
		}
		onFightEnd(war, now);
		return true;
	}

	static void onFightEnd(War war, Instant now) {
		if (war == null) {
			return;
		}
		cancelScheduled(war.getId());
		CampaignRaid raid = CampaignRaidService.getActive(war);
		if (raid == null || raid.getState() != CampaignRaidState.FIGHTING) {
			return;
		}
		String battleId = raid.getBattleId();
		if (battleId == null || battleId.isBlank()) {
			return;
		}
		Battle battle = BattleManager.getByString(battleId);
		if (battle == null || !battle.hasStarted()) {
			return;
		}
		BattleEndSupport.endBattle(battle, null, BattleEndReason.TIMER);
	}

	private static long delayTicksUntil(Instant fightEndsAt, Instant now) {
		long seconds = fightEndsAt.getEpochSecond() - now.getEpochSecond();
		if (seconds <= 0L) {
			return 0L;
		}
		return seconds * 20L;
	}

	private static void cancelScheduled(int warId) {
		Integer taskId = scheduledWarTasks.remove(warId);
		if (taskId != null && canScheduleTasks()) {
			SimpleFactions.plugin.getServer().getScheduler().cancelTask(taskId);
		}
	}

	private static boolean canScheduleTasks() {
		return SimpleFactions.plugin != null && SimpleFactions.plugin.getServer() != null;
	}
}
