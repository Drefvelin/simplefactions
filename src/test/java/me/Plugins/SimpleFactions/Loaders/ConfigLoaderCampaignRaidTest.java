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

class ConfigLoaderCampaignRaidTest {
	private Path tempDir;

	@BeforeEach
	void setUp() throws IOException {
		tempDir = Files.createTempDirectory("sf-config-");
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
	void loadConfig_campaignRaidDefaults() throws IOException {
		Path file = writeConfig("""
				war:
				  battle_schedule:
				    vote_close_hour: 16
				    raid_window_start_hour: 19
				    raid_window_end_hour: 20
				    window_start_hour: 21
				    window_end_hour: 24
				    defender_choice_deadline_hour: 12
				  battle_voting:
				    min_players: 4
				""");

		new ConfigLoader().loadConfig(file.toFile());

		assertEquals(60, Cache.campaignRaidMusterSeconds);
		assertEquals(600, Cache.campaignRaidDurationSeconds);
		assertEquals(48, Cache.campaignRaidRepairLockHours);
	}

	@Test
	void loadConfig_customCampaignRaidValues() throws IOException {
		Path file = writeConfig("""
				war:
				  battle_schedule:
				    vote_close_hour: 16
				    raid_window_start_hour: 19
				    raid_window_end_hour: 20
				    window_start_hour: 21
				    window_end_hour: 24
				    defender_choice_deadline_hour: 12
				  campaign_raid:
				    muster_seconds: 30
				    duration_seconds: 300
				    repair_lock_hours: 24
				  battle_voting:
				    min_players: 4
				""");

		new ConfigLoader().loadConfig(file.toFile());

		assertEquals(30, Cache.campaignRaidMusterSeconds);
		assertEquals(300, Cache.campaignRaidDurationSeconds);
		assertEquals(24, Cache.campaignRaidRepairLockHours);
	}

	@Test
	void loadConfig_invalidCampaignRaidMusterThrows() {
		assertThrows(IllegalStateException.class, () -> writeAndLoad("""
				war:
				  battle_schedule:
				    vote_close_hour: 16
				    raid_window_start_hour: 19
				    raid_window_end_hour: 20
				    window_start_hour: 21
				    window_end_hour: 24
				    defender_choice_deadline_hour: 12
				  campaign_raid:
				    muster_seconds: 0
				  battle_voting:
				    min_players: 4
				"""));
	}

	private void writeAndLoad(String yaml) throws IOException {
		Path file = writeConfig(yaml);
		new ConfigLoader().loadConfig(file.toFile());
	}

	private Path writeConfig(String yaml) throws IOException {
		Path file = tempDir.resolve("config.yml");
		Files.writeString(file, yaml);
		return file;
	}
}
