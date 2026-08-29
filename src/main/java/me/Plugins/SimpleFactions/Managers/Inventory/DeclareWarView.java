package me.Plugins.SimpleFactions.Managers.Inventory;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import me.Plugins.SimpleFactions.Loaders.RelationLoader;
import me.Plugins.SimpleFactions.Diplomacy.RelationType;
import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.Holder.DeclareWarHolder;
import me.Plugins.SimpleFactions.Managers.InventoryManager;
import me.Plugins.SimpleFactions.Managers.WarManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.core.War;
import me.Plugins.SimpleFactions.War.core.WarDeclareHelper;
import me.Plugins.SimpleFactions.War.enums.WarGoalType;
import me.Plugins.SimpleFactions.War.declare.ChangeGovernmentEligibility;
import me.Plugins.SimpleFactions.War.declare.DeJureAnnexEligibility.DeJureTitleOption;
import me.Plugins.SimpleFactions.War.declare.PillageEligibility;
import me.Plugins.SimpleFactions.War.declare.WarDeclareRequest;
import me.Plugins.SimpleFactions.War.declare.WarGoalValidator;
import me.Plugins.SimpleFactions.War.declare.WarValidationResult;
import me.Plugins.SimpleFactions.enums.SFGUI;
import me.Plugins.SimpleFactions.laws.Law;
import me.Plugins.SimpleFactions.laws.LawGroup;

public class DeclareWarView {
	public InventoryManager inv;
	public DeclareWarCreator creator = new DeclareWarCreator();

	public DeclareWarView(InventoryManager inv) {
		this.inv = inv;
	}

	public void openGoalPicker(Player player, Faction attacker, Faction defender) {
		DeclareWarHolder holder = new DeclareWarHolder(attacker.getId(), defender.getId(), SFGUI.WAR_DECLARE_GOAL);
		Inventory inventory = SimpleFactions.plugin.getServer().createInventory(holder, 27, "§7Declare War - Goal");
		int slot = 10;
		inventory.setItem(slot++, creator.createWarGoalItem());
		inventory.setItem(slot++, creator.createTributaryGoalItem());
		if (attacker.canHaveVassals()) {
			inventory.setItem(slot++, creator.createSubjugateGoalItem());
		}
		inventory.setItem(slot++, creator.createOpenMarketGoalItem());
		inventory.setItem(slot++, creator.createChangeGovernmentGoalItem());
		inventory.setItem(slot++, creator.createPillageGoalItem());
		if (WarDeclareHelper.canDeclareUsurp(attacker, defender)) {
			inventory.setItem(slot++, creator.createUsurpGoalItem());
		}
		if (!WarDeclareHelper.deJureTitleOptions(attacker, defender).isEmpty()) {
			inventory.setItem(slot++, creator.createDeJureGoalItem());
		}
		if (attacker.canHaveVassals() && !WarDeclareHelper.defenderSubjects(defender).isEmpty()) {
			inventory.setItem(slot, creator.createTransferSubjectGoalItem());
		}
		inventory.setItem(26, inv.createBackButton(SFGUI.WAR_DECLARE_GOAL));
		player.openInventory(inventory);
	}

	public void openRelationTypePicker(Player player, Faction attacker, Faction defender) {
		DeclareWarHolder holder = new DeclareWarHolder(attacker.getId(), defender.getId(), SFGUI.WAR_DECLARE_RELATION_TYPE);
		Inventory inventory = SimpleFactions.plugin.getServer().createInventory(holder, 27, "§7Declare War - Type");
		int slot = 0;
		for (RelationType type : RelationLoader.getWarPickableVassalTypes()) {
			if (slot >= 26) break;
			inventory.setItem(slot++, creator.createRelationTypeItem(type, attacker));
		}
		inventory.setItem(26, inv.createBackButton(SFGUI.WAR_DECLARE_RELATION_TYPE));
		player.openInventory(inventory);
	}

	public void openTitlePicker(Player player, Faction attacker, Faction defender) {
		DeclareWarHolder holder = new DeclareWarHolder(attacker.getId(), defender.getId(), SFGUI.WAR_DECLARE_TITLE);
		Inventory inventory = SimpleFactions.plugin.getServer().createInventory(holder, 27, "§7Declare War - Title");
		int slot = 0;
		for (DeJureTitleOption option : WarDeclareHelper.deJureTitleOptions(attacker, defender)) {
			if (slot >= 26) break;
			inventory.setItem(slot++, creator.createTitleItem(option));
		}
		inventory.setItem(26, inv.createBackButton(SFGUI.WAR_DECLARE_TITLE));
		player.openInventory(inventory);
	}

	public void openSubjectPicker(Player player, Faction attacker, Faction defender) {
		DeclareWarHolder holder = new DeclareWarHolder(attacker.getId(), defender.getId(), SFGUI.WAR_DECLARE_SUBJECT);
		Inventory inventory = SimpleFactions.plugin.getServer().createInventory(holder, 27, "§7Declare War - Subject");
		int slot = 0;
		for (Faction subject : WarDeclareHelper.defenderSubjects(defender)) {
			if (slot >= 26) break;
			inventory.setItem(slot++, creator.createSubjectItem(subject));
		}
		inventory.setItem(26, inv.createBackButton(SFGUI.WAR_DECLARE_SUBJECT));
		player.openInventory(inventory);
	}

	public void openSettlementPicker(Player player, Faction attacker, Faction defender) {
		DeclareWarHolder holder = new DeclareWarHolder(attacker.getId(), defender.getId(), SFGUI.WAR_DECLARE_SETTLEMENT);
		Inventory inventory = SimpleFactions.plugin.getServer().createInventory(holder, 27, "§7Declare War - Settlement");
		int slot = 0;
		for (PillageEligibility.PillageSettlementOption option : PillageEligibility.options(attacker, defender)) {
			if (slot >= 26) break;
			inventory.setItem(slot++, creator.createSettlementItem(option));
		}
		inventory.setItem(26, inv.createBackButton(SFGUI.WAR_DECLARE_SETTLEMENT));
		player.openInventory(inventory);
	}

	public void openGovernmentPicker(
			Player player,
			Faction attacker,
			Faction defender,
			String governmentLawId,
			String leadershipLawId) {
		String govId = governmentLawId != null
				? governmentLawId
				: ChangeGovernmentEligibility.currentLawId(defender, ChangeGovernmentEligibility.GOVERNMENT_GROUP);
		String leadId = leadershipLawId != null
				? leadershipLawId
				: ChangeGovernmentEligibility.currentLawId(defender, ChangeGovernmentEligibility.LEADERSHIP_GROUP);
		DeclareWarHolder holder = new DeclareWarHolder(
				attacker.getId(), defender.getId(), SFGUI.WAR_DECLARE_GOVERNMENT, govId, leadId);
		Inventory inventory = SimpleFactions.plugin.getServer().createInventory(holder, 27, "§7Declare War - Government");
		LawGroup government = ChangeGovernmentEligibility.group(defender, ChangeGovernmentEligibility.GOVERNMENT_GROUP);
		if (government != null) {
			inventory.setItem(11, creator.createGovernmentAxisItem(government, lawInGroup(government, govId)));
		}
		LawGroup leadership = ChangeGovernmentEligibility.group(defender, ChangeGovernmentEligibility.LEADERSHIP_GROUP);
		if (leadership != null) {
			inventory.setItem(15, creator.createGovernmentAxisItem(leadership, lawInGroup(leadership, leadId)));
		}
		if (!ChangeGovernmentEligibility.combinationEqualsCurrent(defender, govId, leadId)) {
			inventory.setItem(22, creator.createGovernmentConfirmItem());
		}
		inventory.setItem(26, inv.createBackButton(SFGUI.WAR_DECLARE_GOVERNMENT));
		player.openInventory(inventory);
	}

	public void click(InventoryClickEvent e, Inventory inventory, Player player) {
		if (!(inventory.getHolder() instanceof DeclareWarHolder holder)) return;
		e.setCancelled(true);
		ItemStack item = e.getCurrentItem();
		if (item == null || item.getType().equals(Material.AIR)) return;

		Faction attacker = FactionManager.getByString(holder.getAttackerId());
		Faction defender = FactionManager.getByString(holder.getDefenderId());
		if (attacker == null || defender == null) return;

		if (item.getType().equals(Material.BARRIER)) {
			player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
			if (holder.getStep() == SFGUI.WAR_DECLARE_GOAL) {
				inv.diplomacyView(null, player, defender, true);
			} else if (holder.getStep() == SFGUI.WAR_DECLARE_GOVERNMENT_LAW) {
				openGovernmentPicker(
						player, attacker, defender, holder.getGovernmentLawId(), holder.getLeadershipLawId());
			} else {
				openGoalPicker(player, attacker, defender);
			}
			return;
		}

		if (holder.getStep() == SFGUI.WAR_DECLARE_GOAL) {
			NamespacedKey goalKey = new NamespacedKey(SimpleFactions.plugin, "goal");
			String goalId = item.getItemMeta().getPersistentDataContainer().get(goalKey, PersistentDataType.STRING);
			WarGoalType goal = WarGoalType.fromJson(goalId);
			if (goal == null) return;
			player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
			switch (goal) {
				case WAR, TRIBUTARY, USURP, OPEN_MARKET ->
						openConfirm(player, attacker, defender, goal, null, null, null, null, null, null);
				case CHANGE_GOVERNMENT -> openGovernmentPicker(player, attacker, defender, null, null);
				case SUBJUGATE -> openRelationTypePicker(player, attacker, defender);
				case DE_JURE_ANNEX -> openTitlePicker(player, attacker, defender);
				case TRANSFER_SUBJECT -> openSubjectPicker(player, attacker, defender);
				case PILLAGE -> openSettlementPicker(player, attacker, defender);
				case OVERTHROW, CHANGE_LAW, CHANGE_TAX -> {
				}
			}
			return;
		}

		NamespacedKey idKey = new NamespacedKey(SimpleFactions.plugin, "id");
		String id = item.getItemMeta().getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
		if (id == null) return;
		player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
		if (holder.getStep() == SFGUI.WAR_DECLARE_TITLE) {
			NamespacedKey eligibleKey = new NamespacedKey(SimpleFactions.plugin, "eligible");
			String eligible = item.getItemMeta().getPersistentDataContainer().get(eligibleKey, PersistentDataType.STRING);
			if (!"true".equals(eligible)) {
				return;
			}
			openConfirm(player, attacker, defender, WarGoalType.DE_JURE_ANNEX, id, null, null, null, null, null);
		} else if (holder.getStep() == SFGUI.WAR_DECLARE_SETTLEMENT) {
			NamespacedKey eligibleKey = new NamespacedKey(SimpleFactions.plugin, "eligible");
			String eligible = item.getItemMeta().getPersistentDataContainer().get(eligibleKey, PersistentDataType.STRING);
			if (!"true".equals(eligible)) {
				return;
			}
			openConfirm(player, attacker, defender, WarGoalType.PILLAGE, null, null, null, null, null, id);
		} else if (holder.getStep() == SFGUI.WAR_DECLARE_SUBJECT) {
			openConfirm(player, attacker, defender, WarGoalType.TRANSFER_SUBJECT, null, id, null, null, null, null);
		} else if (holder.getStep() == SFGUI.WAR_DECLARE_RELATION_TYPE) {
			openConfirm(player, attacker, defender, WarGoalType.SUBJUGATE, null, null, id, null, null, null);
		} else if (holder.getStep() == SFGUI.WAR_DECLARE_GOVERNMENT) {
			if ("confirm".equals(id)) {
				openConfirm(
						player,
						attacker,
						defender,
						WarGoalType.CHANGE_GOVERNMENT,
						null,
						null,
						null,
						holder.getGovernmentLawId(),
						holder.getLeadershipLawId(),
						null);
				return;
			}
			openGovernmentLawPicker(
					player, attacker, defender, holder.getGovernmentLawId(), holder.getLeadershipLawId(), id);
		} else if (holder.getStep() == SFGUI.WAR_DECLARE_GOVERNMENT_LAW) {
			String govId = holder.getGovernmentLawId();
			String leadId = holder.getLeadershipLawId();
			if (ChangeGovernmentEligibility.GOVERNMENT_GROUP.equalsIgnoreCase(holder.getPickingGroupId())) {
				govId = id;
			} else if (ChangeGovernmentEligibility.LEADERSHIP_GROUP.equalsIgnoreCase(holder.getPickingGroupId())) {
				leadId = id;
			}
			openGovernmentPicker(player, attacker, defender, govId, leadId);
		}
	}

	public void handleConfirm(Player player, WarDeclareRequest request, boolean confirmed) {
		if (!confirmed) {
			returnToPreviousPicker(player, request);
			return;
		}
		WarValidationResult validation = new WarGoalValidator().validate(request);
		if (!validation.isValid()) {
			player.sendMessage(validation.getMessage());
			returnToPreviousPicker(player, request);
			return;
		}
		executeDeclare(player, request);
	}

	private void returnToPreviousPicker(Player player, WarDeclareRequest request) {
		Faction attacker = request.getAttacker();
		Faction defender = request.getDefender();
		switch (request.getGoal()) {
			case WAR, TRIBUTARY, USURP, OPEN_MARKET, OVERTHROW, CHANGE_LAW, CHANGE_TAX ->
					openGoalPicker(player, attacker, defender);
			case CHANGE_GOVERNMENT -> openGovernmentPicker(
					player, attacker, defender, request.getGovernmentLawId(), request.getLeadershipLawId());
			case SUBJUGATE -> openRelationTypePicker(player, attacker, defender);
			case DE_JURE_ANNEX -> openTitlePicker(player, attacker, defender);
			case TRANSFER_SUBJECT -> openSubjectPicker(player, attacker, defender);
			case PILLAGE -> openSettlementPicker(player, attacker, defender);
		}
		player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
	}

	private void openConfirm(
			Player player,
			Faction attacker,
			Faction defender,
			WarGoalType goal,
			String targetTitleId,
			String subjectFactionId,
			String relationTypeId,
			String governmentLawId,
			String leadershipLawId,
			String targetSettlementId) {
		WarDeclareRequest request = new WarDeclareRequest(
				attacker,
				defender,
				goal,
				targetTitleId,
				subjectFactionId,
				relationTypeId,
				governmentLawId,
				leadershipLawId,
				targetSettlementId);
		WarValidationResult validation = new WarGoalValidator().validate(request);
		if (!validation.isValid()) {
			player.sendMessage(validation.getMessage());
			return;
		}
		inv.confirmWarDeclareView(player, request);
	}

	private void executeDeclare(Player player, WarDeclareRequest request) {
		War war = WarManager.declareWar(
				request.getAttacker(),
				request.getDefender(),
				request.getGoal(),
				request.getTargetTitleId(),
				request.getSubjectFactionId(),
				request.getRelationTypeId(),
				request.getGovernmentLawId(),
				request.getLeadershipLawId(),
				request.getTargetSettlementId());
		if (war == null) {
			String error = WarManager.getLastDeclareError();
			player.sendMessage(error != null ? error : "§cCould not declare war.");
			return;
		}
		player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
		inv.warList(player);
	}

	private void openGovernmentLawPicker(
			Player player,
			Faction attacker,
			Faction defender,
			String governmentLawId,
			String leadershipLawId,
			String groupId) {
		LawGroup group = ChangeGovernmentEligibility.group(defender, groupId);
		if (group == null || group.getLaws() == null) {
			return;
		}
		String selectedId = ChangeGovernmentEligibility.GOVERNMENT_GROUP.equalsIgnoreCase(groupId)
				? governmentLawId
				: leadershipLawId;
		DeclareWarHolder holder = new DeclareWarHolder(
				attacker.getId(),
				defender.getId(),
				SFGUI.WAR_DECLARE_GOVERNMENT_LAW,
				governmentLawId,
				leadershipLawId,
				groupId);
		Inventory inventory = SimpleFactions.plugin.getServer().createInventory(holder, 27, "§7Declare War - Law");
		int slot = 0;
		for (Law law : group.getLaws().values()) {
			if (slot >= 26) break;
			boolean selected = law != null && law.getId() != null && law.getId().equalsIgnoreCase(selectedId);
			inventory.setItem(slot++, creator.createGovernmentLawItem(law, selected));
		}
		inventory.setItem(26, inv.createBackButton(SFGUI.WAR_DECLARE_GOVERNMENT_LAW));
		player.openInventory(inventory);
	}

	private static Law lawInGroup(LawGroup group, String lawId) {
		if (group == null || group.getLaws() == null || lawId == null) {
			return null;
		}
		for (Law law : group.getLaws().values()) {
			if (law != null && law.getId() != null && law.getId().equalsIgnoreCase(lawId)) {
				return law;
			}
		}
		return null;
	}
}
