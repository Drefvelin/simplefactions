package me.Plugins.SimpleFactions.War.battle.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.io.File;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import me.Plugins.SimpleFactions.War.battle.engine.core.Battle;
import me.Plugins.SimpleFactions.War.battle.engine.core.BattleFactory;
import me.Plugins.SimpleFactions.War.battle.engine.core.BattleJoinService;
import me.Plugins.SimpleFactions.War.battle.engine.core.BattleManager;
import me.Plugins.SimpleFactions.War.battle.enums.BattleType;
import me.Plugins.SimpleFactions.War.battle.template.BattleTemplate;
import me.Plugins.SimpleFactions.War.battle.warband.Warband;
import me.Plugins.SimpleFactions.War.battle.warband.WarbandManager;

class BattlePersistenceServiceTest {
	private BossBar bossBar;
	private MockedStatic<Bukkit> bukkit;

	@BeforeEach
	void setUp() {
		BattleManager.resetForTests();
		WarbandManager.resetForTests();
		cleanPersistenceDirs();

		bossBar = mock(BossBar.class);
		bukkit = mockStatic(Bukkit.class);
		bukkit.when(() -> Bukkit.createBossBar(anyString(), any(BarColor.class), any(BarStyle.class)))
				.thenReturn(bossBar);
	}

	@AfterEach
	void tearDown() {
		bukkit.close();
		cleanPersistenceDirs();
		BattleManager.resetForTests();
		WarbandManager.resetForTests();
	}

	@Test
	void saveAndLoad_restoresManualBattleAndReferencedWarband() {
		Battle battle = BattleFactory.createBlank(BattleType.FIELD, "persist_field");
		battle.setLocked(false);
		Warband warband = Warband.createWithMemberIds("persist_band", UUID.randomUUID(), true);
		WarbandManager.addWarband(warband);
		BattleManager.addBattle(battle);
		assertNull(BattleJoinService.join(warband, battle, BattleTemplate.ATTACKER_SIDE));
		battle.setStarted(true);

		BattlePersistenceService.saveAll();

		assertTrue(new File("plugins/SimpleFactions/Battles/battle_persist_field.json").exists());
		assertTrue(new File("plugins/SimpleFactions/Warbands/warband_persist_band.json").exists());

		BattleManager.resetForTests();
		WarbandManager.resetForTests();
		BattlePersistenceService.loadAll();

		Battle loaded = BattleManager.getByString("persist_field");
		assertNotNull(loaded);
		assertTrue(loaded.hasStarted());
		assertNotNull(WarbandManager.getByString("persist_band"));
		assertNotNull(loaded.getSideById(BattleTemplate.ATTACKER_SIDE));
		assertEquals(1, loaded.getSideById(BattleTemplate.ATTACKER_SIDE).getBands().size());
	}

	@Test
	void saveAll_purgesOrphanManualWarbands() {
		Warband orphan = Warband.createWithMemberIds("orphan_band", UUID.randomUUID(), true);
		WarbandManager.addWarband(orphan);
		BattlePersistenceService.saveAll();

		assertNull(WarbandManager.getByString("orphan_band"));
		assertFalse(new File("plugins/SimpleFactions/Warbands/warband_orphan_band.json").exists());
	}

	@Test
	void loadAll_keepsSingleManualBattleWhenMultipleFilesExist() {
		writeManualBattleFile("manual_a", false);
		writeManualBattleFile("manual_b", true);

		BattlePersistenceService.loadAll();

		assertEquals(1, BattleManager.get().size());
		assertNotNull(BattleManager.getByString("manual_b"));
		assertFalse(new File("plugins/SimpleFactions/Battles/battle_manual_a.json").exists());
	}

	@Test
	void hasManualBattle_blocksSecondManualBattle() {
		BattleManager.addBattle(BattleFactory.createBlank(BattleType.FIELD, "only_manual"));
		assertTrue(BattleManager.hasManualBattle());
	}

	private void writeManualBattleFile(String id, boolean started) {
		File folder = new File("plugins/SimpleFactions/Battles");
		folder.mkdirs();
		Battle battle = BattleFactory.createBlank(BattleType.FIELD, id);
		battle.setStarted(started);
		new me.Plugins.SimpleFactions.Database.Database().saveBattle(battle);
	}

	private void cleanPersistenceDirs() {
		deleteRecursively(new File("plugins/SimpleFactions/Battles"));
		deleteRecursively(new File("plugins/SimpleFactions/Warbands"));
	}

	private void deleteRecursively(File file) {
		if (file == null || !file.exists()) {
			return;
		}
		if (file.isDirectory()) {
			File[] children = file.listFiles();
			if (children != null) {
				for (File child : children) {
					deleteRecursively(child);
				}
			}
		}
		file.delete();
	}
}
