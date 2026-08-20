package me.Plugins.SimpleFactions.War.battle.engine;

import org.bukkit.Location;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.War.battle.enums.BattleType;
import me.Plugins.SimpleFactions.War.battle.template.BattleModeTemplate;
import me.Plugins.SimpleFactions.War.battle.template.BattleTemplate;
import me.Plugins.SimpleFactions.War.battle.template.BattleTemplateService;
import me.Plugins.SimpleFactions.War.battle.template.CapturePointDefinition;
import me.Plugins.SimpleFactions.War.battle.template.TemplateSideConfig;

public final class BattleFactory {
	private BattleFactory() {
	}

	public static Battle createBlank(BattleType type, String battleId) {
		if (type == null) {
			throw new IllegalArgumentException("Battle type is required (field, siege, raid)");
		}
		if (battleId == null || battleId.isBlank()) {
			throw new IllegalArgumentException("Battle id is required");
		}

		Battle battle = new Battle(battleId);
		battle.setBattleType(type);
		battle.setTemplateName(null);
		seedBaseSides(battle);
		return battle;
	}

	public static void applyTemplate(Battle battle, String templateName) {
		validateEditable(battle);
		if (templateName == null || templateName.isBlank()) {
			throw new IllegalArgumentException("Template name is required");
		}
		if (battle.getBattleType() == null) {
			throw new IllegalStateException("Battle type must be set before applying a template");
		}

		BattleTemplate template = BattleTemplateService.getInstance().getTemplate(templateName);
		if (template == null) {
			throw new IllegalArgumentException("Unknown battle template: " + templateName);
		}
		if (template.getType() != battle.getBattleType()) {
			throw new IllegalArgumentException(
					"Template '" + templateName + "' is type " + template.getType().toJson()
							+ ", not " + battle.getBattleType().toJson());
		}

		resetLayout(battle);
		BattleModeTemplate config = BattleTemplateService.getInstance().getModeConfig(templateName);
		battle.setTemplateName(templateName);
		applyModeSettings(battle, config);
		seedTemplateLayout(battle, config);
	}

	public static void resetToBase(Battle battle) {
		validateEditable(battle);
		resetLayout(battle);
		battle.setTemplateName(null);
		seedBaseSides(battle);
	}

	public static void applyCampaignDefault(Battle battle) {
		if (battle == null || battle.getBattleType() == null) {
			return;
		}
		String name = switch (battle.getBattleType()) {
			case FIELD -> Cache.battleCampaignTemplateField;
			case SIEGE -> Cache.battleCampaignTemplateSiege;
			case RAID -> Cache.battleCampaignTemplateRaid;
		};
		if (name != null && !name.isBlank()) {
			applyTemplate(battle, name);
		}
	}

	public static void resetLayout(Battle battle) {
		battle.clearSides();
		battle.clearPoints();
		battle.clearTemplateMetadata();
		battle.resetBaseSettings();
	}

	private static void validateEditable(Battle battle) {
		if (battle == null) {
			throw new IllegalArgumentException("Battle is required");
		}
		if (battle.hasStarted()) {
			throw new IllegalStateException("Cannot change template while battle has started");
		}
	}

	private static void seedBaseSides(Battle battle) {
		BattleSide attacker = new BattleSide(BattleTemplate.ATTACKER_SIDE, battle.getLifeType(), battle.getLives());
		BattleSide defender = new BattleSide(BattleTemplate.DEFENDER_SIDE, battle.getLifeType(), battle.getLives());
		battle.addSide(attacker);
		battle.addSide(defender);
	}

	private static void applyModeSettings(Battle battle, BattleModeTemplate config) {
		battle.setFriendlyFire(config.getFriendlyFire());
		battle.setKeepInventory(config.getKeepInventory());
		battle.setLifeType(config.getLifeType());
		battle.setLives(config.getLives());
	}

	private static void seedTemplateLayout(Battle battle, BattleModeTemplate config) {
		BattleSide attacker = createSide(BattleTemplate.ATTACKER_SIDE, config.getAttacker(), config);
		BattleSide defender = createSide(BattleTemplate.DEFENDER_SIDE, config.getDefender(), config);
		battle.addSide(attacker);
		battle.addSide(defender);

		BattleType type = battle.getBattleType();
		if (type == BattleType.FIELD) {
			seedFieldPoints(battle, config, defender);
		} else if (type == BattleType.SIEGE) {
			seedSiegeMetadata(battle, config);
		} else if (type == BattleType.RAID) {
			seedRaidMetadata(battle, config);
		}

		seedNavalMetadata(battle, config, type);
	}

	private static BattleSide createSide(String sideId, TemplateSideConfig sideConfig, BattleModeTemplate config) {
		BattleSide side = new BattleSide(sideId, config.getLifeType(), config.getLives());
		if (sideConfig != null) {
			if (sideConfig.getSpawn() != null) {
				side.setSpawn(sideConfig.getSpawn().toBukkitLocation());
			}
			if (sideConfig.getJail() != null) {
				side.setJail(sideConfig.getJail().toBukkitLocation());
			}
		}
		return side;
	}

	private static void seedFieldPoints(Battle battle, BattleModeTemplate config, BattleSide defaultController) {
		if (config.getCapturePoints() == null) {
			return;
		}
		int sequence = 0;
		for (CapturePointDefinition pointDef : config.getCapturePoints()) {
			if (pointDef == null || pointDef.getLocation() == null) {
				continue;
			}
			Location location = pointDef.getLocation().toBukkitLocation();
			if (location == null) {
				continue;
			}
			CapturePoint point = new CapturePoint(pointDef.getId(), location, defaultController, 100);
			point.setSequenceIndex(sequence++);
			point.setAdvanceSideId(BattleTemplate.ATTACKER_SIDE);
			battle.addPoint(point);
		}
	}

	private static void seedSiegeMetadata(Battle battle, BattleModeTemplate config) {
		battle.setContestArea(config.getContestArea());
		battle.setContestDurationSeconds(config.getContestDurationSeconds());
	}

	private static void seedRaidMetadata(Battle battle, BattleModeTemplate config) {
		battle.setDefenderRespawnMode(config.getDefenderRespawnMode());
		battle.setDefenderLives(config.getDefenderLives());
		battle.setRaidTarget(config.getRaidTarget());
	}

	private static void seedNavalMetadata(Battle battle, BattleModeTemplate config, BattleType type) {
		if (!config.isNavalVariant() || (type != BattleType.FIELD && type != BattleType.SIEGE)) {
			return;
		}
		battle.setNavalVariant(true);
		TemplateSideConfig navalSpawn = config.getNavalSpawn();
		if (navalSpawn != null && navalSpawn.getSpawn() != null) {
			battle.setNavalSpawn(navalSpawn.getSpawn().toBukkitLocation());
		}
	}
}
