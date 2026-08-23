package me.Plugins.SimpleFactions.War.pathfinder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BelligerentTerritoryTest {
	private static final String ATK = "atk";
	private static final String DEF = "def";
	private static final String FOREIGN = "foreign";

	private Map<Integer, String> ownerByProvince;
	private BelligerentTerritory territory;

	@BeforeEach
	void setUp() {
		ownerByProvince = new HashMap<>();
		ProvinceOwnerLookup owners = ownerByProvince::get;
		territory = new BelligerentTerritory(Set.of(ATK), Set.of(DEF), owners);
	}

	@Test
	void wildernessHasNoOwnerAndIsNeutral() {
		int wildernessId = 100;
		assertTrue(territory.isWilderness(wildernessId));
		assertFalse(territory.isForeignNation(wildernessId));
		assertTrue(territory.isNeutral(wildernessId));
	}

	@Test
	void foreignNationIsNeutralButNotWilderness() {
		ownerByProvince.put(200, FOREIGN);
		assertFalse(territory.isWilderness(200));
		assertTrue(territory.isForeignNation(200));
		assertTrue(territory.isNeutral(200));
	}

	@Test
	void belligerentIsNeitherWildernessNorForeignNorNeutral() {
		ownerByProvince.put(300, ATK);
		ownerByProvince.put(400, DEF);
		assertFalse(territory.isWilderness(300));
		assertFalse(territory.isForeignNation(300));
		assertFalse(territory.isNeutral(300));
		assertFalse(territory.isNeutral(400));
	}
}
