package me.Plugins.SimpleFactions.War.battle.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import me.Plugins.SimpleFactions.Army.Military;
import me.Plugins.SimpleFactions.Army.Regiment;
import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.Managers.ProvinceManager;
import me.Plugins.SimpleFactions.Managers.TitleManager;
import me.Plugins.SimpleFactions.Managers.WarManager;
import me.Plugins.SimpleFactions.Map.Provinces.Province;
import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.battle.engine.Battle;
import me.Plugins.SimpleFactions.War.battle.engine.BattleFactory;
import me.Plugins.SimpleFactions.War.battle.engine.BattleManager;
import me.Plugins.SimpleFactions.War.battle.enums.BattleType;
import me.Plugins.SimpleFactions.War.battle.events.BattleEndedEvent;
import me.Plugins.SimpleFactions.War.battle.template.BattleTemplate;
import me.Plugins.SimpleFactions.War.enums.BattleSchedulePhase;
import me.Plugins.SimpleFactions.War.enums.CampaignPhase;
import me.Plugins.SimpleFactions.War.enums.WarGoalType;
import me.Plugins.SimpleFactions.War.enums.WarType;
import me.Plugins.SimpleFactions.enums.Terrain;

class CampaignBattleOutcomeServiceTest {
	private Faction attacker;
	private Faction defender;
	private MockedStatic<Bukkit> bukkitMock;
	private MockedStatic<WarManager> warManagerMock;
	private MockedStatic<TitleManager> titleManagerMock;
	private SimpleFactions pluginBackup;

	@BeforeEach
	void setUp() {
		BattleManager.resetForTests();
		Cache.warFirstBattleAtBorder = true;

		attacker = mock(Faction.class);
		defender = mock(Faction.class);
		when(attacker.getId()).thenReturn("atk");
		when(defender.getId()).thenReturn("def");
		when(attacker.getMembers()).thenReturn(java.util.List.of());
		when(defender.getMembers()).thenReturn(java.util.List.of());

		ProvinceManager pm = new ProvinceManager();
		Province battleProvince = new Province(20, Terrain.PLAINS.name(), 50, 200, 200);
		pm.start(java.util.Map.of(20, battleProvince));

		pluginBackup = SimpleFactions.plugin;
		SimpleFactions plugin = mock(SimpleFactions.class);
		when(plugin.getProvinceManager()).thenReturn(pm);
		SimpleFactions.plugin = plugin;

		titleManagerMock = mockStatic(TitleManager.class);
		titleManagerMock.when(() -> TitleManager.getByProvince(20)).thenReturn(defender);

		BossBar bossBar = mock(BossBar.class);
		bukkitMock = mockStatic(Bukkit.class);
		bukkitMock.when(() -> Bukkit.createBossBar(anyString(), any(BarColor.class), any(BarStyle.class)))
				.thenReturn(bossBar);
		bukkitMock.when(() -> Bukkit.createBossBar(anyString(), any(BarColor.class), any(BarStyle.class), any()))
				.thenReturn(bossBar);

		warManagerMock = mockStatic(WarManager.class);
		warManagerMock.when(() -> WarManager.persist(any())).then(inv -> null);
	}

	@AfterEach
	void tearDown() {
		bukkitMock.close();
		warManagerMock.close();
		titleManagerMock.close();
		SimpleFactions.plugin = pluginBackup;
	}

	@Test
	void handleBattleEnded_attackerWin_appliesProgressionAndReopensVote() {
		War war = baseWar();
		warManagerMock.when(() -> WarManager.getById(1)).thenReturn(war);

		Battle battle = BattleFactory.createBlank(BattleType.FIELD, "campaign_w1_p20");
		battle.setWarId(1);
		battle.setProvinceId(20);
		BattleManager.addBattle(battle);

		CampaignBattleOutcomeService.handleBattleEnded(
				new BattleEndedEvent(battle.getId(), BattleType.FIELD, 1, BattleTemplate.ATTACKER_SIDE));

		assertEquals(BattleSchedulePhase.VOTING, war.getBattleSchedulePhase());
		assertEquals(3, war.getCursorIndex());
		assertTrue(BattleManager.get().isEmpty());
	}

	@Test
	void handleBattleEnded_noWinner_reopensVoteOnly() {
		War war = baseWar();
		int cursorBefore = war.getCursorIndex();
		warManagerMock.when(() -> WarManager.getById(1)).thenReturn(war);

		Battle battle = BattleFactory.createBlank(BattleType.FIELD, "campaign_w1_p20");
		battle.setWarId(1);
		battle.setProvinceId(20);
		BattleManager.addBattle(battle);

		CampaignBattleOutcomeService.handleBattleEnded(
				new BattleEndedEvent(battle.getId(), BattleType.FIELD, 1, null));

		assertEquals(BattleSchedulePhase.VOTING, war.getBattleSchedulePhase());
		assertEquals(cursorBefore, war.getCursorIndex());
		assertTrue(BattleManager.get().isEmpty());
	}

	@Test
	void handleBattleEnded_skipsManualBattlesWithoutWarId() {
		Battle battle = BattleFactory.createBlank(BattleType.FIELD, "staff_field");
		BattleManager.addBattle(battle);

		CampaignBattleOutcomeService.handleBattleEnded(
				new BattleEndedEvent(battle.getId(), BattleType.FIELD, null, BattleTemplate.ATTACKER_SIDE));

		assertEquals(1, BattleManager.get().size());
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
		war.setBattleSchedulePhase(BattleSchedulePhase.SCHEDULED);
		return war;
	}
}
