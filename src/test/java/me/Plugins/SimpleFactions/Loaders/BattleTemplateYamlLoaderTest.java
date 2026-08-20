package me.Plugins.SimpleFactions.Loaders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.War.battle.enums.BattleType;
import me.Plugins.SimpleFactions.War.battle.template.BattleTemplate;

class BattleTemplateYamlLoaderTest {
	private Path tempDir;

	@BeforeEach
	void setUp() throws IOException {
		BattleTemplateLoader.resetForTests();
		Cache.worldName = "TFMC_Map";
		tempDir = Files.createTempDirectory("sf-battle-templates-");
	}

	@AfterEach
	void tearDown() throws IOException {
		if (tempDir != null) {
			Files.walk(tempDir)
					.sorted(java.util.Comparator.reverseOrder())
					.forEach(path -> path.toFile().delete());
		}
		BattleTemplateLoader.resetForTests();
	}

	@Test
	void load_parsesSampleYamlAndWorldFallback() throws IOException {
		Path yamlFile = tempDir.resolve("battle-templates.yml");
		Files.writeString(yamlFile, """
				raid_template:
				  type: raid
				  attacker:
				    spawn: { x: 1, y: 64, z: 2 }
				  defender:
				    spawn: { x: 3, y: 64, z: 4 }
				  defender_respawn_mode: infinite
				  raid_target:
				    id: target
				    location: { x: 5, y: 64, z: 6 }
				field_default:
				  type: field
				  attacker:
				    spawn: { x: 10, y: 70, z: 20 }
				  defender:
				    spawn: { x: 11, y: 70, z: 21 }
				  capture_points:
				    - id: alpha
				      location: { x: 12, y: 70, z: 22 }
				""");

		new BattleTemplateLoader().load(yamlFile.toFile());

		BattleTemplate raid = BattleTemplateLoader.getByName("raid_template");
		assertNotNull(raid);
		assertEquals(BattleType.RAID, raid.getType());
		assertNotNull(raid.getConfig().getAttacker().getSpawn());
		assertEquals("TFMC_Map", raid.getConfig().getAttacker().getSpawn().getWorld());
		assertNotNull(raid.getConfig().getRaidTarget());

		BattleTemplate field = BattleTemplateLoader.getByName("field_default");
		assertNotNull(field);
		assertEquals(1, field.getConfig().getCapturePoints().size());
		assertEquals("alpha", field.getConfig().getCapturePoints().get(0).getId());
	}

	@Test
	void load_invalidType_skipsTemplate() throws IOException {
		Path yamlFile = tempDir.resolve("battle-templates.yml");
		Files.writeString(yamlFile, """
				broken:
				  type: not_a_type
				  attacker:
				    spawn: { x: 0, y: 64, z: 0 }
				""");

		new BattleTemplateLoader().load(yamlFile.toFile());

		assertTrue(BattleTemplateLoader.getAll().isEmpty());
	}
}
