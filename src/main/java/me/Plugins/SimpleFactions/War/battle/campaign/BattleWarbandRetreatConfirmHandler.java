package me.Plugins.SimpleFactions.War.battle.campaign;

import java.time.Instant;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.War.battle.campaign.BattleWarbandRetreatService.RetreatResult;

public final class BattleWarbandRetreatConfirmHandler {
	private BattleWarbandRetreatConfirmHandler() {
	}

	public static void handleConfirm(Player player, boolean confirmed) {
		if (player == null) {
			return;
		}
		if (FactionManager.inv != null) {
			FactionManager.inv.confirming.remove(player);
		}
		if (!confirmed) {
			player.closeInventory();
			return;
		}
		Instant now = Instant.now();
		RetreatResult result = BattleWarbandRetreatService.retreat(player, now);
		String message = BattleWarbandRetreatMessages.messageForResult(result, player, now);
		if (message != null) {
			player.sendMessage(message);
		}
		if (result == RetreatResult.SUCCESS) {
			player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
		}
	}
}
