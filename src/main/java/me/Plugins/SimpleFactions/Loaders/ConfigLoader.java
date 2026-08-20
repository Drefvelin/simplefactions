package me.Plugins.SimpleFactions.Loaders;

import java.io.File;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.War.battle.enums.DefenderRespawnMode;
import me.Plugins.SimpleFactions.enums.Scope;
import me.Plugins.SimpleFactions.enums.Terrain;
import me.Plugins.SimpleFactions.laws.LawEffect;

public class ConfigLoader {
	public void loadConfig(File configFile) {
		FileConfiguration config = new YamlConfiguration();
        try {
        	config.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
		Cache.mapRef = config.getString("map-reference", "main");
		Cache.worldName = config.getString("world-name", "TFMC_Map");

		Cache.maxMembers = config.getInt("max-members", 64);
		Cache.maxWealthPrestige = config.getInt("max-prestige-from-wealth", 1000);
		Cache.bankBlock = config.getString("bank-block", "v.lodestone");
		Cache.maxExtraNodeCapacity = config.getInt("max-extra-node-capacity", 0);
		
		Cache.deJureRequirement = config.getDouble("de-jure-requirement", 100.0);
		Cache.maxUntitledProvinces = config.getInt("max-untitled-provinces", 5);
		Cache.maxFreeTitles = config.getInt("max-free-titles", 3);
		Cache.mapEnabled = config.getBoolean("enable-map", false);

		Cache.provinceCost = config.getInt("province-cost", 50);

		Cache.settlementLargePopulationThreshold = config.getInt("settlement-large-population-threshold", 8);
		Cache.portSeaProximityBlocks = config.getInt("port-sea-proximity-blocks", 20);

		Cache.warRequireDeclareCode = config.getBoolean("war.require_declare_code", false);
		Cache.warDeclareOpinionThreshold = config.getInt("war.declare_opinion_threshold", -50);
		Cache.warInitiativePerSide = config.getInt("war.initiative_per_side", 4);
		Cache.warFirstBattleAtBorder = config.getBoolean("war.battle_cadence.first_battle_at_border", true);
		Cache.warProvincesBetweenBattles = config.getInt("war.battle_cadence.provinces_between_battles", 1);
		Cache.warOccupationIncludeEnemyNeighbors = config.getBoolean("war.occupation.include_enemy_neighbors", true);
		Cache.warDeclinedAllyStabilityPenalty = config.getInt("war.declined_ally_stability_penalty", -30);
		Cache.warPathfinderNeutralPenalty = config.getDouble("war.pathfinder.neutral_penalty", 8.0);
		Cache.warPathfinderSeaPassEnabled = config.getBoolean("war.pathfinder.sea_pass_enabled", true);
		Cache.warPathfinderWaterCost = config.getDouble("war.pathfinder.water_cost", 0.0);

		Cache.warBattleWindowStartHour = config.getInt("war.battle_schedule.window_start_hour", 20);
		Cache.warBattleWindowEndHour = config.getInt("war.battle_schedule.window_end_hour", 24);
		Cache.warVoteCloseHour = config.getInt("war.battle_schedule.vote_close_hour", 16);
		Cache.warDefenderChoiceDeadlineHour = config.getInt("war.battle_schedule.defender_choice_deadline_hour", 12);
		Cache.warOneBattlePerDay = config.getBoolean("war.battle_schedule.one_battle_per_day", true);
		Cache.warFirstBattleDayAfterDeclare = config.getBoolean("war.battle_schedule.first_battle_day_after_declare", true);
		Cache.warBattleVotingMinPlayers = config.getInt("war.battle_voting.min_players", 4);
		Cache.warBattleVotingRequireSmallestSideFull = config.getBoolean("war.battle_voting.require_smallest_side_full", true);
		Cache.warBattleVotingPassIfEither = config.getBoolean("war.battle_voting.pass_if_either", true);
		Cache.warBattleVotingDevMinPlayersEnabled = config.contains("war.battle_voting.dev_min_players");
		if (Cache.warBattleVotingDevMinPlayersEnabled) {
			Cache.warBattleVotingDevMinPlayers = config.getInt("war.battle_voting.dev_min_players");
		} else {
			Cache.warBattleVotingDevMinPlayers = Cache.warBattleVotingMinPlayers;
		}
		validateBattleScheduleConfig(config);

		Cache.battleProvincePollIntervalTicks = config.getInt("battle.province_poll_interval_ticks", 20);
		Cache.battleProvinceLeaveCountdownSeconds = config.getInt("battle.province_leave_countdown_seconds", 10);
		Cache.battleSiegeContestDurationSeconds = config.getInt("battle.siege.contest_duration_seconds", 180);
		Cache.battleRaidDefenderRespawnModeDefault = DefenderRespawnMode.fromJson(
				config.getString("battle.raid.defender_respawn_mode_default", "INFINITE"));
		if (Cache.battleRaidDefenderRespawnModeDefault == null) {
			Cache.battleRaidDefenderRespawnModeDefault = DefenderRespawnMode.INFINITE;
		}
		Cache.battleCampaignTemplateField = config.getString("battle.campaign_template.field", "field_default");
		Cache.battleCampaignTemplateSiege = config.getString("battle.campaign_template.siege", "siege_default");
		Cache.battleCampaignTemplateRaid = config.getString("battle.campaign_template.raid", "raid_template");
		validateBattlePresenceConfig();
		validateBattleTemplateDefaultsConfig();

		Cache.branchUpgradeCost = config.getDouble("branch-upgrade-cost", 100.0);
		Cache.branchUpgradeExponent = config.getDouble("branch-upgrade-exponent", 1.1);
		Cache.votingBlock = config.getString("voting-block", "v(chiseled_bookshelf)");
		Cache.baseYear = config.getString("starting-year", "372 AE");

		if(config.contains("terrain-modifiers")) {
			for(String s : config.getStringList("terrain-modifiers")) {
				String[] args = s.split("\\s+");
				if(args.length != 2) continue;
				try {
					Cache.tradeCarry.put(Terrain.valueOf(args[0].toUpperCase()), Double.parseDouble(args[1]));
				} catch (Exception e) {
					// TODO: handle exception
					Bukkit.getLogger().info("Could not parse "+s);
				}
			}
		}
		
		if(config.contains("icons")) {
			for(String s : config.getStringList("icons")) {
				String id = s.split("\\(")[0];
				String path = s.split("\\(")[1].replace(")", "");
				Cache.icons.put(id, path);
			}
		}

		if(config.contains("base-effects")) {
            for(String s : config.getConfigurationSection("base-effects").getKeys(false)) {
                try {
                    Scope scope = Scope.valueOf(s.toUpperCase());
                    Cache.baseEffects.put(scope, new LawEffect(scope, config.getConfigurationSection("base-effects."+s)));
                } catch (Exception e) {
                    Bukkit.getLogger().info("[SimpleFactions] could not parse modifier for scope "+s);
                    // TODO: handle exception
                }
            }
        }

		if (!config.isConfigurationSection("installations")) {
			throw new IllegalStateException("config.yml missing installations section");
		}
		InstallationConfigLoader.load(config.getConfigurationSection("installations"));
	}

	private static void validateBattleScheduleConfig(FileConfiguration config) {
		int defenderDeadline = Cache.warDefenderChoiceDeadlineHour;
		int voteClose = Cache.warVoteCloseHour;
		int windowStart = Cache.warBattleWindowStartHour;
		int windowEnd = Cache.warBattleWindowEndHour;

		if (defenderDeadline < 0 || defenderDeadline >= voteClose) {
			failBattleSchedule("war.battle_schedule.defender_choice_deadline_hour must be >= 0 and < vote_close_hour");
		}
		if (voteClose >= windowStart) {
			failBattleSchedule("war.battle_schedule.vote_close_hour must be < window_start_hour");
		}
		if (windowStart > windowEnd || windowEnd > 24) {
			failBattleSchedule("war.battle_schedule requires window_start_hour <= window_end_hour <= 24");
		}
		if (Cache.warBattleVotingMinPlayers < 1) {
			failBattleSchedule("war.battle_voting.min_players must be >= 1");
		}
		if (Cache.warBattleVotingDevMinPlayersEnabled && Cache.warBattleVotingDevMinPlayers < 1) {
			failBattleSchedule("war.battle_voting.dev_min_players must be >= 1");
		}
	}

	private static void failBattleSchedule(String message) {
		if (Bukkit.getServer() != null) {
			Bukkit.getLogger().severe("[SimpleFactions] " + message);
		}
		throw new IllegalStateException(message);
	}

	private static void validateBattlePresenceConfig() {
		if (Cache.battleProvincePollIntervalTicks < 1) {
			failBattleSchedule("battle.province_poll_interval_ticks must be >= 1");
		}
	}

	private static void validateBattleTemplateDefaultsConfig() {
		if (Cache.battleProvinceLeaveCountdownSeconds < 1) {
			failBattleSchedule("battle.province_leave_countdown_seconds must be >= 1");
		}
		if (Cache.battleSiegeContestDurationSeconds < 1) {
			failBattleSchedule("battle.siege.contest_duration_seconds must be >= 1");
		}
		if (Cache.battleRaidDefenderRespawnModeDefault == null) {
			failBattleSchedule("battle.raid.defender_respawn_mode_default must be INFINITE or LIVES");
		}
	}
}
