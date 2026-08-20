package me.Plugins.SimpleFactions.War.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class WarGoalTypeTest {

	@Test
	void fromJson_mapsV2Goals() {
		assertEquals(WarGoalType.DE_JURE_ANNEX, WarGoalType.fromJson("de_jure_annex"));
		assertEquals(WarGoalType.SUBJUGATE, WarGoalType.fromJson("subjugate"));
		assertEquals(WarGoalType.TRANSFER_SUBJECT, WarGoalType.fromJson("transfer_subject"));
	}

	@Test
	void toJson_roundTrips() {
		for (WarGoalType type : WarGoalType.values()) {
			assertEquals(type, WarGoalType.fromJson(type.toJson()));
		}
	}

	@Test
	void fromJson_nullSafe() {
		assertNull(WarGoalType.fromJson(null));
		assertNull(WarGoalType.fromJson(""));
		assertNull(WarGoalType.fromJson("unknown"));
	}

	@Test
	void getDisplayName_returnsPlayerFacingLabels() {
		assertEquals("De Jure Annex", WarGoalType.DE_JURE_ANNEX.getDisplayName());
		assertEquals("Subjugate", WarGoalType.SUBJUGATE.getDisplayName());
		assertEquals("Transfer Subject", WarGoalType.TRANSFER_SUBJECT.getDisplayName());
	}
}
