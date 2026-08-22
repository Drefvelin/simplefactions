package me.Plugins.SimpleFactions.War.validation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.Plugins.SimpleFactions.Loaders.TitleLoader;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.RelationManager;
import me.Plugins.SimpleFactions.Managers.TitleManager;
import me.Plugins.SimpleFactions.Managers.WarManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Tiers.Title;
import me.Plugins.SimpleFactions.War.Side;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.enums.WarGoalType;

/**
 * Validates war declare requests before {@code WarManager.addWar}.
 * <p>
 * Step 56.05 wiring example:
 * <pre>
 * WarValidationResult result = new WarGoalValidator().validate(request);
 * if (!result.isValid()) {
 *   player.sendMessage(result.getMessage());
 *   return;
 * }
 * War w = WarManager.addWar(...);
 * </pre>
 */
public class WarGoalValidator {

	public WarValidationResult validate(WarDeclareRequest request) {
		WarValidationResult shared = validateShared(request);
		if (!shared.isValid()) {
			return shared;
		}

		return switch (request.getGoal()) {
			case DE_JURE_ANNEX -> validateDeJureAnnex(request);
			case SUBJUGATE -> validateSubjugate(request);
			case TRANSFER_SUBJECT -> validateTransferSubject(request);
		};
	}

	private WarValidationResult validateShared(WarDeclareRequest request) {
		if (request.getAttacker().getId().equalsIgnoreCase(request.getDefender().getId())) {
			return WarValidationResult.fail("§cYou cannot declare war on your own faction.");
		}
		War sharedWar = WarManager.findSharedActiveWar(request.getAttacker(), request.getDefender());
		if (sharedWar != null) {
			Side attackerSide = sharedWar.getSide(request.getAttacker());
			Side defenderSide = sharedWar.getSide(request.getDefender());
			if (attackerSide != null && attackerSide.equals(defenderSide)) {
				return WarValidationResult.fail("§cYou are already allied with that faction in an active war.");
			}
			return WarValidationResult.fail("§cYou are already at war with that faction.");
		}
		return WarValidationResult.ok();
	}

	private WarValidationResult validateDeJureAnnex(WarDeclareRequest request) {
		String targetTitleId = request.getTargetTitleId();
		if (targetTitleId == null || targetTitleId.isBlank()) {
			return WarValidationResult.fail("§cSpecify a de jure title to annex.");
		}

		Title title = TitleLoader.getById(targetTitleId);
		if (title == null) {
			return WarValidationResult.fail("§cThat title does not exist.");
		}

		Faction attacker = request.getAttacker();
		Faction defender = request.getDefender();

		if (!canAnnexByRank(attacker.getTier().getTier(), title.getTier().getTier())) {
			return WarValidationResult.fail("§cYou cannot de jure annex a title above your rank.");
		}

		Faction owner = TitleManager.getOwner(title);
		if (owner == null || !owner.getId().equalsIgnoreCase(defender.getId())) {
			return WarValidationResult.fail("§cThat title is not held by the defender.");
		}

		List<Integer> titleProvinces = TitleManager.getProvinces(title);
		int ownedInTitle = title.nestedProvinceCheck(TitleManager.getProvinces(attacker), titleProvinces);
		if (ownedInTitle < 1) {
			return WarValidationResult.fail("§cYou do not partially control this title.");
		}
		if (title.canBeHeld(attacker)) {
			return WarValidationResult.fail("§cYou already fully hold this title.");
		}

		if (titleProvincesContainSettlement(new HashSet<>(titleProvinces), collectSettlementProbes(), collectCapitals())) {
			return WarValidationResult.fail("§cThis title has settlements - use subjugate instead.");
		}

		return WarValidationResult.ok();
	}

	private WarValidationResult validateSubjugate(WarDeclareRequest request) {
		Faction attacker = request.getAttacker();
		Faction defender = request.getDefender();

		if (!FactionManager.factions.contains(defender)) {
			return WarValidationResult.fail("§cInvalid war target.");
		}

		String overlord = RelationManager.getOverlord(defender);
		if (overlord != null && overlord.equalsIgnoreCase(attacker.getId())) {
			return WarValidationResult.fail("§cThat faction is already your subject.");
		}

		return WarValidationResult.ok();
	}

	private WarValidationResult validateTransferSubject(WarDeclareRequest request) {
		String subjectFactionId = request.getSubjectFactionId();
		if (subjectFactionId == null || subjectFactionId.isBlank()) {
			return WarValidationResult.fail("§cSpecify a valid subject faction.");
		}

		Faction subject = FactionManager.getByString(subjectFactionId);
		if (subject == null) {
			return WarValidationResult.fail("§cSpecify a valid subject faction.");
		}

		Faction attacker = request.getAttacker();
		Faction defender = request.getDefender();

		if (subject.getId().equalsIgnoreCase(attacker.getId())) {
			return WarValidationResult.fail("§cYou cannot transfer your own faction as a subject.");
		}

		String overlord = RelationManager.getOverlord(subject);
		if (overlord == null) {
			return WarValidationResult.fail("§cThat faction is not a subject of the defender.");
		}
		if (!overlord.equalsIgnoreCase(defender.getId())) {
			return WarValidationResult.fail("§cThat faction is not a subject of the defender.");
		}
		if (overlord.equalsIgnoreCase(attacker.getId())) {
			return WarValidationResult.fail("§cThat faction is already your subject.");
		}

		return WarValidationResult.ok();
	}

	static boolean canAnnexByRank(int attackerTierLevel, int titleTierLevel) {
		return attackerTierLevel >= titleTierLevel;
	}

	static boolean titleProvincesContainSettlement(
			Set<Integer> titleProvinces,
			List<SettlementProbe> settlements,
			List<Integer> capitals) {
		for (SettlementProbe settlement : settlements) {
			if (titleProvinces.contains(settlement.centerProvince())) {
				return true;
			}
		}
		for (Integer capital : capitals) {
			if (capital != null && capital > 0 && titleProvinces.contains(capital)) {
				return true;
			}
		}
		return false;
	}

	record SettlementProbe(int centerProvince) {}

	private List<SettlementProbe> collectSettlementProbes() {
		return FactionManager.factions.stream()
				.flatMap(f -> f.getSettlementHandler().getAll().stream())
				.map(s -> new SettlementProbe(s.getCenterProvince()))
				.toList();
	}

	private List<Integer> collectCapitals() {
		return FactionManager.factions.stream()
				.map(Faction::getCapital)
				.toList();
	}
}
