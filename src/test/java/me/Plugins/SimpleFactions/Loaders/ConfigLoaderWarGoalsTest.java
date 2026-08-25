package me.Plugins.SimpleFactions.Loaders;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.War.enums.WarGoalType;

class ConfigLoaderWarGoalsTest {
	private Path tempDir;

	@BeforeEach
	void setUp() throws IOException {
		tempDir = Files.createTempDirectory("sf-config-war-goals-");
	}

	@AfterEach
	void tearDown() throws IOException {
		if (tempDir != null) {
			Files.walk(tempDir)
					.sorted(java.util.Comparator.reverseOrder())
					.forEach(path -> path.toFile().delete());
		}
	}

	@Test
	void loadConfig_readsInitiativeFactorAndGoalMaxBattles() throws IOException {
		Path file = writeConfig("""
				war:
				  initiative_factor: 2.0
				  goals:
				    SUBJUGATE:
				      max_battles: 5
				    de_jure_annex:
				      max_battles: 3
				""");

		new ConfigLoader().loadConfig(file.toFile());

		assertEquals(2.0, Cache.warInitiativeFactor);
		assertEquals(4, Cache.warGoalMaxBattles.get(WarGoalType.SUBJUGATE));
		assertEquals(3, Cache.warGoalMaxBattles.get(WarGoalType.DE_JURE_ANNEX));
		assertEquals(4, Cache.warGoalMaxBattles.get(WarGoalType.TRANSFER_SUBJECT));
	}

	@Test
	void loadConfig_prefersMaxBattlesPerLeg() throws IOException {
		Path file = writeConfig("""
				war:
				  goals:
				    SUBJUGATE:
				      max_battles_per_leg: 6
				      max_battles: 2
				    TRANSFER_SUBJECT:
				      max_battles_per_leg: 5
				""");

		new ConfigLoader().loadConfig(file.toFile());

		assertEquals(4, Cache.warGoalMaxBattles.get(WarGoalType.SUBJUGATE));
		assertEquals(4, Cache.warGoalMaxBattles.get(WarGoalType.TRANSFER_SUBJECT));
	}

	private Path writeConfig(String yaml) throws IOException {
		Path file = tempDir.resolve("config.yml");
		Files.writeString(file, yaml);
		return file;
	}
}
