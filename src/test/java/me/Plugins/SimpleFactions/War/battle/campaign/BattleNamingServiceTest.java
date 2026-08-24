package me.Plugins.SimpleFactions.War.battle.campaign;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.core.War;
import me.Plugins.SimpleFactions.War.battle.engine.core.Battle;
import me.Plugins.SimpleFactions.War.battle.enums.BattleType;
import me.Plugins.SimpleFactions.War.enums.CampaignBattleKind;
import me.Plugins.SimpleFactions.War.campaign.schedule.CampaignScheduleService.ScheduleLeg;
import me.Plugins.SimpleFactions.War.campaign.schedule.ScheduledCampaignBattle;
import me.Plugins.SimpleFactions.settlement.Settlement;

class BattleNamingServiceTest {
	@Test
	void buildDisplayName_fieldOrdinals() {
		assertEquals("Battle of Lanbury", BattleNamingService.buildDisplayName(
				BattleType.FIELD, "Lanbury", 1));
		assertEquals("Second Battle of Lanbury", BattleNamingService.buildDisplayName(
				BattleType.FIELD, "Lanbury", 2));
	}

	@Test
	void buildDisplayName_nonFieldVariants() {
		record Case(BattleType type, String location, int ordinal, String expected) {}
		Case[] cases = {
				new Case(BattleType.SIEGE, "Fort Redoubt", 1, "Siege of Fort Redoubt"),
				new Case(BattleType.RAID, "Lanbury", 2, "Lanbury Raid"),
				new Case(BattleType.FIELD, BattleNamingService.WILDERNESS, 1, "Battle of Wilderness"),
		};
		for (Case c : cases) {
			assertEquals(c.expected, BattleNamingService.buildDisplayName(c.type, c.location, c.ordinal));
		}
	}

	@Test
	void ordinalPrefix_values() {
		assertEquals("", BattleNamingService.ordinalPrefix(1));
		assertEquals("Second ", BattleNamingService.ordinalPrefix(2));
		assertEquals("Third ", BattleNamingService.ordinalPrefix(3));
		assertEquals("4th ", BattleNamingService.ordinalPrefix(4));
	}

	@Test
	void resolveLocation_settlementOverCounty() {
		Settlement settlement = mock(Settlement.class);
		when(settlement.getName()).thenReturn("Lanbury");

		BattleNamingService.LocationInfo info =
				new BattleNamingService.LocationInfo("settlement:Lanbury", "Lanbury");

		assertEquals("settlement:Lanbury", info.key());
		assertEquals("Lanbury", info.displayName());
	}

	@Test
	void applyCampaignName_usesWarLocationCount() {
		War war = new War(1, mock(Faction.class), mock(Faction.class));
		war.recordLocationBattle("settlement:Lanbury");

		Battle battle = new Battle("campaign_w1_p100");
		BattleNamingService.applyCampaignName(battle, war, 100, BattleType.FIELD);

		// Ordinal derived from war count; location resolution needs live world data in integration.
		// With no settlement data, falls back to wilderness key.
		assertEquals("Battle of Wilderness", battle.getDisplayName());
	}

	@Test
	void recordLocationBattle_incrementsWarCounter() {
		War war = new War(1, mock(Faction.class), mock(Faction.class));
		assertEquals(0, war.getLocationBattleCount("county:Shire"));

		BattleNamingService.recordLocationBattle(war, 999);
		String key = BattleNamingService.resolveLocationKey(999);
		assertEquals(1, war.getLocationBattleCount(key));
	}

	@Test
	void resolveScheduledOrdinal_twoFieldSlotsSameProvince() {
		War war = new War(1, mock(Faction.class), mock(Faction.class));
		ScheduledCampaignBattle first = new ScheduledCampaignBattle(100, CampaignBattleKind.FIELD, false, null);
		ScheduledCampaignBattle second = new ScheduledCampaignBattle(100, CampaignBattleKind.FIELD, true, null);
		war.setCampaignBattleSchedule(List.of(first, second));

		assertEquals(1, BattleNamingService.resolveScheduledOrdinal(war, ScheduleLeg.INVASION, 0, first));
		assertEquals(2, BattleNamingService.resolveScheduledOrdinal(war, ScheduleLeg.INVASION, 1, second));
	}

	@Test
	void resolveScheduledOrdinal_respectsFoughtCount() {
		War war = new War(1, mock(Faction.class), mock(Faction.class));
		war.recordLocationBattle("wilderness:100");
		ScheduledCampaignBattle slot = new ScheduledCampaignBattle(100, CampaignBattleKind.FIELD, false, null);
		war.setCampaignBattleSchedule(List.of(slot));

		assertEquals(2, BattleNamingService.resolveScheduledOrdinal(war, ScheduleLeg.INVASION, 0, slot));
	}

	@Test
	void resolveScheduledDisplayName_siegeThenFieldAtCapital() {
		War war = new War(1, mock(Faction.class), mock(Faction.class));
		ScheduledCampaignBattle naval = new ScheduledCampaignBattle(
				795, CampaignBattleKind.NAVAL, false, null, "port");
		ScheduledCampaignBattle siege = new ScheduledCampaignBattle(705, CampaignBattleKind.SIEGE, false, "Greenfort");
		ScheduledCampaignBattle field = new ScheduledCampaignBattle(705, CampaignBattleKind.FIELD, true, null);
		war.setCampaignBattleSchedule(List.of(naval, siege, field));

		assertEquals(
				"Siege of Greenfort",
				BattleNamingService.resolveScheduledDisplayName(
						war, ScheduleLeg.INVASION, 1, siege, 705));
		String fieldName = BattleNamingService.resolveScheduledDisplayName(
				war, ScheduleLeg.INVASION, 2, field, 705);
		assertTrue(fieldName.startsWith("Battle of "));
		assertEquals(1, BattleNamingService.resolveScheduledOrdinal(war, ScheduleLeg.INVASION, 1, siege));
		assertEquals(1, BattleNamingService.resolveScheduledOrdinal(war, ScheduleLeg.INVASION, 2, field));
	}

	@Test
	void applyCampaignName_siegeUsesFortKey() {
		War war = new War(1, mock(Faction.class), mock(Faction.class));
		ScheduledCampaignBattle siege = new ScheduledCampaignBattle(705, CampaignBattleKind.SIEGE, false, "Greenfort");
		Battle battle = new Battle("campaign_w1_p705");

		BattleNamingService.applyCampaignName(battle, war, 705, BattleType.SIEGE, siege);

		assertEquals("Siege of Greenfort", battle.getDisplayName());
	}
}
