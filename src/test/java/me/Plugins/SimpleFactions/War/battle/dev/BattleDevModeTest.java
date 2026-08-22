package me.Plugins.SimpleFactions.War.battle.dev;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.Army.Military;
import me.Plugins.SimpleFactions.Army.Regiment;
import me.Plugins.SimpleFactions.Managers.WarManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.battle.campaign.CampaignBattleRosterService;
import me.Plugins.SimpleFactions.War.battle.engine.Battle;
import me.Plugins.SimpleFactions.War.battle.engine.BattleFactory;
import me.Plugins.SimpleFactions.War.battle.engine.BattleManager;
import me.Plugins.SimpleFactions.War.battle.engine.BattleSide;
import me.Plugins.SimpleFactions.War.battle.enums.BattleType;
import me.Plugins.SimpleFactions.War.battle.enums.LifeType;
import me.Plugins.SimpleFactions.War.battle.template.BattleTemplate;
import me.Plugins.SimpleFactions.War.battle.warband.Warband;
import me.Plugins.SimpleFactions.War.battle.warband.WarbandManager;

class BattleDevModeTest {
	private int previousPhantomCount;

	@BeforeEach
	void setUp() {
		previousPhantomCount = Cache.battleDevmodePhantomCount;
		Cache.battleDevmodePhantomCount = 10;
		BattleManager.resetForTests();
		WarbandManager.resetForTests();
		BattleDevMode.resetForTests();
	}

	@AfterEach
	void tearDown() {
		Cache.battleDevmodePhantomCount = previousPhantomCount;
		BattleDevMode.resetForTests();
	}

	@Test
	void toggle_notPersisted() {
		BattleDevMode.setEnabled(true);
		assertTrue(BattleDevMode.isEnabled());

		BattleDevMode.resetForTests();

		assertFalse(BattleDevMode.isEnabled());
	}

	@Test
	void seedDummyMembers_addsTenWithDisplayNames() {
		UUID leaderId = UUID.randomUUID();
		Warband warband = Warband.createWithMemberIds("test_band", leaderId, true);

		BattleDevMode.seedDummyMembers(warband, 10);

		assertEquals(11, warband.getMemberCount());
		assertEquals(10, warband.getDummyMemberCount());
		UUID firstDummy = BattleDevMode.dummyMemberId("test_band", 0);
		assertEquals(BattleDevMode.dummyDisplayName("test_band", 0), warband.getMemberDisplayName(firstDummy));
	}

	@Test
	void dummyMemberIds_deterministic() {
		UUID first = BattleDevMode.dummyMemberId("alpha", 0);
		UUID second = BattleDevMode.dummyMemberId("alpha", 0);

		assertEquals(first, second);
	}

	@Test
	void getPlayers_stillOnlineOnly() {
		UUID leaderId = UUID.randomUUID();
		Warband warband = Warband.createWithMemberIds("test_band", leaderId, true);

		BattleDevMode.seedDummyMembers(warband, 10);

		try (MockedStatic<org.bukkit.Bukkit> bukkit = mockStatic(org.bukkit.Bukkit.class)) {
			bukkit.when(() -> org.bukkit.Bukkit.getPlayer(Mockito.any(UUID.class))).thenReturn(null);
			assertTrue(warband.getPlayers().isEmpty());
		}
	}

	@Test
	void seedDummyMembersIfEnabled_skipsWhenOff() {
		UUID leaderId = UUID.randomUUID();
		Warband warband = Warband.createWithMemberIds("test_band", leaderId, true);

		BattleDevMode.seedDummyMembersIfEnabled(warband);

		assertEquals(1, warband.getMemberCount());
		assertEquals(0, warband.getDummyMemberCount());
	}

	@Test
	void seedDummyMembersIfEnabled_seedsWhenOn() {
		BattleDevMode.setEnabled(true);
		UUID leaderId = UUID.randomUUID();
		Warband warband = Warband.createWithMemberIds("test_band", leaderId, true);

		BattleDevMode.seedDummyMembersIfEnabled(warband);

		assertEquals(11, warband.getMemberCount());
		assertEquals(10, warband.getDummyMemberCount());
	}

	@Test
	void seedDummyMembersOnFirstSignupIfEnabled_noOpWhenDummiesAlreadySeeded() {
		BattleDevMode.setEnabled(true);
		UUID leaderId = UUID.randomUUID();
		Warband warband = Warband.createWithMemberIds("test_band", leaderId, true);
		BattleDevMode.seedDummyMembers(warband, 3);

		Faction attacker = mock(Faction.class);
		when(attacker.getId()).thenReturn("atk");
		War war = new War(1, attacker, mock(Faction.class));
		Battle battle = mock(Battle.class);
		when(battle.getProvinceId()).thenReturn(20);

		BattleDevMode.seedDummyMembersOnFirstSignupIfEnabled(
				warband, war, battle, BattleTemplate.ATTACKER_SIDE);

		assertEquals(3, warband.getDummyMemberCount());
	}

	@Test
	void seedDummyMembersOnFirstSignupIfEnabled_seedsWhenEmpty() {
		BattleDevMode.setEnabled(true);
		UUID leaderId = UUID.randomUUID();
		Warband warband = Warband.createWithMemberIds("test_band", leaderId, true);

		Faction attacker = mock(Faction.class);
		when(attacker.getId()).thenReturn("atk");
		War war = new War(1, attacker, mock(Faction.class));
		Battle battle = mock(Battle.class);
		when(battle.getProvinceId()).thenReturn(null);

		BattleDevMode.seedDummyMembersOnFirstSignupIfEnabled(
				warband, war, battle, BattleTemplate.ATTACKER_SIDE);

		assertEquals(10, warband.getDummyMemberCount());
	}

	@Test
	void seedCampaignSideIfEnabled_skipsWhenOff() {
		Faction attacker = mock(Faction.class);
		Faction defender = mock(Faction.class);
		when(attacker.getName()).thenReturn("Brume");
		when(attacker.getLeader()).thenReturn("Alice");
		War war = new War(1, attacker, defender);
		Warband warband = Warband.createCampaignSideShell(war, war.getAttackers(), BattleTemplate.ATTACKER_SIDE);
		Battle battle = mock(Battle.class);

		BattleDevMode.seedCampaignSideIfEnabled(warband, war, battle, BattleTemplate.ATTACKER_SIDE);

		assertEquals(0, warband.getDummyMemberCount());
		assertTrue(warband.isPendingLeader());
	}

	@Test
	void seedCampaignSideIfEnabled_setsDummyLeaderWithDisplayNameWhenOn() {
		BattleDevMode.setEnabled(true);
		Faction attacker = mock(Faction.class);
		Faction defender = mock(Faction.class);
		when(attacker.getName()).thenReturn("Brume");
		when(attacker.getLeader()).thenReturn("Alice");
		War war = new War(1, attacker, defender);
		Warband warband = Warband.createCampaignSideShell(war, war.getAttackers(), BattleTemplate.ATTACKER_SIDE);
		Battle battle = mock(Battle.class);
		when(battle.getProvinceId()).thenReturn(null);

		BattleDevMode.seedCampaignSideIfEnabled(warband, war, battle, BattleTemplate.ATTACKER_SIDE);

		assertEquals(10, warband.getDummyMemberCount());
		assertEquals(BattleDevMode.dummyMemberId(warband.getId(), 0), warband.getLeaderId());
		assertEquals(
				BattleDevMode.dummyDisplayName(warband.getId(), 0),
				warband.getLeaderDisplayName());
		assertNotEquals("Test Dummy", warband.getLeaderDisplayName());
	}

	@Test
	void sideRosterCount_matchesMemberCountWithDummies() {
		BattleDevMode.setEnabled(true);
		Faction attacker = mock(Faction.class);
		Faction defender = mock(Faction.class);
		when(attacker.getName()).thenReturn("Brume");
		when(attacker.getLeader()).thenReturn("Alice");
		War war = new War(1, attacker, defender);
		Warband warband = Warband.createCampaignSideShell(war, war.getAttackers(), BattleTemplate.ATTACKER_SIDE);
		Battle battle = mock(Battle.class);
		when(battle.getProvinceId()).thenReturn(null);

		BattleDevMode.seedCampaignSideIfEnabled(warband, war, battle, BattleTemplate.ATTACKER_SIDE);

		try (MockedStatic<org.bukkit.Bukkit> bukkit = mockStatic(org.bukkit.Bukkit.class)) {
			BossBar bossBar = mock(BossBar.class);
			bukkit.when(() -> org.bukkit.Bukkit.createBossBar(anyString(), any(BarColor.class), any(BarStyle.class)))
					.thenReturn(bossBar);
			BattleSide side = new BattleSide(BattleTemplate.ATTACKER_SIDE, LifeType.COLLECTIVE, 0);
			side.getBands().add(warband);

			assertEquals(warband.getMemberCount(), side.getAllParticipants());
			assertTrue(side.getAllParticipants() > 0);
		}
	}

	@Test
	void setEnabled_on_refillsCampaignSideWarbandsAfterRestart() {
		Faction attacker = mock(Faction.class);
		Faction defender = mock(Faction.class);
		when(attacker.getId()).thenReturn("atk");
		when(defender.getId()).thenReturn("def");
		when(attacker.getLeader()).thenReturn("Alice");
		when(defender.getLeader()).thenReturn("Carol");
		when(attacker.getName()).thenReturn("Attacker");
		when(defender.getName()).thenReturn("Defender");
		mockMilitary(attacker);
		mockMilitary(defender);

		War war = new War(1, attacker, defender);
		war.setScheduledBattleProvinceId(20);

		try (MockedStatic<org.bukkit.Bukkit> bukkit = mockStatic(org.bukkit.Bukkit.class);
				MockedStatic<WarManager> wars = mockStatic(WarManager.class);
				MockedStatic<me.Plugins.SimpleFactions.Managers.FactionManager> factions =
						mockStatic(me.Plugins.SimpleFactions.Managers.FactionManager.class);
				MockedStatic<me.Plugins.SimpleFactions.War.battle.military.BattlePoolService> pool =
						mockStatic(me.Plugins.SimpleFactions.War.battle.military.BattlePoolService.class)) {
			mockBossBar(bukkit);
			Battle battle = BattleFactory.createBlank(BattleType.FIELD, "campaign_w1");
			battle.setWarId(1);
			battle.setProvinceId(20);
			BattleManager.addBattle(battle);
			wars.when(() -> WarManager.getById(1)).thenReturn(war);
			factions.when(() -> me.Plugins.SimpleFactions.Managers.FactionManager.getByString("atk"))
					.thenReturn(attacker);
			factions.when(() -> me.Plugins.SimpleFactions.Managers.FactionManager.getByString("def"))
					.thenReturn(defender);
			pool.when(() -> me.Plugins.SimpleFactions.War.battle.military.BattlePoolService.totalCommittedRegiments(
					eq(war), eq(20), any())).thenReturn(5);

			CampaignBattleRosterService.enrollWarbands(war, battle);

			Warband attackerBand = WarbandManager.getByString(
					Warband.campaignSideWarbandId(1, BattleTemplate.ATTACKER_SIDE));
			Warband defenderBand = WarbandManager.getByString(
					Warband.campaignSideWarbandId(1, BattleTemplate.DEFENDER_SIDE));
			assertEquals(0, attackerBand.getDummyMemberCount());
			assertEquals(0, defenderBand.getDummyMemberCount());

			int filled = BattleDevMode.setEnabled(true);

			assertEquals(2, filled);
			assertEquals(10, attackerBand.getDummyMemberCount());
			assertEquals(10, defenderBand.getDummyMemberCount());
		}
	}

	@Test
	void setEnabled_off_clearsDummyMembersFromAllWarbands() {
		UUID leaderId = UUID.randomUUID();
		Warband manual = Warband.createWithMemberIds("manual", leaderId, true);
		WarbandManager.addWarband(manual);

		BattleDevMode.seedDummyMembers(manual, 5);
		assertEquals(5, manual.getDummyMemberCount());

		int cleared = BattleDevMode.setEnabled(false);

		assertEquals(1, cleared);
		assertEquals(0, manual.getDummyMemberCount());
		assertFalse(BattleDevMode.isEnabled());
	}

	private static void mockMilitary(Faction faction) {
		Military military = mock(Military.class);
		Regiment levy = mock(Regiment.class);
		when(military.getManpowerNoLevy(anyBoolean())).thenReturn(10);
		when(military.getRegiment("levy")).thenReturn(levy);
		when(levy.getEntries()).thenReturn(List.of());
		when(faction.getMilitary()).thenReturn(military);
	}

	private static void mockBossBar(MockedStatic<org.bukkit.Bukkit> bukkit) {
		BossBar bossBar = mock(BossBar.class);
		bukkit.when(() -> org.bukkit.Bukkit.createBossBar(anyString(), any(BarColor.class), any(BarStyle.class)))
				.thenReturn(bossBar);
		bukkit.when(() -> org.bukkit.Bukkit.createBossBar(
				anyString(), any(BarColor.class), any(BarStyle.class), any()))
				.thenReturn(bossBar);
	}
}
