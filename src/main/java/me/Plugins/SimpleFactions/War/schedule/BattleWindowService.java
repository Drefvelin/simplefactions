package me.Plugins.SimpleFactions.War.schedule;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.War.War;

public final class BattleWindowService {
	private BattleWindowService() {}

	public static List<Integer> listValidHours() {
		int start = Cache.warBattleWindowStartHour;
		int end = Cache.warBattleWindowEndHour;
		List<Integer> hours = new ArrayList<>();
		for (int hour = start; hour <= end; hour++) {
			hours.add(hour);
		}
		return Collections.unmodifiableList(hours);
	}

	public static boolean isValidHour(int hour) {
		return hour >= Cache.warBattleWindowStartHour && hour <= Cache.warBattleWindowEndHour;
	}

	public static Instant computeScheduledBattleAt(LocalDate battleDay, int hour) {
		if (battleDay == null || !isValidHour(hour)) {
			return null;
		}
		if (hour == 24) {
			return battleDay.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
		}
		return battleDay.atTime(hour, 0).atZone(ZoneOffset.UTC).toInstant();
	}

	public static Instant computeScheduledBattleAt(War war, int hour) {
		if (war == null) {
			return null;
		}
		return computeScheduledBattleAt(war.getBattleDay(), hour);
	}
}
