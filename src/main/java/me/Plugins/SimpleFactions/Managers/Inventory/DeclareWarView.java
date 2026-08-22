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

import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.Holder.DeclareWarHolder;
import me.Plugins.SimpleFactions.Managers.InventoryManager;
import me.Plugins.SimpleFactions.Managers.WarManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Tiers.Title;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.WarDeclareHelper;
import me.Plugins.SimpleFactions.War.enums.WarGoalType;
import me.Plugins.SimpleFactions.War.validation.WarDeclareRequest;
import me.Plugins.SimpleFactions.War.validation.WarGoalValidator;
import me.Plugins.SimpleFactions.War.validation.WarValidationResult;
import me.Plugins.SimpleFactions.enums.SFGUI;

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
		inventory.setItem(slot++, creator.createSubjugateGoalItem());
		if (!WarDeclareHelper.eligibleDeJureTitles(attacker, defender).isEmpty()) {
			inventory.setItem(slot++, creator.createDeJureGoalItem());
		}
		if (!WarDeclareHelper.defenderSubjects(defender).isEmpty()) {
			inventory.setItem(slot, creator.createTransferSubjectGoalItem());
		}
		inventory.setItem(26, inv.createBackButton(SFGUI.WAR_DECLARE_GOAL));
		player.openInventory(inventory);
	}

	public void openTitlePicker(Player player, Faction attacker, Faction defender) {
		DeclareWarHolder holder = new DeclareWarHolder(attacker.getId(), defender.getId(), SFGUI.WAR_DECLARE_TITLE);
		Inventory inventory = SimpleFactions.plugin.getServer().createInventory(holder, 27, "§7Declare War - Title");
		int slot = 0;
		for (Title title : WarDeclareHelper.eligibleDeJureTitles(attacker, defender)) {
			if (slot >= 26) break;
			inventory.setItem(slot++, creator.createTitleItem(title));
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
				case SUBJUGATE -> finishDeclare(player, attacker, defender, goal, null, null);
				case DE_JURE_ANNEX -> openTitlePicker(player, attacker, defender);
				case TRANSFER_SUBJECT -> openSubjectPicker(player, attacker, defender);
			}
			return;
		}

		NamespacedKey idKey = new NamespacedKey(SimpleFactions.plugin, "id");
		String id = item.getItemMeta().getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
		if (id == null) return;
		player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
		if (holder.getStep() == SFGUI.WAR_DECLARE_TITLE) {
			finishDeclare(player, attacker, defender, WarGoalType.DE_JURE_ANNEX, id, null);
		} else if (holder.getStep() == SFGUI.WAR_DECLARE_SUBJECT) {
			finishDeclare(player, attacker, defender, WarGoalType.TRANSFER_SUBJECT, null, id);
		}
	}

	private void finishDeclare(
			Player player,
			Faction attacker,
			Faction defender,
			WarGoalType goal,
			String targetTitleId,
			String subjectFactionId) {
		WarDeclareRequest request = new WarDeclareRequest(attacker, defender, goal, targetTitleId, subjectFactionId);
		WarValidationResult validation = new WarGoalValidator().validate(request);
		if (!validation.isValid()) {
			player.sendMessage(validation.getMessage());
			return;
		}
		War war = WarManager.declareWar(attacker, defender, goal, targetTitleId, subjectFactionId);
		if (war == null) {
			String error = WarManager.getLastDeclareError();
			player.sendMessage(error != null ? error : "§cCould not declare war.");
			return;
		}
		inv.warList(player);
	}
}
