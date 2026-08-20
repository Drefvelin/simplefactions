package me.Plugins.SimpleFactions.War.battle.engine;

import org.bukkit.Bukkit;

import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.War.battle.events.BattleEndedEvent;

final class BattleEndSupport {
	private BattleEndSupport() {
	}

	static void endBattle(Battle battle, String winningSideId) {
		SiegeContestService.clearBattleState(battle);
		RaidAttackerEliminationService.clearBattleState(battle);
		battle.endTitle();
		battle.end();
		if (SimpleFactions.plugin != null) {
			Bukkit.getPluginManager().callEvent(
					new BattleEndedEvent(
							battle.getId(),
							battle.getBattleType(),
							battle.getWarId(),
							winningSideId));
		}
	}
}
