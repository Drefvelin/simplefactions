package me.Plugins.SimpleFactions.Loaders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import me.Plugins.SimpleFactions.Cache;

class ConfigLoaderBattlePresenceTest {
	private Path tempDir;

	@BeforeEach
	void setUp() throws IOException {
		tempDir = Files.createTempDirectory("sf-config-presence-");
		Cache.battleCaptureMinPlayers = 0;
		Cache.warDevmodePhantomCount = 0;
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
				""");

		new ConfigLoader().loadConfig(file.toFile());

		assertEquals(20, Cache.battleProvincePollIntervalTicks);
		assertFalse(Cache.battleProvinceBlockProtectionEnabled);
	}

	@Test
	void loadConfig_invalidProvincePollIntervalThrows() throws IOException {
		Path file = writeConfig("""
				battle:
				  province_poll_interval_ticks: 0
				""");

		assertThrows(IllegalStateException.class, () -> new ConfigLoader().loadConfig(file.toFile()));
	}

	@Test
	void loadConfig_defaultCaptureMinPlayers() throws IOException {
		Path file = writeConfig("""
				battle:
				  province_poll_interval_ticks: 20
				""");

		new ConfigLoader().loadConfig(file.toFile());

		assertEquals(1, Cache.battleCaptureMinPlayers);
	}

	@Test
	void loadConfig_customCaptureMinPlayers() throws IOException {
		Path file = writeConfig("""
				battle:
				  province_poll_interval_ticks: 20
				  capture_min_players: 2
				""");

		new ConfigLoader().loadConfig(file.toFile());

		assertEquals(2, Cache.battleCaptureMinPlayers);
	}

	@Test
	void loadConfig_invalidCaptureMinPlayersThrows() throws IOException {
		Path file = writeConfig("""
				battle:
				  province_poll_interval_ticks: 20
				  capture_min_players: 0
				""");

		assertThrows(IllegalStateException.class, () -> new ConfigLoader().loadConfig(file.toFile()));
	}

	@Test
	void loadConfig_defaultDevmodePhantomCount() throws IOException {
		Path file = writeConfig("""
				battle:
				  province_poll_interval_ticks: 20
				""");

		new ConfigLoader().loadConfig(file.toFile());

		assertEquals(10, Cache.warDevmodePhantomCount);
	}

	@Test
	void loadConfig_customDevmodePhantomCount() throws IOException {
		Path file = writeConfig("""
				war:
				  devmode:
				    phantom_count: 5
				battle:
				  province_poll_interval_ticks: 20
				""");

		new ConfigLoader().loadConfig(file.toFile());

		assertEquals(5, Cache.warDevmodePhantomCount);
	}

	@Test
	void loadConfig_invalidDevmodePhantomCountThrows() throws IOException {
		Path file = writeConfig("""
				war:
				  devmode:
				    phantom_count: -1
				battle:
				  province_poll_interval_ticks: 20
				""");

		assertThrows(IllegalStateException.class, () -> new ConfigLoader().loadConfig(file.toFile()));
	}

	@Test
	void loadConfig_provinceBlockProtectionEnabled() throws IOException {
		Path file = writeConfig("""
				battle:
				  province_poll_interval_ticks: 20
				  province_block_protection_enabled: true
				""");

		new ConfigLoader().loadConfig(file.toFile());

		assertTrue(Cache.battleProvinceBlockProtectionEnabled);
	}

	private Path writeConfig(String yaml) throws IOException {
		Path file = tempDir.resolve("config.yml");
		Files.writeString(file, yaml);
		return file;
	}
}
