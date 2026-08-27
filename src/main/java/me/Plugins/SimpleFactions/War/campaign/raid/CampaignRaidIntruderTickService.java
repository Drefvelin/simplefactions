package me.Plugins.SimpleFactions.War.campaign.raid;

import org.bukkit.scheduler.BukkitRunnable;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.SimpleFactions;

public final class CampaignRaidIntruderTickService {
	private CampaignRaidIntruderTickService() {}

	public static void start() {
		long interval = Math.max(1L, Cache.campaignRaidIntruderDamageIntervalTicks);
		new BukkitRunnable() {
			@Override
			public void run() {
				CampaignRaidIntruderService.processTick();
			}
		}.runTaskTimer(SimpleFactions.plugin, interval, interval);
	}
}
