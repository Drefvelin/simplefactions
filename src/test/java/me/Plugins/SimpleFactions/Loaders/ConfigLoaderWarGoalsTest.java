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
	private static final String INSTALLATIONS = """
			installations:
			  fort:
			    daily-upkeep: 50
			    construction-time: 10
			    slots:
			      static_emplacement: 8
			  port:
			    daily-upkeep: 20
			    construction-time: 10
			    slots:
			      ship: 10
			  airport:
			    daily-upkeep: 35
			    construction-time: 10
			    slots:
			      aircraft: 10
			""";

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
				""" + INSTALLATIONS);

		new ConfigLoader().loadConfig(file.toFile());

		assertEquals(2.0, Cache.warInitiativeFactor);
		assertEquals(5, Cache.warGoalMaxBattles.get(WarGoalType.SUBJUGATE));
		assertEquals(3, Cache.warGoalMaxBattles.get(WarGoalType.DE_JURE_ANNEX));
		assertEquals(4, Cache.warGoalMaxBattles.get(WarGoalType.TRANSFER_SUBJECT));
	}

	private Path writeConfig(String yaml) throws IOException {
		Path file = tempDir.resolve("config.yml");
		Files.writeString(file, yaml);
		return file;
	}
}
