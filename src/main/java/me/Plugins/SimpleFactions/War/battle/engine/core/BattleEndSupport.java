package me.Plugins.SimpleFactions.War.battle.engine.core;

import java.util.Map;

import org.bukkit.Bukkit;

import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.War.battle.events.BattleEndedEvent;
import me.Plugins.SimpleFactions.War.battle.engine.raid.RaidAttackerEliminationService;
import me.Plugins.SimpleFactions.War.battle.engine.win.SiegeContestService;
import me.Plugins.SimpleFactions.War.battle.military.BattleCasualtyLedger;

public final class BattleEndSupport {
	private BattleEndSupport() {
	}

	public static void endBattle(Battle battle, String winningSideId) {
		SiegeContestService.clearBattleState(battle);
		RaidAttackerEliminationService.clearBattleState(battle);
		battle.endTitle();
		Map<String, Integer> sideCasualties = BattleCasualtyLedger.getSideCasualties(battle);
		battle.end();
		if (SimpleFactions.plugin != null) {
			Bukkit.getPluginManager().callEvent(
					new BattleEndedEvent(
							battle.getId(),
							battle.getBattleType(),
							battle.getWarId(),
							winningSideId,
							sideCasualties));
		}
	}
}
