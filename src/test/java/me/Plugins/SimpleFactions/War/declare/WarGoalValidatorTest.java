package me.Plugins.SimpleFactions.War.declare;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import me.Plugins.SimpleFactions.Diplomacy.Relation;
import me.Plugins.SimpleFactions.Diplomacy.RelationType;
import me.Plugins.SimpleFactions.Loaders.TitleLoader;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.TitleManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Tiers.Tier;
import me.Plugins.SimpleFactions.Tiers.Title;
import me.Plugins.SimpleFactions.War.enums.WarGoalType;
import me.Plugins.SimpleFactions.War.declare.WarGoalValidator.SettlementProbe;

class WarGoalValidatorTest {
	private final List<Faction> savedFactions = new ArrayList<>();
	private WarGoalValidator validator;

	@BeforeEach
	void setUp() {
		validator = new WarGoalValidator();
		savedFactions.addAll(FactionManager.factions);
		FactionManager.factions.clear();
	}

	@AfterEach
	void tearDown() {
		FactionManager.factions.clear();
		FactionManager.factions.addAll(savedFactions);
	}

	@Test
	void canAnnexByRank_countyCannotTargetKingdomTitle() {
		assertFalse(WarGoalValidator.canAnnexByRank(2, 4));
		assertTrue(WarGoalValidator.canAnnexByRank(4, 2));
		assertTrue(WarGoalValidator.canAnnexByRank(4, 4));
	}

	@Test
	void titleProvincesContainSettlement_blockedWhenSettlementInRegion() {
		Set<Integer> titleProvinces = Set.of(10, 11, 12);
		List<SettlementProbe> settlements = List.of(new SettlementProbe(11));
		List<Integer> capitals = List.of();

		assertTrue(WarGoalValidator.titleProvincesContainSettlement(titleProvinces, settlements, capitals));
	}

	@Test
	void titleProvincesContainSettlement_blockedWhenCapitalInRegion() {
		Set<Integer> titleProvinces = Set.of(10, 11, 12);
		List<SettlementProbe> settlements = List.of();
		List<Integer> capitals = List.of(12);

		assertTrue(WarGoalValidator.titleProvincesContainSettlement(titleProvinces, settlements, capitals));
	}

	@Test
	void deJureAnnex_allowedWhenNoSettlementsAndPartialControl() {
		Faction attacker = mockFaction("attacker", 4);
		Faction defender = mockFaction("defender", 2);
		Title title = mock(Title.class);
		Tier titleTier = mock(Tier.class);
		when(title.getTier()).thenReturn(titleTier);
		when(titleTier.getTier()).thenReturn(2);
		when(title.canBeHeld(attacker)).thenReturn(false);
		when(title.nestedProvinceCheck(any(), any())).thenReturn(1);

		try (MockedStatic<TitleLoader> titleLoader = mockStatic(TitleLoader.class);
				MockedStatic<TitleManager> titleManager = mockStatic(TitleManager.class)) {
			titleLoader.when(() -> TitleLoader.getById("county_x")).thenReturn(title);
			titleManager.when(() -> TitleManager.getOwner(title)).thenReturn(defender);
			titleManager.when(() -> TitleManager.getProvinces(title)).thenReturn(List.of(10, 11));
			titleManager.when(() -> TitleManager.getProvinces(attacker)).thenReturn(List.of(10));

			WarDeclareRequest request = new WarDeclareRequest(
					attacker, defender, WarGoalType.DE_JURE_ANNEX, "county_x", null);

			assertTrue(validator.validate(request).isValid());
		}
	}

	@Test
	void deJureAnnex_rejectsWhenSettlementPresent() {
		Faction attacker = mockFaction("attacker", 4);
		Faction defender = mockFaction("defender", 2);
		Faction settler = mockFactionWithSettlement("settler", 11);
		FactionManager.factions.add(settler);

		Title title = mock(Title.class);
		Tier titleTier = mock(Tier.class);
		when(title.getTier()).thenReturn(titleTier);
		when(titleTier.getTier()).thenReturn(2);
		when(title.canBeHeld(attacker)).thenReturn(false);
		when(title.nestedProvinceCheck(any(), any())).thenReturn(1);

		try (MockedStatic<TitleLoader> titleLoader = mockStatic(TitleLoader.class);
				MockedStatic<TitleManager> titleManager = mockStatic(TitleManager.class)) {
			titleLoader.when(() -> TitleLoader.getById("county_x")).thenReturn(title);
			titleManager.when(() -> TitleManager.getOwner(title)).thenReturn(defender);
			titleManager.when(() -> TitleManager.getProvinces(title)).thenReturn(List.of(10, 11));
			titleManager.when(() -> TitleManager.getProvinces(attacker)).thenReturn(List.of(10));

			WarDeclareRequest request = new WarDeclareRequest(
					attacker, defender, WarGoalType.DE_JURE_ANNEX, "county_x", null);

			WarValidationResult result = validator.validate(request);
			assertFalse(result.isValid());
			assertTrue(result.getMessage().contains("settlements"));
		}
	}

	@Test
	void subjugate_allowedWhenSettlementPresent() {
		Faction attacker = mockFaction("attacker", 3);
		Faction defender = mockFactionWithCapital("defender", 42);
		FactionManager.factions.add(defender);

		WarDeclareRequest request = WarDeclareRequest.of(attacker, defender, WarGoalType.SUBJUGATE);
		assertTrue(validator.validate(request).isValid());
	}

	@Test
	void subjugate_rejectsAlreadySubject() {
		Faction attacker = mockFaction("attacker", 3);
		Faction defender = mockSubject("defender", "attacker");
		FactionManager.factions.add(defender);

		WarValidationResult result = validator.validate(WarDeclareRequest.of(attacker, defender, WarGoalType.SUBJUGATE));
		assertFalse(result.isValid());
		assertTrue(result.getMessage().contains("already your subject"));
	}

	@Test
	void transferSubject_requiresDefenderAsOverlord() {
		Faction attacker = mockFaction("attacker", 4);
		Faction defender = mockFaction("defender", 4);
		Faction subject = mockSubject("subject", "other_overlord");
		FactionManager.factions.add(subject);

		WarDeclareRequest request = new WarDeclareRequest(
				attacker, defender, WarGoalType.TRANSFER_SUBJECT, null, "subject");

		WarValidationResult result = validator.validate(request);
		assertFalse(result.isValid());
		assertTrue(result.getMessage().contains("not a subject of the defender"));
	}

	@Test
	void transferSubject_allowedWhenDefenderIsOverlord() {
		Faction attacker = mockFaction("attacker", 4);
		Faction defender = mockFaction("defender", 4);
		Faction subject = mockSubject("subject", "defender");
		FactionManager.factions.add(subject);

		WarDeclareRequest request = new WarDeclareRequest(
				attacker, defender, WarGoalType.TRANSFER_SUBJECT, null, "subject");

		assertTrue(validator.validate(request).isValid());
	}

	private static Faction mockFaction(String id, int tierLevel) {
		Faction faction = mock(Faction.class);
		Tier tier = mock(Tier.class);
		when(faction.getId()).thenReturn(id);
		when(faction.getTier()).thenReturn(tier);
		when(tier.getTier()).thenReturn(tierLevel);
		when(faction.getRelations()).thenReturn(new HashMap<>());
		when(faction.getCapital()).thenReturn(0);
		when(faction.getSettlementHandler()).thenReturn(mock(me.Plugins.SimpleFactions.settlement.handler.SettlementHandler.class));
		when(faction.getSettlementHandler().getAll()).thenReturn(List.of());
		return faction;
	}

	private static Faction mockFactionWithCapital(String id, int capitalProvince) {
		Faction faction = mockFaction(id, 2);
		when(faction.getCapital()).thenReturn(capitalProvince);
		return faction;
	}

	private static Faction mockFactionWithSettlement(String id, int settlementProvince) {
		Faction faction = mockFaction(id, 2);
		me.Plugins.SimpleFactions.settlement.Settlement settlement =
				mock(me.Plugins.SimpleFactions.settlement.Settlement.class);
		when(settlement.getCenterProvince()).thenReturn(settlementProvince);
		when(faction.getSettlementHandler().getAll()).thenReturn(List.of(settlement));
		return faction;
	}

	private static Faction mockSubject(String id, String overlordId) {
		Faction faction = mockFaction(id, 2);
		Relation relation = mock(Relation.class);
		RelationType type = mock(RelationType.class);
		when(type.isOverlord()).thenReturn(true);
		when(relation.getType()).thenReturn(type);
		HashMap<String, Relation> relations = new HashMap<>();
		relations.put(overlordId, relation);
		when(faction.getRelations()).thenReturn(relations);
		return faction;
	}
}
