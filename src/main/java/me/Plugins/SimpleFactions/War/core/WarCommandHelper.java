package me.Plugins.SimpleFactions.War.core;

import java.util.Optional;

public final class WarCommandHelper {
	private WarCommandHelper() {}

	public static Optional<Integer> parseWarId(String value) {
		if (value == null || value.isBlank()) {
			return Optional.empty();
		}
		try {
			return Optional.of(Integer.parseInt(value));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}
}
