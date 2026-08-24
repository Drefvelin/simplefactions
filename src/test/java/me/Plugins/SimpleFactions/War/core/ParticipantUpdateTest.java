package me.Plugins.SimpleFactions.War.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import me.Plugins.SimpleFactions.Managers.RelationManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.core.Participant;
import me.Plugins.SimpleFactions.War.core.War;

class ParticipantUpdateTest {

	@Test
	void update_removesSubjectWithChangedOverlord() {
		Faction leader = mock(Faction.class);
		when(leader.getId()).thenReturn("leader");
		Faction staleSubject = mock(Faction.class);

		Participant participant = new Participant(
				leader,
				List.of(staleSubject),
				new HashMap<>(),
				new HashMap<>(),
				false);
		War war = mock(War.class);
		when(war.isMainParticipant(staleSubject)).thenReturn(false);

		try (MockedStatic<RelationManager> relations = mockStatic(RelationManager.class)) {
			relations.when(() -> RelationManager.getOverlord(staleSubject)).thenReturn("other_lord");
			relations.when(() -> RelationManager.getSubjects(leader)).thenReturn(List.of());

			participant.update(war);
		}

		assertFalse(participant.getSubjects().contains(staleSubject));
	}

	@Test
	void update_addsNewSubjectFromRelations() {
		Faction leader = mock(Faction.class);
		when(leader.getId()).thenReturn("leader");
		when(leader.getRelations()).thenReturn(new HashMap<>());

		Faction newSubject = mock(Faction.class);
		Participant participant = new Participant(
				leader,
				new ArrayList<>(),
				new HashMap<>(),
				new HashMap<>(),
				false);
		War war = mock(War.class);
		when(war.isMainParticipant(newSubject)).thenReturn(false);

		try (MockedStatic<RelationManager> relations = mockStatic(RelationManager.class)) {
			relations.when(() -> RelationManager.getSubjects(leader)).thenReturn(List.of(newSubject));

			participant.update(war);
		}

		assertTrue(participant.getSubjects().contains(newSubject));
	}
}
