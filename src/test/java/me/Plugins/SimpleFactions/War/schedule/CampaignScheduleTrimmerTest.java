package me.Plugins.SimpleFactions.War.schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.War.enums.CampaignBattleKind;
import me.Plugins.SimpleFactions.War.enums.WarGoalType;

class CampaignScheduleTrimmerTest {

	@BeforeEach
	void setUp() {
		Cache.warGoalMaxBattles = new EnumMap<>(WarGoalType.class);
		Cache.warGoalMaxBattles.put(WarGoalType.SUBJUGATE, 4);
	}

	@Test
	void trim_noOpWhenUnderMax() {
		List<ScheduledCampaignBattle> schedule = List.of(
				field(10, false),
				field(20, false),
				field(30, true));

		List<ScheduledCampaignBattle> trimmed = CampaignScheduleTrimmer.trim(schedule, 4);

		assertEquals(3, trimmed.size());
		assertEquals(schedule, trimmed);
	}

	@Test
	void trim_dropsFieldsBeforeSieges() {
		List<ScheduledCampaignBattle> schedule = List.of(
				field(10, false),
				field(15, false),
				siege(18, "fort_a"),
				siege(22, "fort_b"),
				field(20, false),
				field(30, true));

		List<ScheduledCampaignBattle> trimmed = CampaignScheduleTrimmer.trim(schedule, 4);

		assertEquals(4, trimmed.size());
		assertTrue(trimmed.stream().anyMatch(slot -> slot.provinceId() == 30 && slot.required()));
		assertEquals(2, trimmed.stream().filter(slot -> slot.kind() == CampaignBattleKind.SIEGE).count());
		assertEquals(1, trimmed.stream().filter(slot -> slot.kind() == CampaignBattleKind.FIELD && !slot.required()).count());
		assertTrue(trimmed.stream().noneMatch(slot -> slot.provinceId() == 10));
		assertTrue(trimmed.stream().noneMatch(slot -> slot.provinceId() == 15));
	}

	@Test
	void trim_neverDropsRequiredObjective() {
		List<ScheduledCampaignBattle> schedule = List.of(
				field(10, false),
				field(15, false),
				field(20, false),
				field(30, true));

		List<ScheduledCampaignBattle> trimmed = CampaignScheduleTrimmer.trim(schedule, 1);

		assertEquals(1, trimmed.size());
		assertEquals(30, trimmed.get(0).provinceId());
		assertTrue(trimmed.get(0).required());
	}

	@Test
	void trim_navalKindsDropBeforeSiege() {
		List<ScheduledCampaignBattle> schedule = List.of(
				field(10, false),
				naval(12, CampaignBattleKind.NAVAL_INVASION),
				naval(14, CampaignBattleKind.NAVAL),
				siege(18, "fort_a"),
				field(30, true));

		List<ScheduledCampaignBattle> trimmed = CampaignScheduleTrimmer.trim(schedule, 3);

		assertEquals(3, trimmed.size());
		assertTrue(trimmed.stream().anyMatch(slot -> slot.kind() == CampaignBattleKind.NAVAL));
		assertTrue(trimmed.stream().anyMatch(slot -> slot.kind() == CampaignBattleKind.SIEGE));
		assertTrue(trimmed.stream().anyMatch(slot -> slot.provinceId() == 30 && slot.required()));
		assertTrue(trimmed.stream().noneMatch(slot -> slot.provinceId() == 10));
		assertTrue(trimmed.stream().noneMatch(slot -> slot.kind() == CampaignBattleKind.NAVAL_INVASION));
	}

	@Test
	void maxBattlesForGoal_readsCache() {
		assertEquals(4, CampaignScheduleTrimmer.maxBattlesForGoal(WarGoalType.SUBJUGATE));
		assertEquals(4, CampaignScheduleTrimmer.maxBattlesForGoal(null));
	}

	private static ScheduledCampaignBattle field(int provinceId, boolean required) {
		return new ScheduledCampaignBattle(provinceId, CampaignBattleKind.FIELD, required, null);
	}

	private static ScheduledCampaignBattle siege(int provinceId, String fortId) {
		return new ScheduledCampaignBattle(provinceId, CampaignBattleKind.SIEGE, false, fortId);
	}

	private static ScheduledCampaignBattle naval(int provinceId, CampaignBattleKind kind) {
		return new ScheduledCampaignBattle(provinceId, kind, false, null);
	}
}
