package me.Plugins.SimpleFactions.War.battle.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import me.Plugins.SimpleFactions.War.battle.enums.BattleType;
import me.Plugins.SimpleFactions.War.battle.warband.Warband;
import me.Plugins.SimpleFactions.War.battle.warband.WarbandManager;

class BattleJoinServiceTest {
	@BeforeEach
	void setUp() {
		BattleManager.resetForTests();
		WarbandManager.resetForTests();
	}

	@Test
	void join_success() {
		BossBar bossBar = mock(BossBar.class);
		try (MockedStatic<org.bukkit.Bukkit> bukkit = Mockito.mockStatic(org.bukkit.Bukkit.class)) {
			bukkit.when(() -> org.bukkit.Bukkit.createBossBar(
					Mockito.anyString(), Mockito.any(BarColor.class), Mockito.any(BarStyle.class)))
					.thenReturn(bossBar);
			bukkit.when(() -> org.bukkit.Bukkit.createBossBar(
					Mockito.anyString(), Mockito.any(BarColor.class), Mockito.any(BarStyle.class), Mockito.any()))
					.thenReturn(bossBar);

			Battle battle = BattleFactory.createBlank(BattleType.FIELD, "test");
			battle.setLocked(false);
			Warband warband = Warband.createWithMemberIds("alpha", java.util.UUID.randomUUID(), false);

			assertNull(BattleJoinService.join(warband, battle, "attacker"));
			assertEquals(1, battle.getSideById("attacker").getBands().size());
		}
	}

	@Test
	void join_failsWhenLocked() {
		BossBar bossBar = mock(BossBar.class);
		try (MockedStatic<org.bukkit.Bukkit> bukkit = Mockito.mockStatic(org.bukkit.Bukkit.class)) {
			bukkit.when(() -> org.bukkit.Bukkit.createBossBar(
					Mockito.anyString(), Mockito.any(BarColor.class), Mockito.any(BarStyle.class)))
					.thenReturn(bossBar);
			bukkit.when(() -> org.bukkit.Bukkit.createBossBar(
					Mockito.anyString(), Mockito.any(BarColor.class), Mockito.any(BarStyle.class), Mockito.any()))
					.thenReturn(bossBar);

			Battle battle = BattleFactory.createBlank(BattleType.FIELD, "test");
			battle.setLocked(true);
			Warband warband = Warband.createWithMemberIds("alpha", java.util.UUID.randomUUID(), false);

			assertEquals("Battle is locked", BattleJoinService.join(warband, battle, "attacker"));
		}
	}

	@Test
	void join_failsWithoutWarband() {
		BossBar bossBar = mock(BossBar.class);
		try (MockedStatic<org.bukkit.Bukkit> bukkit = Mockito.mockStatic(org.bukkit.Bukkit.class)) {
			bukkit.when(() -> org.bukkit.Bukkit.createBossBar(
					Mockito.anyString(), Mockito.any(BarColor.class), Mockito.any(BarStyle.class)))
					.thenReturn(bossBar);
			bukkit.when(() -> org.bukkit.Bukkit.createBossBar(
					Mockito.anyString(), Mockito.any(BarColor.class), Mockito.any(BarStyle.class), Mockito.any()))
					.thenReturn(bossBar);

			Battle battle = BattleFactory.createBlank(BattleType.FIELD, "test");
			battle.setLocked(false);

			assertEquals("You need to lead a warband to join a battle", BattleJoinService.join((Warband) null, battle, "attacker"));
		}
	}
}
