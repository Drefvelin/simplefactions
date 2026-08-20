package me.Plugins.SimpleFactions.War;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import me.Plugins.SimpleFactions.Database.ParticipantData;
import me.Plugins.SimpleFactions.Database.SideData;
import me.Plugins.SimpleFactions.Database.WarData;

class WarDataRoundTripTest {
	private static final Gson GSON = new Gson();

	@Test
	void warData_gsonRoundTrip_preservesV2Fields() {
		WarData original = new WarData();
		original.schemaVersion = 2;
		original.id = 3;
		original.status = "active";
		original.goal = "subjugate";
		original.warType = "subjugate";
		original.attackerLeaderId = "faction_a";
		original.defenderLeaderId = "faction_b";
		original.targetTitleId = "county_x";
		original.objectiveProvinceId = 42;
		original.campaignStartProvinceId = 17;
		original.campaignProvinces = new java.util.ArrayList<>(java.util.List.of(17, 23, 42));
		original.cursorIndex = 1;
		original.initiativeAttacker = 4;
		original.initiativeDefender = 3;
		original.occupiedByAttacker = new java.util.ArrayList<>(java.util.List.of(17));
		original.campaignPhase = "invasion";
		original.objectiveHeldBy = "defender";
		original.whitePeaceProposedByDefender = true;
		original.campaignBattlesFought = 2;
		original.battleSchedulePhase = "voting";
		original.battleDay = "2026-08-21";
		original.scheduledBattleAt = "2026-08-21T21:00:00Z";
		original.scheduledBattleHour = 21;
		original.scheduledBattleProvinceId = 23;
		original.battleVotes = new HashMap<>();
		original.battleVotes.put("00000000-0000-0000-0000-000000000002", new java.util.ArrayList<>(List.of(20, 21)));
		original.autoresolveProposedByAttacker = true;
		original.postponementsThisCycle = 2;
		original.defenderChoiceResolved = true;
		original.subjectFactionId = "subject_a";
		original.startedAt = "2026-08-19T12:00:00Z";
		original.endedAt = null;
		original.endReason = null;

		original.attackers = new SideData();
		original.attackers.leader = "faction_a";
		ParticipantData attacker = new ParticipantData();
		attacker.leader = "faction_a";
		attacker.civilWar = false;
		original.attackers.participants.add(attacker);

		original.defenders = new SideData();
		original.defenders.leader = "faction_b";
		ParticipantData defender = new ParticipantData();
		defender.leader = "faction_b";
		defender.civilWar = true;
		original.defenders.participants.add(defender);

		String json = GSON.toJson(original);
		WarData restored = GSON.fromJson(json, WarData.class);

		assertNotNull(restored);
		assertEquals(2, restored.schemaVersion);
		assertEquals(3, restored.id);
		assertEquals("active", restored.status);
		assertEquals("subjugate", restored.goal);
		assertEquals("subjugate", restored.warType);
		assertEquals("faction_a", restored.attackerLeaderId);
		assertEquals("faction_b", restored.defenderLeaderId);
		assertEquals("county_x", restored.targetTitleId);
		assertEquals(Integer.valueOf(42), restored.objectiveProvinceId);
		assertEquals(Integer.valueOf(17), restored.campaignStartProvinceId);
		assertEquals(java.util.List.of(17, 23, 42), restored.campaignProvinces);
		assertEquals(1, restored.cursorIndex);
		assertEquals(Integer.valueOf(4), restored.initiativeAttacker);
		assertEquals(Integer.valueOf(3), restored.initiativeDefender);
		assertEquals(java.util.List.of(17), restored.occupiedByAttacker);
		assertEquals("invasion", restored.campaignPhase);
		assertEquals("defender", restored.objectiveHeldBy);
		assertTrue(restored.whitePeaceProposedByDefender);
		assertEquals(Integer.valueOf(2), restored.campaignBattlesFought);
		assertEquals("voting", restored.battleSchedulePhase);
		assertEquals("2026-08-21", restored.battleDay);
		assertEquals("2026-08-21T21:00:00Z", restored.scheduledBattleAt);
		assertEquals(Integer.valueOf(21), restored.scheduledBattleHour);
		assertEquals(Integer.valueOf(23), restored.scheduledBattleProvinceId);
		assertEquals(List.of(20, 21), restored.battleVotes.get("00000000-0000-0000-0000-000000000002"));
		assertTrue(restored.autoresolveProposedByAttacker);
		assertEquals(Integer.valueOf(2), restored.postponementsThisCycle);
		assertTrue(restored.defenderChoiceResolved);
		assertEquals("subject_a", restored.subjectFactionId);
		assertEquals("2026-08-19T12:00:00Z", restored.startedAt);
		assertEquals("faction_a", restored.attackers.leader);
		assertEquals(1, restored.attackers.participants.size());
		assertEquals(true, restored.defenders.participants.get(0).civilWar);
	}
}
