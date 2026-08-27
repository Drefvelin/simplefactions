package me.Plugins.SimpleFactions.War.campaign.raid;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import me.Plugins.SimpleFactions.Events.PlayerProvinceEnterEvent;

public final class CampaignRaidIntruderListener implements Listener {
	@EventHandler
	public void onProvinceEnter(PlayerProvinceEnterEvent event) {
		if (event == null || event.getPlayer() == null) {
			return;
		}
		CampaignRaidIntruderService.onProvinceEnter(event.getPlayer(), event.getProvinceId());
	}
}
