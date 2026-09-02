package me.Plugins.SimpleFactions.Utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DisplayNameGateTest {

	@Test
	void flagsCamelCaseWithoutSpaces() {
		assertTrue(DisplayNameGate.looksLikeMissingSpaces("GreenWrathTribe"));
	}

	@Test
	void allowsUnderscoresSpacesAndSingleWords() {
		assertFalse(DisplayNameGate.looksLikeMissingSpaces("Green_Wrath_Tribe"));
		assertFalse(DisplayNameGate.looksLikeMissingSpaces("Green Wrath Tribe"));
		assertFalse(DisplayNameGate.looksLikeMissingSpaces("Green"));
		assertFalse(DisplayNameGate.looksLikeMissingSpaces("USA"));
	}

	@Test
	void suggestUnderscoresInsertsAtCamelBoundaries() {
		assertEquals("Green_Wrath_Tribe", DisplayNameGate.suggestUnderscores("GreenWrathTribe"));
	}
}
