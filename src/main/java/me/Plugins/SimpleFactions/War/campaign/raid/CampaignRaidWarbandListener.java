package me.Plugins.SimpleFactions.War.campaign.raid;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import me.Plugins.SimpleFactions.War.battle.warband.Warband;
import me.Plugins.SimpleFactions.War.battle.warband.WarbandManager;

public class CampaignRaidWarbandListener implements Listener {
	private final Map<UUID, String> pendingLeaderPromotion = new ConcurrentHashMap<>();

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		CampaignRaidWarbandService.tryEnrollDefenderOnLogin(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onQuitLow(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		Warband warband = WarbandManager.getByMemberId(player.getUniqueId());
		if (warband == null || !CampaignRaidWarbandService.isRaidWarband(warband)) {
			return;
		}
		if (player.getUniqueId().equals(warband.getLeaderId())) {
			pendingLeaderPromotion.put(player.getUniqueId(), warband.getId());
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onQuitMonitor(PlayerQuitEvent event) {
		String warbandId = pendingLeaderPromotion.remove(event.getPlayer().getUniqueId());
		if (warbandId == null) {
			return;
		}
		Warband warband = WarbandManager.getByString(warbandId);
		if (warband != null) {
			CampaignRaidWarbandService.promoteLeaderIfNeeded(warband);
		}
	}

	void resetForTests() {
		pendingLeaderPromotion.clear();
	}
}
