package me.Plugins.SimpleFactions.War;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.enums.BattleSchedulePhase;
import me.Plugins.SimpleFactions.War.enums.CampaignPhase;
import me.Plugins.SimpleFactions.War.enums.ObjectiveHolder;
import me.Plugins.SimpleFactions.War.enums.WarGoalType;
import me.Plugins.SimpleFactions.War.enums.WarType;

class WarDebugFormatterTest {

	@Test
	void parseWarId_parsesValidInteger() {
		assertEquals(Optional.of(3), WarCommandHelper.parseWarId("3"));
	}

	@Test
	void parseWarId_rejectsInvalidInput() {
		assertTrue(WarCommandHelper.parseWarId("abc").isEmpty());
		assertTrue(WarCommandHelper.parseWarId("").isEmpty());
		assertTrue(WarCommandHelper.parseWarId(null).isEmpty());
	}

	@Test
	void formatStatusLines_includesCampaignFields() {
		Faction attacker = mock(Faction.class);
		Faction defender = mock(Faction.class);
		when(attacker.getId()).thenReturn("faction_a");
		when(defender.getId()).thenReturn("faction_b");
		when(attacker.getName()).thenReturn("Attacker");
		when(defender.getName()).thenReturn("Defender");

		War war = new War(3, attacker, defender);
		war.setGoal(WarGoalType.SUBJUGATE);
		war.setWarType(WarType.SUBJUGATE);
		war.setObjectiveProvinceId(42);
		war.setCampaignStartProvinceId(17);
		war.setCampaignProvinces(java.util.List.of(17, 23, 42));
		war.setCursorIndex(1);
		war.setInitiativeAttacker(4);
		war.setInitiativeDefender(3);
		war.setCampaignPhase(CampaignPhase.INVASION);
		war.setObjectiveHeldBy(ObjectiveHolder.DEFENDER);
		war.setCampaignBattlesFought(1);
		war.setLastBattleOccupied(java.util.List.of(23));
		war.setOccupiedByAttacker(java.util.List.of(23));
		war.setBattleSchedulePhase(BattleSchedulePhase.VOTING);
		war.setBattleDay(LocalDate.parse("2026-08-21"));
		war.setScheduledBattleHour(21);
		war.setScheduledBattleProvinceId(23);
		Map<UUID, Set<Integer>> votes = new HashMap<>();
		votes.put(UUID.randomUUID(), new HashSet<>(List.of(20, 21)));
		war.setBattleVotes(votes);
		war.setPostponementsThisCycle(2);

		String json = WarDebugFormatter.formatStatusLines(war).get(0);

		assertTrue(json.contains("\"objectiveProvinceId\":42"));
		assertTrue(json.contains("\"campaignStartProvinceId\":17"));
		assertTrue(json.contains("\"campaignProvinces\":[17,23,42]"));
		assertTrue(json.contains("\"cursorIndex\":1"));
		assertTrue(json.contains("\"cursorProvinceId\":23"));
		assertTrue(json.contains("\"initiativeAttacker\":4"));
		assertTrue(json.contains("\"initiativeDefender\":3"));
		assertTrue(json.contains("\"campaignPhase\":\"invasion\""));
		assertTrue(json.contains("\"campaignBattlesFought\":1"));
		assertTrue(json.contains("\"lastBattleOccupied\":[23]"));
		assertTrue(json.contains("\"nextBattleNodes\""));
		assertTrue(json.contains("\"battleSchedulePhase\":\"voting\""));
		assertTrue(json.contains("\"battleDay\":\"2026-08-21\""));
		assertTrue(json.contains("\"scheduledBattleHour\":21"));
		assertTrue(json.contains("\"scheduledBattleProvinceId\":23"));
		assertTrue(json.contains("\"battleVoteCount\":1"));
		assertTrue(json.contains("\"postponementsThisCycle\":2"));
	}

	@Test
	void formatStatusLines_includesV2FieldsWithDefaults() {
		Faction attacker = mock(Faction.class);
		Faction defender = mock(Faction.class);
		when(attacker.getId()).thenReturn("faction_a");
		when(defender.getId()).thenReturn("faction_b");
		when(attacker.getName()).thenReturn("Attacker");
		when(defender.getName()).thenReturn("Defender");

		War war = new War(3, attacker, defender);
		war.setGoal(WarGoalType.SUBJUGATE);
		war.setWarType(WarType.SUBJUGATE);

		List<String> lines = WarDebugFormatter.formatStatusLines(war);

		assertEquals(1, lines.size());
		String json = lines.get(0);
		assertTrue(json.contains("\"id\":3"));
		assertTrue(json.contains("\"goal\":\"subjugate\""));
		assertTrue(json.contains("\"initiativeAttacker\":0"));
		assertTrue(json.contains("\"initiativeDefender\":0"));
		assertTrue(json.contains("\"status\":\"active\""));
	}
}
