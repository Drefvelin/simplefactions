package me.Plugins.SimpleFactions.War;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import me.Plugins.SimpleFactions.Army.Military;
import me.Plugins.SimpleFactions.Army.Regiment;
import me.Plugins.SimpleFactions.Managers.WarManager;
import me.Plugins.SimpleFactions.Objects.Faction;

class WarCommitmentTest {
	private static final Gson GSON = new Gson();
	private List<War> savedWars;

	@BeforeEach
	void setUp() {
		savedWars = new ArrayList<>(WarManager.get());
		WarManager.get().clear();
	}

	@AfterEach
	void tearDown() {
		for (War war : new ArrayList<>(WarManager.get())) {
			WarManager.endWar(war);
		}
		WarManager.get().clear();
		WarManager.get().addAll(savedWars);
	}

	@Test
	void commitment_preservesWarIdAndFields() {
		Instant committedAt = Instant.parse("2026-08-19T12:00:00Z");
		WarCommitment commitment = new WarCommitment(3, "faction_a", "levy", 0, committedAt);

		assertEquals(3, commitment.warId());
		assertEquals("faction_a", commitment.factionId());
		assertEquals("levy", commitment.regimentId());
		assertEquals(0, commitment.count());
		assertEquals(committedAt, commitment.committedAt());
	}

	@Test
	void commitment_jsonShape_preservesWarId() {
		String json = "{\"warId\":3,\"factionId\":\"faction_a\",\"regimentId\":\"levy\",\"count\":0,\"committedAt\":\"2026-08-19T12:00:00Z\"}";
		CommitmentJson restored = GSON.fromJson(json, CommitmentJson.class);

		assertNotNull(restored);
		assertEquals(3, restored.warId);
		assertEquals("faction_a", restored.factionId);
		assertEquals("levy", restored.regimentId);
		assertEquals(0, restored.count);
		assertEquals("2026-08-19T12:00:00Z", restored.committedAt);
	}

	private static final class CommitmentJson {
		int warId;
		String factionId;
		String regimentId;
		int count;
		String committedAt;
	}

	@Test
	void getWarId_matchesGetId() {
		Faction attacker = mock(Faction.class);
		Faction defender = mock(Faction.class);
		when(attacker.getId()).thenReturn("attacker");
		when(defender.getId()).thenReturn("defender");
		when(attacker.getName()).thenReturn("Attacker");
		when(defender.getName()).thenReturn("Defender");

		War war = new War(7, attacker, defender);
		assertEquals(war.getId(), war.getWarId());
	}

	@Test
	void commitForWar_createsZeroCountStubPerRegiment() {
		Faction attacker = mock(Faction.class);
		Faction defender = mock(Faction.class);
		when(attacker.getId()).thenReturn("attacker");
		when(defender.getId()).thenReturn("defender");
		when(attacker.getName()).thenReturn("Attacker");
		when(defender.getName()).thenReturn("Defender");

		War war = new War(5, attacker, defender);
		WarManager.get().add(war);

		Faction faction = mock(Faction.class);
		when(faction.getId()).thenReturn("attacker");
		Military military = mock(Military.class);
		Regiment levy = mock(Regiment.class);
		Regiment infantry = mock(Regiment.class);
		when(levy.getId()).thenReturn("levy");
		when(infantry.getId()).thenReturn("infantry");
		when(military.getRegiments()).thenReturn(List.of(levy, infantry));
		when(faction.getMilitary()).thenReturn(military);

		List<WarCommitment> commitments = WarManager.commitForWar(5, faction);

		assertEquals(2, commitments.size());
		for (WarCommitment commitment : commitments) {
			assertEquals(5, commitment.warId());
			assertEquals("attacker", commitment.factionId());
			assertEquals(0, commitment.count());
			assertNotNull(commitment.committedAt());
		}

		List<WarCommitment> stored = WarManager.getCommitmentsForWar(5);
		assertEquals(2, stored.size());
		assertEquals(commitments, stored);
	}

	@Test
	void commitForWar_returnsExistingFactionCommitments() {
		Faction attacker = mock(Faction.class);
		Faction defender = mock(Faction.class);
		when(attacker.getId()).thenReturn("attacker");
		when(defender.getId()).thenReturn("defender");
		when(attacker.getName()).thenReturn("Attacker");
		when(defender.getName()).thenReturn("Defender");

		War war = new War(9, attacker, defender);
		WarManager.get().add(war);

		Faction faction = mock(Faction.class);
		when(faction.getId()).thenReturn("attacker");
		Military military = mock(Military.class);
		Regiment levy = mock(Regiment.class);
		when(levy.getId()).thenReturn("levy");
		when(military.getRegiments()).thenReturn(List.of(levy));
		when(faction.getMilitary()).thenReturn(military);

		List<WarCommitment> first = WarManager.commitForWar(9, faction);
		List<WarCommitment> second = WarManager.commitForWar(9, faction);

		assertEquals(first, second);
		assertEquals(1, WarManager.getCommitmentsForWar(9).size());
	}

	@Test
	void endWar_clearsCommitments() {
		Faction attacker = mock(Faction.class);
		Faction defender = mock(Faction.class);
		when(attacker.getId()).thenReturn("attacker");
		when(defender.getId()).thenReturn("defender");
		when(attacker.getName()).thenReturn("Attacker");
		when(defender.getName()).thenReturn("Defender");

		War war = new War(11, attacker, defender);
		WarManager.get().add(war);

		Faction faction = mock(Faction.class);
		when(faction.getId()).thenReturn("attacker");
		Military military = mock(Military.class);
		Regiment levy = mock(Regiment.class);
		when(levy.getId()).thenReturn("levy");
		when(military.getRegiments()).thenReturn(List.of(levy));
		when(faction.getMilitary()).thenReturn(military);

		WarManager.commitForWar(11, faction);
		assertEquals(1, WarManager.getCommitmentsForWar(11).size());

		WarManager.endWar(war);
		assertTrue(WarManager.getCommitmentsForWar(11).isEmpty());
	}
}
