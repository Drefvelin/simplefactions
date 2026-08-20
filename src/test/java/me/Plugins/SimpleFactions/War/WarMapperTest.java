package me.Plugins.SimpleFactions.War;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import me.Plugins.SimpleFactions.Database.WarData;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.enums.BattleSchedulePhase;
import me.Plugins.SimpleFactions.War.enums.CampaignPhase;
import me.Plugins.SimpleFactions.War.enums.ObjectiveHolder;
import me.Plugins.SimpleFactions.War.enums.WarGoalType;
import me.Plugins.SimpleFactions.War.enums.WarStatus;
import me.Plugins.SimpleFactions.War.enums.WarType;

class WarMapperTest {

	@Test
	void toData_v2FieldsWhenGoalSetOnWar() {
		Faction attacker = mock(Faction.class);
		Faction defender = mock(Faction.class);
		when(attacker.getId()).thenReturn("faction_a");
		when(defender.getId()).thenReturn("faction_b");

		Participant attackerParticipant = mock(Participant.class);
		when(attackerParticipant.getLeader()).thenReturn(attacker);
		when(attackerParticipant.getSubjects()).thenReturn(List.of());
		when(attackerParticipant.getAllies()).thenReturn(new HashMap<>());
		when(attackerParticipant.isCivilWar()).thenReturn(false);

		Participant defenderParticipant = mock(Participant.class);
		when(defenderParticipant.getLeader()).thenReturn(defender);
		when(defenderParticipant.getSubjects()).thenReturn(List.of());
		when(defenderParticipant.getAllies()).thenReturn(new HashMap<>());
		when(defenderParticipant.isCivilWar()).thenReturn(false);

		Side attackers = mock(Side.class);
		when(attackers.getLeader()).thenReturn(attacker);
		when(attackers.getMainParticipants()).thenReturn(List.of(attackerParticipant));

		Side defenders = mock(Side.class);
		when(defenders.getLeader()).thenReturn(defender);
		when(defenders.getMainParticipants()).thenReturn(List.of(defenderParticipant));

		War war = mock(War.class);
		when(war.getSchemaVersion()).thenReturn(2);
		when(war.getId()).thenReturn(7);
		when(war.getStatus()).thenReturn(WarStatus.ACTIVE);
		when(war.getGoal()).thenReturn(WarGoalType.SUBJUGATE);
		when(war.getWarType()).thenReturn(WarType.SUBJUGATE);
		when(war.getAttackerLeaderId()).thenReturn("faction_a");
		when(war.getDefenderLeaderId()).thenReturn("faction_b");
		when(war.getTargetTitleId()).thenReturn("county_x");
		when(war.getObjectiveProvinceId()).thenReturn(42);
		when(war.getCampaignStartProvinceId()).thenReturn(17);
		when(war.getCampaignProvinces()).thenReturn(List.of(17, 23, 42));
		when(war.getCursorIndex()).thenReturn(0);
		when(war.getInitiativeAttacker()).thenReturn(4);
		when(war.getInitiativeDefender()).thenReturn(3);
		when(war.getOccupiedByAttacker()).thenReturn(List.of(17));
		when(war.getOccupiedByDefender()).thenReturn(List.of());
		when(war.getLastBattleOccupied()).thenReturn(List.of(17));
		when(war.getCampaignPhase()).thenReturn(CampaignPhase.INVASION);
		when(war.getObjectiveHeldBy()).thenReturn(ObjectiveHolder.DEFENDER);
		when(war.isWhitePeaceProposedByAttacker()).thenReturn(false);
		when(war.isWhitePeaceProposedByDefender()).thenReturn(true);
		when(war.getCampaignBattlesFought()).thenReturn(2);
		when(war.getBattleSchedulePhase()).thenReturn(BattleSchedulePhase.SCHEDULED);
		when(war.getBattleDay()).thenReturn(LocalDate.parse("2026-08-21"));
		when(war.getScheduledBattleAt()).thenReturn(Instant.parse("2026-08-21T21:00:00Z"));
		when(war.getScheduledBattleHour()).thenReturn(21);
		when(war.getScheduledBattleProvinceId()).thenReturn(23);
		Map<UUID, Set<Integer>> votes = new HashMap<>();
		votes.put(UUID.fromString("00000000-0000-0000-0000-000000000001"), new HashSet<>(List.of(20, 21)));
		when(war.getBattleVotes()).thenReturn(votes);
		when(war.isAutoresolveProposedByAttacker()).thenReturn(true);
		when(war.isAutoresolveProposedByDefender()).thenReturn(false);
		when(war.getPostponementsThisCycle()).thenReturn(1);
		when(war.isDefenderChoiceResolved()).thenReturn(true);
		when(war.getSubjectFactionId()).thenReturn(null);
		when(war.getStartedAt()).thenReturn(Instant.parse("2026-08-19T12:00:00Z"));
		when(war.getEndedAt()).thenReturn(null);
		when(war.getEndReason()).thenReturn(null);
		when(war.getAttackers()).thenReturn(attackers);
		when(war.getDefenders()).thenReturn(defenders);

		WarData data = WarMapper.toData(war);

		assertEquals(2, data.schemaVersion);
		assertEquals(7, data.id);
		assertEquals("subjugate", data.goal);
		assertEquals("subjugate", data.warType);
		assertEquals(WarStatus.ACTIVE.toJson(), data.status);
		assertEquals("faction_a", data.attackerLeaderId);
		assertEquals("faction_b", data.defenderLeaderId);
		assertEquals("county_x", data.targetTitleId);
		assertEquals(Integer.valueOf(42), data.objectiveProvinceId);
		assertEquals(Integer.valueOf(17), data.campaignStartProvinceId);
		assertEquals(List.of(17, 23, 42), data.campaignProvinces);
		assertEquals(0, data.cursorIndex);
		assertEquals(Integer.valueOf(4), data.initiativeAttacker);
		assertEquals(Integer.valueOf(3), data.initiativeDefender);
		assertEquals(List.of(17), data.occupiedByAttacker);
		assertEquals(CampaignPhase.INVASION.toJson(), data.campaignPhase);
		assertEquals(ObjectiveHolder.DEFENDER.toJson(), data.objectiveHeldBy);
		assertTrue(data.whitePeaceProposedByDefender);
		assertEquals(Integer.valueOf(2), data.campaignBattlesFought);
		assertEquals("scheduled", data.battleSchedulePhase);
		assertEquals("2026-08-21", data.battleDay);
		assertEquals("2026-08-21T21:00:00Z", data.scheduledBattleAt);
		assertEquals(Integer.valueOf(21), data.scheduledBattleHour);
		assertEquals(Integer.valueOf(23), data.scheduledBattleProvinceId);
		assertEquals(List.of(20, 21), data.battleVotes.get("00000000-0000-0000-0000-000000000001"));
		assertTrue(data.autoresolveProposedByAttacker);
		assertEquals(Integer.valueOf(1), data.postponementsThisCycle);
		assertTrue(data.defenderChoiceResolved);
		assertEquals("2026-08-19T12:00:00Z", data.startedAt);
		assertNull(data.endReason);
		assertTrue(data.attackers.participants.get(0).warGoals.isEmpty());
	}

	@Test
	void fromData_legacyMissingScheduleFieldsUsesDefaults() {
		Faction attacker = mock(Faction.class);
		Faction defender = mock(Faction.class);
		when(attacker.getId()).thenReturn("faction_a");
		when(defender.getId()).thenReturn("faction_b");
		FactionManager.factions.add(attacker);
		FactionManager.factions.add(defender);
		try {
			WarData data = new WarData();
			data.id = 9;
			data.status = "active";
			data.attackers = new me.Plugins.SimpleFactions.Database.SideData();
			data.attackers.leader = "faction_a";
			me.Plugins.SimpleFactions.Database.ParticipantData atk = new me.Plugins.SimpleFactions.Database.ParticipantData();
			atk.leader = "faction_a";
			data.attackers.participants.add(atk);
			data.defenders = new me.Plugins.SimpleFactions.Database.SideData();
			data.defenders.leader = "faction_b";
			me.Plugins.SimpleFactions.Database.ParticipantData def = new me.Plugins.SimpleFactions.Database.ParticipantData();
			def.leader = "faction_b";
			data.defenders.participants.add(def);

			War war = WarMapper.fromData(data);

			assertEquals(BattleSchedulePhase.IDLE, war.getBattleSchedulePhase());
			assertNull(war.getBattleDay());
			assertNull(war.getScheduledBattleAt());
			assertEquals(0, war.getScheduledBattleHour());
			assertTrue(war.getBattleVotes().isEmpty());
			assertEquals(0, war.getPostponementsThisCycle());
		} finally {
			FactionManager.factions.remove(attacker);
			FactionManager.factions.remove(defender);
		}
	}
}
