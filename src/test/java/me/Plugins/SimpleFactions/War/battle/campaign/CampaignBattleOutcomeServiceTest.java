package me.Plugins.SimpleFactions.War.battle.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

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
import me.Plugins.SimpleFactions.War.battle.military.BattleCasualtyService;
import me.Plugins.SimpleFactions.War.battle.template.BattleTemplate;
import me.Plugins.SimpleFactions.War.enums.BattleSchedulePhase;
import me.Plugins.SimpleFactions.War.enums.CampaignBattleKind;
import me.Plugins.SimpleFactions.War.enums.CampaignPhase;
import me.Plugins.SimpleFactions.War.enums.WarGoalType;
import me.Plugins.SimpleFactions.War.enums.WarType;
import me.Plugins.SimpleFactions.War.progression.BelligerentRole;
import me.Plugins.SimpleFactions.War.progression.CampaignCapabilityService;
import me.Plugins.SimpleFactions.War.progression.CampaignCoalition;
import me.Plugins.SimpleFactions.War.progression.CampaignPostBattleChoiceService;
import me.Plugins.SimpleFactions.War.progression.CampaignPushTarget;
import me.Plugins.SimpleFactions.War.progression.PostBattleChoicePhase;
import me.Plugins.SimpleFactions.War.schedule.ScheduledCampaignBattle;
import me.Plugins.SimpleFactions.War.schedule.BattleScheduleService;
import me.Plugins.SimpleFactions.enums.Terrain;

class CampaignBattleOutcomeServiceTest {
	private Faction attacker;
	private Faction defender;
	private MockedStatic<Bukkit> bukkitMock;
	private MockedStatic<WarManager> warManagerMock;
	private MockedStatic<TitleManager> titleManagerMock;
	private MockedStatic<CampaignCapabilityService> capabilityMock;
	private SimpleFactions pluginBackup;

	@BeforeEach
	void setUp() {
		BattleManager.resetForTests();
		Cache.warFirstBattleAtBorder = true;

		attacker = mock(Faction.class);
		defender = mock(Faction.class);
		when(attacker.getId()).thenReturn("atk");
		when(defender.getId()).thenReturn("def");
		when(attacker.getMembers()).thenReturn(List.of());
		when(defender.getMembers()).thenReturn(List.of());

		ProvinceManager pm = new ProvinceManager();
		Province battleProvince = new Province(20, Terrain.PLAINS.name(), 50, 200, 200);
		pm.start(Map.of(20, battleProvince));

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

		capabilityMock = mockStatic(CampaignCapabilityService.class, CALLS_REAL_METHODS);
		capabilityMock.when(() -> CampaignCapabilityService.canMountOffensiveAfterPush(any(), any()))
				.thenReturn(true);
	}

	@AfterEach
	void tearDown() {
		bukkitMock.close();
		warManagerMock.close();
		titleManagerMock.close();
		capabilityMock.close();
		SimpleFactions.plugin = pluginBackup;
	}

	@Test
	void applyCampaignBattleOutcome_siegeWin_flipsFortControllerAndAdvancesIndex() {
		War war = baseWar();
		war.setCampaignBattleSchedule(List.of(
				new ScheduledCampaignBattle(20, CampaignBattleKind.SIEGE, false, "fort_a")));
		war.setCampaignScheduleIndex(0);
		war.putFortController("fort_a", CampaignCoalition.DEFENDER);
		warManagerMock.when(() -> WarManager.getById(1)).thenReturn(war);

		CampaignBattleOutcomeService.applyCampaignBattleOutcome(
				war,
				BelligerentRole.ATTACKER,
				20);

		assertEquals(CampaignCoalition.AGGRESSOR, war.getFortControllers().get("fort_a"));
		assertEquals(1, war.getCampaignScheduleIndex());
		assertEquals(1, war.getCampaignBattlesFought());
	}

	@Test
	void handleBattleEnded_attackerWin_opensWinnerChoiceWithoutMovingCursor() {
		War war = baseWar();
		warManagerMock.when(() -> WarManager.getById(1)).thenReturn(war);

		Battle battle = BattleFactory.createBlank(BattleType.FIELD, "campaign_w1_p20");
		battle.setWarId(1);
		battle.setProvinceId(20);
		BattleManager.addBattle(battle);

		CampaignBattleOutcomeService.handleBattleEnded(
				new BattleEndedEvent(
						battle.getId(),
						BattleType.FIELD,
						1,
						BattleTemplate.ATTACKER_SIDE,
						Map.of()));

		assertEquals(BattleSchedulePhase.VOTING, war.getBattleSchedulePhase());
		assertEquals(2, war.getCursorIndex());
		assertTrue(CampaignPostBattleChoiceService.needsWinnerChoice(war));
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
				new BattleEndedEvent(battle.getId(), BattleType.FIELD, 1, null, Map.of()));

		assertEquals(BattleSchedulePhase.VOTING, war.getBattleSchedulePhase());
		assertEquals(cursorBefore, war.getCursorIndex());
		assertTrue(BattleManager.get().isEmpty());
	}

	@Test
	void handleBattleEnded_skipsManualBattlesWithoutWarId() {
		Battle battle = BattleFactory.createBlank(BattleType.FIELD, "staff_field");
		BattleManager.addBattle(battle);

		CampaignBattleOutcomeService.handleBattleEnded(
				new BattleEndedEvent(
						battle.getId(),
						BattleType.FIELD,
						null,
						BattleTemplate.ATTACKER_SIDE,
						Map.of()));

		assertEquals(1, BattleManager.get().size());
	}

	@Test
	void handleBattleEnded_appliesCasualtiesBeforeOpenVote() {
		War war = baseWar();
		warManagerMock.when(() -> WarManager.getById(1)).thenReturn(war);

		Battle battle = BattleFactory.createBlank(BattleType.FIELD, "campaign_w1_p20");
		battle.setWarId(1);
		battle.setProvinceId(20);
		BattleManager.addBattle(battle);

		List<String> steps = new ArrayList<>();
		try (MockedStatic<BattleCasualtyService> casualtyMock = mockStatic(BattleCasualtyService.class);
				MockedStatic<BattleScheduleService> scheduleMock = mockStatic(BattleScheduleService.class)) {
			casualtyMock.when(() -> BattleCasualtyService.applyBattleCasualties(any(), any(), any()))
					.then(inv -> {
						steps.add("casualties");
						return null;
					});
			scheduleMock.when(() -> BattleScheduleService.openVote(any())).then(inv -> {
				steps.add("vote");
				return null;
			});

			CampaignBattleOutcomeService.handleBattleEnded(
					new BattleEndedEvent(
							battle.getId(),
							BattleType.FIELD,
							1,
							null,
							Map.of(BattleTemplate.ATTACKER_SIDE, 2)));
		}

		assertEquals(List.of("casualties", "vote"), steps);
	}

	@Test
	void applyCampaignBattleOutcome_winnerWithoutNextOffensive_autoHolds() {
		War war = baseWar();
		warManagerMock.when(() -> WarManager.getById(1)).thenReturn(war);
		capabilityMock.when(() -> CampaignCapabilityService.canMountOffensiveAfterPush(war, CampaignCoalition.DEFENDER))
				.thenReturn(false);

		CampaignBattleOutcomeService.CampaignBattleApplyResult result =
				CampaignBattleOutcomeService.applyCampaignBattleOutcome(
						war,
						BelligerentRole.DEFENDER,
						20);

		assertTrue(result.progressionApplied());
		assertTrue(result.postBattleChoicePending());
		assertEquals(PostBattleChoicePhase.LOSER_ATTACK_PEACE, war.getPostBattleChoicePhase());
		assertTrue(CampaignPostBattleChoiceService.needsLoserResponse(war));
	}

	@Test
	void applyCampaignBattleOutcome_defenderWin_opensWinnerChoice() {
		War war = baseWar();
		warManagerMock.when(() -> WarManager.getById(1)).thenReturn(war);

		CampaignBattleOutcomeService.CampaignBattleApplyResult result =
				CampaignBattleOutcomeService.applyCampaignBattleOutcome(
						war,
						BelligerentRole.DEFENDER,
						20);

		assertTrue(result.progressionApplied());
		assertTrue(result.postBattleChoicePending());
		assertEquals(BattleSchedulePhase.VOTING, war.getBattleSchedulePhase());
		assertEquals(2, war.getCursorIndex());
		assertEquals(BelligerentRole.ATTACKER, war.getInitiativeHolder());
		assertTrue(CampaignPostBattleChoiceService.needsWinnerChoice(war));
	}

	@Test
	void handleBattleEnded_defenderWin_flipsInitiativeAndOpensChoice() {
		War war = baseWar();
		warManagerMock.when(() -> WarManager.getById(1)).thenReturn(war);

		Battle battle = BattleFactory.createBlank(BattleType.FIELD, "campaign_w1_p20");
		battle.setWarId(1);
		battle.setProvinceId(20);
		BattleManager.addBattle(battle);

		CampaignBattleOutcomeService.handleBattleEnded(
				new BattleEndedEvent(
						battle.getId(),
						BattleType.FIELD,
						1,
						BattleTemplate.DEFENDER_SIDE,
						Map.of()));

		assertEquals(BattleSchedulePhase.VOTING, war.getBattleSchedulePhase());
		assertEquals(2, war.getCursorIndex());
		assertEquals(BelligerentRole.ATTACKER, war.getInitiativeHolder());
		assertTrue(CampaignPostBattleChoiceService.needsWinnerChoice(war));
	}

	private War baseWar() {
		War war = new War(1, attacker, defender);
		war.setGoal(WarGoalType.SUBJUGATE);
		war.setWarType(WarType.SUBJUGATE);
		war.setObjectiveProvinceId(30);
		war.setCampaignStartProvinceId(20);
		war.setCampaignProvinces(List.of(5, 10, 20, 30));
		war.setCursorIndex(2);
		war.setInitiativeAttacker(4);
		war.setInitiativeDefender(4);
		war.setInitiativeHolder(BelligerentRole.ATTACKER);
		war.setCampaignPhase(CampaignPhase.INVASION);
		war.setPushTarget(CampaignPushTarget.TOWARD_OBJECTIVE);
		war.setPostBattleChoicePhase(PostBattleChoicePhase.NONE);
		war.setPostBattleChoiceResolved(true);
		war.setBattleSchedulePhase(BattleSchedulePhase.SCHEDULED);
		return war;
	}
}
