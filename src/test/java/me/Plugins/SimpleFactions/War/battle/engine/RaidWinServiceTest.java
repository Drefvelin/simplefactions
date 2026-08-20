package me.Plugins.SimpleFactions.War.battle.engine;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import me.Plugins.SimpleFactions.War.battle.enums.BattleType;
import me.Plugins.SimpleFactions.War.battle.enums.DefenderRespawnMode;
import me.Plugins.SimpleFactions.War.battle.template.BattleLocation;
import me.Plugins.SimpleFactions.War.battle.template.BattleTemplate;
import me.Plugins.SimpleFactions.War.battle.template.CapturePointDefinition;

class RaidWinServiceTest {
	@BeforeEach
	void setUp() {
		RaidAttackerEliminationService.resetForTests();
	}

	@Test
	void targetCaptured_detectsAttackerControl() {
		withMockWorldAndBossBar(() -> {
			Battle battle = startedRaid("test_raid");
			CapturePoint target = battle.getPointManager().getPoints().get(0);
			BattleSide attacker = battle.getSideById(BattleTemplate.ATTACKER_SIDE);
			setController(target, attacker, 100);
			assertTrue(RaidWinService.isTargetCaptured(battle));
		});
	}

	@Test
	void defenderEliminated_whenLivesModeAndNoLives() {
		withMockWorldAndBossBar(() -> {
			Battle battle = startedRaid("test_raid");
			battle.setDefenderRespawnMode(DefenderRespawnMode.LIVES);
			BattleSide defender = battle.getSideById(BattleTemplate.DEFENDER_SIDE);
			defender.setLives(0);
			assertTrue(FieldWinService.isSideEliminated(defender));
		});
	}

	@Test
	void defenderEliminationOnlyAppliesInLivesMode() {
		withMockWorldAndBossBar(() -> {
			Battle battle = startedRaid("test_raid");
			battle.setDefenderRespawnMode(DefenderRespawnMode.INFINITE);
			BattleSide defender = battle.getSideById(BattleTemplate.DEFENDER_SIDE);
			defender.setLives(0);
			boolean defenderEliminated = BattleRaidSetup.getEffectiveDefenderRespawnMode(battle) == DefenderRespawnMode.LIVES
					&& FieldWinService.isSideEliminated(defender);
			assertFalse(defenderEliminated);
		});
	}

	private Battle startedRaid(String id) {
		Battle battle = BattleFactory.createBlank(BattleType.RAID, id);
		battle.setRaidTarget(new CapturePointDefinition("target", new BattleLocation("world", 10, 64, 10, 0, 0)));
		battle.start();
		return battle;
	}

	private void setController(CapturePoint point, BattleSide side, int progress) {
		try {
			java.lang.reflect.Field controllerField = CapturePoint.class.getDeclaredField("controller");
			controllerField.setAccessible(true);
			controllerField.set(point, side);
			java.lang.reflect.Field progressField = CapturePoint.class.getDeclaredField("captureProgress");
			progressField.setAccessible(true);
			progressField.setInt(point, progress);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	private void withMockWorldAndBossBar(Runnable action) {
		World world = mock(World.class);
		when(world.getName()).thenReturn("world");
		BossBar bossBar = mock(BossBar.class);
		try (MockedStatic<org.bukkit.Bukkit> bukkit = Mockito.mockStatic(org.bukkit.Bukkit.class)) {
			bukkit.when(() -> org.bukkit.Bukkit.getWorld("world")).thenReturn(world);
			bukkit.when(() -> org.bukkit.Bukkit.createBossBar(
					Mockito.anyString(), Mockito.any(BarColor.class), Mockito.any(BarStyle.class)))
					.thenReturn(bossBar);
			bukkit.when(() -> org.bukkit.Bukkit.createBossBar(
					Mockito.anyString(), Mockito.any(BarColor.class), Mockito.any(BarStyle.class), Mockito.any()))
					.thenReturn(bossBar);
			action.run();
		}
	}
}
