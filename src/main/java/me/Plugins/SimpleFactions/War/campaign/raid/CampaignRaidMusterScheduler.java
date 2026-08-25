package me.Plugins.SimpleFactions.War.campaign.raid;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.scheduler.BukkitRunnable;

import me.Plugins.SimpleFactions.Managers.WarManager;
import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.War.core.War;

public final class CampaignRaidMusterScheduler {
	private static final Map<Integer, Integer> scheduledWarTasks = new ConcurrentHashMap<>();

	private CampaignRaidMusterScheduler() {}

	static void resetForTests() {
		scheduledWarTasks.clear();
	}

	public static void onMusterStarted(War war, Instant now) {
		if (war == null || now == null) {
			return;
		}
		CampaignRaid raid = war.getActiveCampaignRaid();
		if (raid == null || raid.getMusterEndsAt() == null) {
			return;
		}
		cancelScheduled(war.getId());
		long delayTicks = delayTicksUntil(raid.getMusterEndsAt(), now);
		if (delayTicks <= 0L) {
			onMusterEnd(war, now);
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
					onMusterEnd(current, Instant.now());
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
		if (raid == null || raid.getState() != CampaignRaidState.MUSTER) {
			return false;
		}
		if (raid.getMusterEndsAt() == null || now.isBefore(raid.getMusterEndsAt())) {
			return false;
		}
		onMusterEnd(war, now);
		return true;
	}

	static void onMusterEnd(War war, Instant now) {
		if (war == null || now == null) {
			return;
		}
		cancelScheduled(war.getId());
		CampaignRaid raid = CampaignRaidService.getActive(war);
		if (raid == null || raid.getState() != CampaignRaidState.MUSTER) {
			return;
		}
		CampaignRaidLaunchService.startFight(war, now);
	}

	private static long delayTicksUntil(Instant musterEndsAt, Instant now) {
		long seconds = musterEndsAt.getEpochSecond() - now.getEpochSecond();
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
