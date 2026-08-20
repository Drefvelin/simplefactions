package me.Plugins.SimpleFactions.War.battle.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import me.Plugins.SimpleFactions.War.battle.enums.BattleType;

class CapturePointSequenceTest {
	@Test
	void letterForIndex_generatesSequence() {
		assertEquals("A", BattleCapturePoints.letterForIndex(0));
		assertEquals("B", BattleCapturePoints.letterForIndex(1));
		assertEquals("Z", BattleCapturePoints.letterForIndex(25));
		assertEquals("AA", BattleCapturePoints.letterForIndex(26));
	}

	@Test
	void autoIds_incrementPerSide() {
		BossBar bossBar = mock(BossBar.class);
		try (MockedStatic<org.bukkit.Bukkit> bukkit = Mockito.mockStatic(org.bukkit.Bukkit.class)) {
			bukkit.when(() -> org.bukkit.Bukkit.createBossBar(
					Mockito.anyString(), Mockito.any(BarColor.class), Mockito.any(BarStyle.class)))
					.thenReturn(bossBar);
			bukkit.when(() -> org.bukkit.Bukkit.createBossBar(
					Mockito.anyString(), Mockito.any(BarColor.class), Mockito.any(BarStyle.class), Mockito.any()))
					.thenReturn(bossBar);

			Battle battle = BattleFactory.createBlank(BattleType.FIELD, "test");
			BattleSide attacker = battle.getSideById("attacker");
			Location loc = new Location(mock(World.class), 0, 64, 0);

			CapturePoint a = BattleCapturePoints.createAtPlayer(battle, "attacker", loc, attacker);
			battle.addPoint(a);
			CapturePoint b = BattleCapturePoints.createAtPlayer(battle, "attacker", loc, attacker);

			assertEquals("A", a.getId());
			assertEquals("B", b.getId());
			assertEquals(0, a.getSequenceIndex());
			assertEquals(1, b.getSequenceIndex());
		}
	}

	@Test
	void sequentialFront_blocksDeepCapture() {
		BossBar bossBar = mock(BossBar.class);
		try (MockedStatic<org.bukkit.Bukkit> bukkit = Mockito.mockStatic(org.bukkit.Bukkit.class)) {
			bukkit.when(() -> org.bukkit.Bukkit.createBossBar(
					Mockito.anyString(), Mockito.any(BarColor.class), Mockito.any(BarStyle.class)))
					.thenReturn(bossBar);
			bukkit.when(() -> org.bukkit.Bukkit.createBossBar(
					Mockito.anyString(), Mockito.any(BarColor.class), Mockito.any(BarStyle.class), Mockito.any()))
					.thenReturn(bossBar);

			Battle battle = BattleFactory.createBlank(BattleType.FIELD, "test");
			BattleSide attacker = battle.getSideById("attacker");
			BattleSide defender = battle.getSideById("defender");
			Location loc = new Location(mock(World.class), 0, 64, 0);

			CapturePoint front = new CapturePoint("A", loc, defender, 50);
			front.setAdvanceSideId("attacker");
			front.setSequenceIndex(0);

			CapturePoint deep = new CapturePoint("B", loc, defender, 50);
			deep.setAdvanceSideId("attacker");
			deep.setSequenceIndex(1);

			List<CapturePoint> points = new ArrayList<>();
			points.add(front);
			points.add(deep);

			assertTrue(CapturePoint.isFrontPoint(front, points));
			assertFalse(CapturePoint.isFrontPoint(deep, points));
		}
	}
}
