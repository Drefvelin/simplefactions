package me.Plugins.SimpleFactions.War.battle.engine;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.UUID;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import me.Plugins.SimpleFactions.War.battle.enums.BattleType;

class BattleLeavePenaltyServiceTest {
	@BeforeEach
	void setUp() {
		BattleLeavePenaltyService.resetForTests();
	}

	@Test
	void shouldStartCountdown_whenLeavingAllowedToDisallowed() {
		Battle battle = startedFieldBattle();
		battle.setAllowedProvinceIds(Set.of(10, 11));

		assertTrue(BattleLeavePenaltyService.shouldStartCountdown(battle, 10, 99));
	}

	@Test
	void shouldNotStart_whenMovingBetweenAllowedProvinces() {
		Battle battle = startedFieldBattle();
		battle.setAllowedProvinceIds(Set.of(10, 11));

		assertFalse(BattleLeavePenaltyService.shouldStartCountdown(battle, 10, 11));
	}

	@Test
	void shouldNotStart_forRaid() {
		Battle battle = startedFieldBattle();
		battle.setBattleType(BattleType.RAID);
		battle.setAllowedProvinceIds(Set.of(10));

		assertFalse(BattleLeavePenaltyService.shouldStartCountdown(battle, 10, 99));
	}

	@Test
	void shouldNotStart_whenBattleNotStarted() {
		Battle battle = startedFieldBattle();
		setStarted(battle, false);
		battle.setAllowedProvinceIds(Set.of(10));

		assertFalse(BattleLeavePenaltyService.shouldStartCountdown(battle, 10, 99));
	}

	@Test
	void penaltyRespawnFlag_tracksAndClears() {
		UUID playerId = UUID.randomUUID();
		assertFalse(BattleLeavePenaltyService.isPenaltyRespawn(playerId));

		BattleLeavePenaltyService.clearPenaltyRespawn(playerId);
		assertFalse(BattleLeavePenaltyService.isPenaltyRespawn(playerId));
	}

	private Battle startedFieldBattle() {
		BossBar bossBar = mock(BossBar.class);
		try (MockedStatic<org.bukkit.Bukkit> bukkit = Mockito.mockStatic(org.bukkit.Bukkit.class)) {
			bukkit.when(() -> org.bukkit.Bukkit.createBossBar(
					Mockito.anyString(), Mockito.any(BarColor.class), Mockito.any(BarStyle.class)))
					.thenReturn(bossBar);
			bukkit.when(() -> org.bukkit.Bukkit.createBossBar(
					Mockito.anyString(), Mockito.any(BarColor.class), Mockito.any(BarStyle.class), Mockito.any()))
					.thenReturn(bossBar);
			Battle battle = BattleFactory.createBlank(BattleType.FIELD, "test");
			setStarted(battle, true);
			return battle;
		}
	}

	private void setStarted(Battle battle, boolean started) {
		try {
			java.lang.reflect.Field field = Battle.class.getDeclaredField("started");
			field.setAccessible(true);
			field.setBoolean(battle, started);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}
}
