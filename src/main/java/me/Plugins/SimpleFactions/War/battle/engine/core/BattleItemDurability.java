package me.Plugins.SimpleFactions.War.battle.engine.core;

import java.util.function.DoubleSupplier;

/**
 * Scales {@link org.bukkit.event.player.PlayerItemDamageEvent} amounts for started battles.
 * Sub-1 scaled damage uses a probability so a typical 1-damage hit is not always cancelled.
 */
public final class BattleItemDurability {

	private BattleItemDurability() {
	}

	public static int apply(int damage, double multiplier, DoubleSupplier random) {
		if (damage <= 0) {
			return 0;
		}
		double factor = clampUnit(multiplier);
		if (factor <= 0.0) {
			return 0;
		}
		if (factor >= 1.0) {
			return damage;
		}
		double scaled = damage * factor;
		if (scaled < 1.0) {
			double roll = random == null ? 1.0 : random.getAsDouble();
			return roll < scaled ? damage : 0;
		}
		return Math.max(1, (int) Math.round(scaled));
	}

	static double clampUnit(double value) {
		if (value < 0.0) {
			return 0.0;
		}
		if (value > 1.0) {
			return 1.0;
		}
		return value;
	}
}
