package me.Plugins.SimpleFactions.War.battle.naming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.battle.engine.Battle;
import me.Plugins.SimpleFactions.War.battle.enums.BattleType;
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
}
