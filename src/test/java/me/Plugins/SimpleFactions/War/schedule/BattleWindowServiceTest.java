package me.Plugins.SimpleFactions.War.schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import me.Plugins.SimpleFactions.Cache;

class BattleWindowServiceTest {
	@BeforeEach
	void setUp() {
		Cache.warBattleWindowStartHour = 20;
		Cache.warBattleWindowEndHour = 24;
	}

	@Test
	void listValidHours_defaultWindow() {
		assertEquals(List.of(20, 21, 22, 23, 24), BattleWindowService.listValidHours());
	}

	@Test
	void isValidHour_acceptsWindowBounds() {
		assertTrue(BattleWindowService.isValidHour(20));
		assertTrue(BattleWindowService.isValidHour(24));
		assertFalse(BattleWindowService.isValidHour(19));
		assertFalse(BattleWindowService.isValidHour(25));
	}

	@Test
	void computeScheduledBattleAt_hour20_usesBattleDay() {
		LocalDate battleDay = LocalDate.of(2026, 8, 21);
		Instant instant = BattleWindowService.computeScheduledBattleAt(battleDay, 20);
		assertEquals(battleDay.atTime(20, 0).atZone(ZoneOffset.UTC).toInstant(), instant);
	}

	@Test
	void computeScheduledBattleAt_hour24_usesNextDayMidnight() {
		LocalDate battleDay = LocalDate.of(2026, 8, 21);
		Instant instant = BattleWindowService.computeScheduledBattleAt(battleDay, 24);
		assertEquals(battleDay.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant(), instant);
	}

	@Test
	void computeScheduledBattleAt_rejectsInvalidHour() {
		assertNull(BattleWindowService.computeScheduledBattleAt(LocalDate.of(2026, 8, 21), 19));
		LocalDate missingDay = null;
		assertNull(BattleWindowService.computeScheduledBattleAt(missingDay, 20));
	}
}
