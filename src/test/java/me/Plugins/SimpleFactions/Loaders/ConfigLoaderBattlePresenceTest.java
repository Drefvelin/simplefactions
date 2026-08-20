package me.Plugins.SimpleFactions.Loaders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import me.Plugins.SimpleFactions.Cache;

class ConfigLoaderBattlePresenceTest {
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
		tempDir = Files.createTempDirectory("sf-config-presence-");
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
	void loadConfig_defaultProvincePollInterval() throws IOException {
		Path file = writeConfig("""
				battle:
				  province_poll_interval_ticks: 20
				""" + INSTALLATIONS);

		new ConfigLoader().loadConfig(file.toFile());

		assertEquals(20, Cache.battleProvincePollIntervalTicks);
	}

	@Test
	void loadConfig_invalidProvincePollIntervalThrows() throws IOException {
		Path file = writeConfig("""
				battle:
				  province_poll_interval_ticks: 0
				""" + INSTALLATIONS);

		assertThrows(IllegalStateException.class, () -> new ConfigLoader().loadConfig(file.toFile()));
	}

	private Path writeConfig(String yaml) throws IOException {
		Path file = tempDir.resolve("config.yml");
		Files.writeString(file, yaml);
		return file;
	}
}
