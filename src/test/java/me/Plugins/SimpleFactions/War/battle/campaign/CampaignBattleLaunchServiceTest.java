package me.Plugins.SimpleFactions.War.battle.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import me.Plugins.SimpleFactions.Army.Military;
import me.Plugins.SimpleFactions.Army.Regiment;
import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.battle.engine.Battle;
import me.Plugins.SimpleFactions.War.battle.engine.BattleManager;
import me.Plugins.SimpleFactions.War.battle.enums.BattleType;
import me.Plugins.SimpleFactions.War.battle.warband.WarbandManager;
import me.Plugins.SimpleFactions.War.enums.BattleSchedulePhase;
import me.Plugins.SimpleFactions.War.enums.CampaignPhase;
import me.Plugins.SimpleFactions.War.enums.WarGoalType;
import me.Plugins.SimpleFactions.War.enums.WarType;

class CampaignBattleLaunchServiceTest {
	private Faction attacker;
	private Faction defender;

	@BeforeEach
	void setUp() {
		BattleManager.resetForTests();
		WarbandManager.resetForTests();
		Cache.warFirstBattleAtBorder = true;
		Cache.battleCampaignTemplateField = "";

		attacker = mock(Faction.class);
		defender = mock(Faction.class);
		when(attacker.getId()).thenReturn("atk");
		when(defender.getId()).thenReturn("def");
		when(attacker.getLeader()).thenReturn("Alice");
		when(defender.getLeader()).thenReturn("Bob");
		mockMilitary(attacker);
		mockMilitary(defender);
	}

	private void mockMilitary(Faction faction) {
		Military military = mock(Military.class);
		Regiment levy = mock(Regiment.class);
		when(military.getManpowerNoLevy(anyBoolean())).thenReturn(10);
		when(military.getRegiment("levy")).thenReturn(levy);
		when(levy.getEntries()).thenReturn(java.util.List.of());
		when(faction.getMilitary()).thenReturn(military);
		when(faction.getMembers()).thenReturn(java.util.List.of());
		when(faction.getName()).thenReturn("faction");
	}

	@Test
	void prepareScheduledBattle_setsWarIdProvinceAndType() {
		War war = scheduledWar();

		withMockBossBar(() -> {
			Battle battle = CampaignBattleLaunchService.prepareScheduledBattle(war);

			assertNotNull(battle);
			assertEquals("campaign_w1_p20", battle.getId());
			assertEquals(Integer.valueOf(1), battle.getWarId());
			assertEquals(Integer.valueOf(20), battle.getProvinceId());
			assertEquals(BattleType.FIELD, battle.getBattleType());
			assertFalse(battle.isLocked());
			assertTrue(battle.hasTeleport());
		});
	}

	@Test
	void prepareScheduledBattle_isIdempotent() {
		War war = scheduledWar();

		withMockBossBar(() -> {
			Battle first = CampaignBattleLaunchService.prepareScheduledBattle(war);
			Battle second = CampaignBattleLaunchService.prepareScheduledBattle(war);
			assertEquals(first.getId(), second.getId());
			assertEquals(1, BattleManager.get().size());
		});
	}

	@Test
	void launchAutoresolveBattle_startsImmediately() {
		War war = baseWar();
		war.setBattleSchedulePhase(BattleSchedulePhase.AUTORESOLVE_PENDING);

		withMockBossBar(() -> {
			Battle battle = CampaignBattleLaunchService.launchAutoresolveBattle(war);

			assertNotNull(battle);
			assertTrue(battle.hasStarted());
			assertEquals("campaign_w1_p20", battle.getId());
		});
	}

	@Test
	void tryStartScheduledBattle_startsWhenDue() {
		War war = scheduledWar();
		Instant startAt = war.getScheduledBattleAt();

		withMockBossBar(() -> {
			CampaignBattleLaunchService.prepareScheduledBattle(war);
			assertFalse(BattleManager.getByWarId(war.getId()).hasStarted());

			assertTrue(CampaignBattleLaunchService.tryStartScheduledBattle(war, startAt));
			assertTrue(BattleManager.getByWarId(war.getId()).hasStarted());
		});
	}

	private War baseWar() {
		War war = new War(1, attacker, defender);
		war.setGoal(WarGoalType.SUBJUGATE);
		war.setWarType(WarType.SUBJUGATE);
		war.setObjectiveProvinceId(30);
		war.setCampaignStartProvinceId(20);
		war.setCampaignProvinces(java.util.List.of(5, 10, 20, 30));
		war.setCursorIndex(2);
		war.setInitiativeAttacker(4);
		war.setInitiativeDefender(4);
		war.setCampaignPhase(CampaignPhase.INVASION);
		return war;
	}

	private War scheduledWar() {
		War war = baseWar();
		LocalDate battleDay = LocalDate.of(2026, 8, 21);
		war.setBattleDay(battleDay);
		war.setScheduledBattleHour(21);
		war.setScheduledBattleAt(battleDay.atTime(21, 0).atZone(ZoneOffset.UTC).toInstant());
		war.setScheduledBattleProvinceId(20);
		war.setBattleSchedulePhase(BattleSchedulePhase.SCHEDULED);
		return war;
	}

	private void withMockBossBar(Runnable action) {
		BossBar bossBar = mock(BossBar.class);
		try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
			bukkit.when(() -> Bukkit.createBossBar(anyString(), any(BarColor.class), any(BarStyle.class)))
					.thenReturn(bossBar);
			bukkit.when(() -> Bukkit.createBossBar(anyString(), any(BarColor.class), any(BarStyle.class), any()))
					.thenReturn(bossBar);
			action.run();
		}
	}
}
