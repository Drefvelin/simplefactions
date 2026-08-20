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

class ConfigLoaderBattleScheduleTest {
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
	void loadConfig_validBattleScheduleHours() throws IOException {
		Path file = writeConfig("""
				war:
				  battle_schedule:
				    window_start_hour: 20
				    window_end_hour: 24
				    vote_close_hour: 16
				    defender_choice_deadline_hour: 12
				  battle_voting:
				    min_players: 4
				    dev_min_players: 1
				""" + INSTALLATIONS);

		new ConfigLoader().loadConfig(file.toFile());

		assertEquals(20, Cache.warBattleWindowStartHour);
		assertEquals(24, Cache.warBattleWindowEndHour);
		assertEquals(16, Cache.warVoteCloseHour);
		assertEquals(12, Cache.warDefenderChoiceDeadlineHour);
		assertEquals(4, Cache.warBattleVotingMinPlayers);
		assertEquals(1, Cache.warBattleVotingDevMinPlayers);
	}

	@Test
	void loadConfig_invalidHourOrderThrows() throws IOException {
		Path file = writeConfig("""
				war:
				  battle_schedule:
				    window_start_hour: 20
				    window_end_hour: 24
				    vote_close_hour: 20
				    defender_choice_deadline_hour: 12
				  battle_voting:
				    min_players: 4
				    dev_min_players: 1
				""" + INSTALLATIONS);

		assertThrows(IllegalStateException.class, () -> new ConfigLoader().loadConfig(file.toFile()));
	}

	private Path writeConfig(String yaml) throws IOException {
		Path file = tempDir.resolve("config.yml");
		Files.writeString(file, yaml);
		return file;
	}
}
