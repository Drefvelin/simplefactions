package me.Plugins.SimpleFactions.Utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FormatterTest {

	@Test
	void formatMoneyAlwaysShowsTwoDecimals() {
		assertEquals("6.10", Formatter.formatMoney(6.1));
		assertEquals("12.35", Formatter.formatMoney(12.345));
		assertEquals("0.00", Formatter.formatMoney(0.0));
	}
}
