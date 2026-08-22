package me.Plugins.SimpleFactions.War.schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import me.Plugins.SimpleFactions.Managers.ProvinceManager;
import me.Plugins.SimpleFactions.Managers.TitleManager;
import me.Plugins.SimpleFactions.Map.Provinces.Province;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.enums.CampaignBattleKind;
import me.Plugins.SimpleFactions.War.enums.WarGoalType;
import me.Plugins.SimpleFactions.War.progression.CampaignCoalition;
import me.Plugins.SimpleFactions.War.progression.CampaignPushTarget;
import me.Plugins.SimpleFactions.enums.Terrain;

class CampaignScheduleServiceTest {
	private Faction attacker;
	private Faction defender;
	private ProvinceManager pm;
	private MockedStatic<SimpleFactions> simpleFactions;
	private MockedStatic<TitleManager> titleManager;

	@BeforeEach
	void setUp() {
		attacker = mock(Faction.class);
		defender = mock(Faction.class);
		when(attacker.getId()).thenReturn("atk");
		when(defender.getId()).thenReturn("def");

		pm = new ProvinceManager();
		Province p5 = province(5);
		Province p10 = province(10);
		Province p20 = province(20);
		Province p30 = province(30);
		link(p5, p10);
		link(p10, p20);
		link(p20, p30);
		pm.start(Map.of(5, p5, 10, p10, 20, p20, 30, p30));

		SimpleFactions plugin = mock(SimpleFactions.class);
		when(plugin.getProvinceManager()).thenReturn(pm);
		simpleFactions = mockStatic(SimpleFactions.class);
		simpleFactions.when(SimpleFactions::getInstance).thenReturn(plugin);

		titleManager = mockStatic(TitleManager.class);
		titleManager.when(() -> TitleManager.getByProvince(5)).thenReturn(attacker);
		titleManager.when(() -> TitleManager.getByProvince(10)).thenReturn(attacker);
		titleManager.when(() -> TitleManager.getByProvince(20)).thenReturn(defender);
		titleManager.when(() -> TitleManager.getByProvince(30)).thenReturn(defender);
	}

	@AfterEach
	void tearDown() {
		if (titleManager != null) {
			titleManager.close();
		}
		if (simpleFactions != null) {
			simpleFactions.close();
		}
	}

	@Test
	void currentSlot_returnsIndex0Then1() {
		War war = warWithSchedule(
				field(20),
				siege(18, "fort_a"));

		assertEquals(20, CampaignScheduleService.currentSlot(war).orElseThrow().provinceId());

		CampaignScheduleService.advanceIndex(war);

		assertEquals(18, CampaignScheduleService.currentSlot(war).orElseThrow().provinceId());
	}

	@Test
	void advanceIndex_increments() {
		War war = warWithSchedule(field(20));
		assertEquals(0, war.getCampaignScheduleIndex());

		CampaignScheduleService.advanceIndex(war);

		assertEquals(1, war.getCampaignScheduleIndex());
	}

	@Test
	void insertSiegeAtCurrentIndex_shiftsTail() {
		War war = warWithSchedule(field(20), field(30));
		OperationalFort fort = new OperationalFort("fort_a", defender, 18, 100L);

		CampaignScheduleService.insertSiegeAtCurrentIndex(war, fort);

		assertEquals(3, war.getCampaignBattleSchedule().size());
		assertEquals(CampaignBattleKind.SIEGE, war.getCampaignBattleSchedule().get(0).kind());
		assertEquals("fort_a", war.getCampaignBattleSchedule().get(0).fortInstallationId());
		assertEquals(20, war.getCampaignBattleSchedule().get(1).provinceId());
	}

	@Test
	void ensureReSiegeInsert_addsSiegeWhenEnemyFortOnPath() {
		War war = warWithSchedule(field(30));
		war.putFortController("fort_a", CampaignCoalition.AGGRESSOR);
		war.setInitiativeHolderCoalition(CampaignCoalition.DEFENDER);
		war.setPushTarget(CampaignPushTarget.TOWARD_AGGRESSOR_CAPITAL);

		FortZocIndex index = FortZocIndex.fromForts(List.of(
				new OperationalFort("fort_a", attacker, 10, 100L)));

		CampaignScheduleService.ensureReSiegeInsert(war, index);

		assertEquals(CampaignBattleKind.SIEGE, war.getCampaignBattleSchedule().get(0).kind());
		assertEquals(10, war.getCampaignBattleSchedule().get(0).provinceId());
	}

	@Test
	void ensureReSiegeInsert_skipsWhenCurrentSlotAlreadySiegeForFort() {
		War war = warWithSchedule(siege(10, "fort_a"), field(30));
		war.putFortController("fort_a", CampaignCoalition.AGGRESSOR);
		war.setInitiativeHolderCoalition(CampaignCoalition.DEFENDER);
		war.setPushTarget(CampaignPushTarget.TOWARD_AGGRESSOR_CAPITAL);

		FortZocIndex index = FortZocIndex.fromForts(List.of(
				new OperationalFort("fort_a", attacker, 10, 100L)));

		CampaignScheduleService.ensureReSiegeInsert(war, index);

		assertEquals(2, war.getCampaignBattleSchedule().size());
		assertEquals(CampaignBattleKind.SIEGE, war.getCampaignBattleSchedule().get(0).kind());
	}

	@Test
	void hasSchedule_falseWhenEmpty() {
		War war = new War(1, attacker, defender);
		assertFalse(CampaignScheduleService.hasSchedule(war));
	}

	@Test
	void slotForProvince_returnsMatchingSlot() {
		War war = warWithSchedule(field(20), siege(18, "fort_a"));
		assertEquals(20, CampaignScheduleService.slotForProvince(war, 20).orElseThrow().provinceId());
		assertEquals("fort_a", CampaignScheduleService.slotForProvince(war, 18).orElseThrow().fortInstallationId());
		assertTrue(CampaignScheduleService.slotForProvince(war, 99).isEmpty());
	}

	private War warWithSchedule(ScheduledCampaignBattle... slots) {
		War war = new War(1, attacker, defender);
		war.setGoal(WarGoalType.SUBJUGATE);
		war.setObjectiveProvinceId(30);
		war.setCampaignProvinces(List.of(5, 10, 20, 30));
		war.setCursorIndex(2);
		war.setInitiativeAttacker(4);
		war.setInitiativeDefender(4);
		war.setInitiativeHolderCoalition(CampaignCoalition.AGGRESSOR);
		war.setPushTarget(CampaignPushTarget.TOWARD_OBJECTIVE);
		war.setCampaignBattleSchedule(List.of(slots));
		war.setCampaignScheduleIndex(0);
		return war;
	}

	private static ScheduledCampaignBattle field(int provinceId) {
		return new ScheduledCampaignBattle(provinceId, CampaignBattleKind.FIELD, false, null);
	}

	private static ScheduledCampaignBattle siege(int provinceId, String fortId) {
		return new ScheduledCampaignBattle(provinceId, CampaignBattleKind.SIEGE, false, fortId);
	}

	private Province province(int id) {
		return new Province(id, Terrain.PLAINS.name(), 50, id * 10, id * 10);
	}

	private void link(Province a, Province b) {
		a.addNeighbour(b.getId());
		b.addNeighbour(a.getId());
	}
}
