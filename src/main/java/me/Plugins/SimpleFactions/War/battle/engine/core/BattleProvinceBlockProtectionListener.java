package me.Plugins.SimpleFactions.War.battle.engine.core;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import me.Plugins.SimpleFactions.Utils.Permissions;
import me.Plugins.SimpleFactions.War.battle.ui.BattlePermissions;

public final class BattleProvinceBlockProtectionListener implements Listener {

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		handleBlockChange(event.getPlayer(), event.getBlock().getLocation(), event);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {
		handleBlockChange(event.getPlayer(), event.getBlock().getLocation(), event);
	}

	private static void handleBlockChange(
			Player player,
			org.bukkit.Location location,
			org.bukkit.event.Cancellable event) {
		if (isStaffBypass(player)) {
			return;
		}
		if (BattleProvinceBlockProtectionService.isPlayerBlockChangeBlocked(location)) {
			event.setCancelled(true);
			if (player != null) {
				player.sendMessage(BattleProvinceBlockProtectionService.BLOCKED);
			}
		}
	}

	private static boolean isStaffBypass(Player player) {
		return player != null && (Permissions.isAdmin(player) || BattlePermissions.isAdmin(player));
	}
}
