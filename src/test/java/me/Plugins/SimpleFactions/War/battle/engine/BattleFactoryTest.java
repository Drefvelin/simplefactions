package me.Plugins.SimpleFactions.War.battle.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.Loaders.BattleTemplateLoader;
import me.Plugins.SimpleFactions.War.battle.enums.BattleType;
import me.Plugins.SimpleFactions.War.battle.enums.DefenderRespawnMode;
import me.Plugins.SimpleFactions.War.battle.template.BattleTemplate;
import me.Plugins.SimpleFactions.War.battle.template.BattleTemplateService;

class BattleFactoryTest {
	@BeforeEach
	void setUp() {
		BattleTemplateLoader.resetForTests();
		BattleTemplateService.resetForTests();
		Cache.battleSiegeContestDurationSeconds = 180;
		Cache.battleRaidDefenderRespawnModeDefault = DefenderRespawnMode.INFINITE;
		Cache.worldName = "TFMC_Map";
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

	@Test
	void createBlank_setsTypeAndEmptyTemplate() {
		withMockBossBar(() -> {
			Battle battle = BattleFactory.createBlank(BattleType.RAID, "test_raid");

			assertEquals("test_raid", battle.getId());
			assertEquals(BattleType.RAID, battle.getBattleType());
			assertNull(battle.getTemplateName());
			assertNotNull(battle.getSideById("attacker"));
			assertNotNull(battle.getSideById("defender"));
			assertTrue(battle.getPoints().isEmpty());
		});
	}

	@Test
	void applyTemplate_seedsSiegeContestMetadata() {
		YamlConfiguration config = new YamlConfiguration();
		config.set("type", "siege");
		config.set("contest_duration_seconds", 240);
		config.createSection("contest_area").createSection("min").set("x", 1);
		config.set("contest_area.min.y", 60);
		config.set("contest_area.min.z", 1);
		config.set("contest_area.max.x", 10);
		config.set("contest_area.max.y", 80);
		config.set("contest_area.max.z", 10);
		BattleTemplateLoader.putForTests(new BattleTemplate("siege_default", config));

		withMockBossBar(() -> {
			Battle battle = BattleFactory.createBlank(BattleType.SIEGE, "test_siege");
			BattleFactory.applyTemplate(battle, "siege_default");

			assertEquals("siege_default", battle.getTemplateName());
			assertEquals(240, battle.getContestDurationSeconds());
			assertTrue(battle.getContestArea() != null);
			assertTrue(battle.getContestArea().getMin() != null);
			assertTrue(battle.getContestArea().getMax() != null);
		});
	}

	@Test
	void applyTemplate_seedsLayoutAndMetadata() {
		YamlConfiguration config = new YamlConfiguration();
		config.set("type", "raid");
		config.set("defender_respawn_mode", "infinite");
		config.createSection("raid_target").set("id", "target");
		config.set("raid_target.location.x", 50);
		config.set("raid_target.location.y", 64);
		config.set("raid_target.location.z", 50);
		BattleTemplateLoader.putForTests(new BattleTemplate("raid_template", config));

		withMockBossBar(() -> {
			Battle battle = BattleFactory.createBlank(BattleType.RAID, "test_raid");
			BattleFactory.applyTemplate(battle, "raid_template");

			assertEquals("raid_template", battle.getTemplateName());
			assertEquals(DefenderRespawnMode.INFINITE, battle.getDefenderRespawnMode());
			assertNotNull(battle.getRaidTarget());
			assertEquals("target", battle.getRaidTarget().getId());
		});
	}

	@Test
	void resetToBase_clearsTemplateMetadata() {
		YamlConfiguration config = new YamlConfiguration();
		config.set("type", "field");
		config.set("lives", 30);
		BattleTemplateLoader.putForTests(new BattleTemplate("field_default", config));

		withMockBossBar(() -> {
			Battle battle = BattleFactory.createBlank(BattleType.FIELD, "test_field");
			BattleFactory.applyTemplate(battle, "field_default");
			BattleFactory.resetToBase(battle);

			assertNull(battle.getTemplateName());
			assertTrue(battle.getPoints().isEmpty());
			assertNotNull(battle.getSideById("attacker"));
			assertEquals(25, battle.getLives());
		});
	}

	@Test
	void applyTemplate_wipesPreviousLayout() {
		YamlConfiguration fieldA = new YamlConfiguration();
		fieldA.set("type", "field");
		fieldA.set("lives", 10);
		BattleTemplateLoader.putForTests(new BattleTemplate("field_a", fieldA));

		YamlConfiguration fieldB = new YamlConfiguration();
		fieldB.set("type", "field");
		fieldB.set("lives", 40);
		BattleTemplateLoader.putForTests(new BattleTemplate("field_b", fieldB));

		withMockBossBar(() -> {
			Battle battle = BattleFactory.createBlank(BattleType.FIELD, "test_field");
			BattleFactory.applyTemplate(battle, "field_a");
			assertEquals(10, battle.getLives());

			battle.setFriendlyFire(false);
			BattleFactory.applyTemplate(battle, "field_b");

			assertEquals("field_b", battle.getTemplateName());
			assertEquals(40, battle.getLives());
			assertTrue(battle.hasFriendlyFire());
		});
	}

	@Test
	void applyTemplate_preservesCampaignIds() {
		YamlConfiguration config = new YamlConfiguration();
		config.set("type", "siege");
		config.set("contest_duration_seconds", 240);
		BattleTemplateLoader.putForTests(new BattleTemplate("siege_default", config));

		withMockBossBar(() -> {
			Battle battle = BattleFactory.createBlank(BattleType.SIEGE, "test_siege");
			battle.setProvinceId(42);
			battle.setWarId(7);
			BattleFactory.applyTemplate(battle, "siege_default");

			assertEquals(42, battle.getProvinceId());
			assertEquals(7, battle.getWarId());
			assertEquals("siege_default", battle.getTemplateName());
		});
	}

	@Test
	void applyTemplate_typeMismatch_throws() {
		YamlConfiguration config = new YamlConfiguration();
		config.set("type", "raid");
		BattleTemplateLoader.putForTests(new BattleTemplate("raid_template", config));

		withMockBossBar(() -> {
			Battle battle = BattleFactory.createBlank(BattleType.FIELD, "x");
			IllegalArgumentException error = assertThrows(
					IllegalArgumentException.class,
					() -> BattleFactory.applyTemplate(battle, "raid_template"));
			assertTrue(error.getMessage().contains("raid_template"));
		});
	}

	@Test
	void applyTemplate_whenStarted_throws() {
		withMockBossBar(() -> {
			Battle battle = BattleFactory.createBlank(BattleType.FIELD, "x");
			try {
				java.lang.reflect.Field started = Battle.class.getDeclaredField("started");
				started.setAccessible(true);
				started.setBoolean(battle, true);
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
			assertThrows(IllegalStateException.class, () -> BattleFactory.resetToBase(battle));
		});
	}

	@Test
	void applyCampaignDefault_appliesConfiguredTemplate() {
		Cache.battleCampaignTemplateRaid = "raid_template";
		YamlConfiguration config = new YamlConfiguration();
		config.set("type", "raid");
		config.set("defender_respawn_mode", "infinite");
		BattleTemplateLoader.putForTests(new BattleTemplate("raid_template", config));

		withMockBossBar(() -> {
			Battle battle = BattleFactory.createBlank(BattleType.RAID, "campaign_raid");
			BattleFactory.applyCampaignDefault(battle);
			assertEquals("raid_template", battle.getTemplateName());
		});
	}
}
