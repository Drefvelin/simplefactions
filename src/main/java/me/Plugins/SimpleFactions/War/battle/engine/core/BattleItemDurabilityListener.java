package me.Plugins.SimpleFactions.War.battle.engine.core;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;

import me.Plugins.SimpleFactions.Cache;

public final class BattleItemDurabilityListener implements Listener {

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onItemDamage(PlayerItemDamageEvent event) {
		Player player = event.getPlayer();
		Battle battle = BattleManager.getBattleByPlayer(player);
		if (battle == null || !battle.hasStarted()) {
			return;
		}
		int next = BattleItemDurability.apply(
				event.getDamage(),
				Cache.battleItemDurabilityMultiplier,
				Math::random);
		if (next <= 0) {
			event.setCancelled(true);
			return;
		}
		event.setDamage(next);
	}
}
