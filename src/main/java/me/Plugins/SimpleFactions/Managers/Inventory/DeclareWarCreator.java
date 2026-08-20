package me.Plugins.SimpleFactions.Managers.Inventory;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import me.Plugins.SimpleFactions.Managers.Inventory.IconGetter;
import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Tiers.Title;
import me.Plugins.SimpleFactions.War.enums.WarGoalType;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public class DeclareWarCreator {

	public ItemStack createSubjugateGoalItem() {
		return createGoalItem(WarGoalType.SUBJUGATE, "§lSubjugate", List.of(
				"§7Force the defender to become",
				"§7your subject."));
	}

	public ItemStack createDeJureGoalItem() {
		return createGoalItem(WarGoalType.DE_JURE_ANNEX, "§lDe Jure Annex", List.of(
				"§7Annex a title you partially",
				"§7control from the defender."));
	}

	public ItemStack createTransferSubjectGoalItem() {
		return createGoalItem(WarGoalType.TRANSFER_SUBJECT, "§lTransfer Subject", List.of(
				"§7Take one of the defender's",
				"§7subjects as your own."));
	}

	private ItemStack createGoalItem(WarGoalType goal, String name, List<String> description) {
		ItemStack item = IconGetter.getIcon("war");
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(StringFormatter.formatHex("#d42300" + name));
		List<String> lore = new ArrayList<>(description);
		lore.add(" ");
		lore.add("§eClick to select");
		meta.setLore(lore);
		NamespacedKey goalKey = new NamespacedKey(SimpleFactions.plugin, "goal");
		meta.getPersistentDataContainer().set(goalKey, PersistentDataType.STRING, goal.toJson());
		item.setItemMeta(meta);
		return item;
	}

	public ItemStack createTitleItem(Title title) {
		ItemStack item = new ItemStack(Material.PAPER, 1);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(title.getName());
		List<String> lore = new ArrayList<>();
		lore.add("§7Tier: §f" + title.getTier().getName());
		lore.add(" ");
		lore.add("§eClick to declare");
		meta.setLore(lore);
		NamespacedKey idKey = new NamespacedKey(SimpleFactions.plugin, "id");
		meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, title.getId());
		item.setItemMeta(meta);
		return item;
	}

	public ItemStack createSubjectItem(Faction subject) {
		ItemStack item = subject.getBanner();
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(subject.getName());
		List<String> lore = new ArrayList<>();
		lore.add("§7Subject faction");
		lore.add(" ");
		lore.add("§eClick to declare");
		meta.setLore(lore);
		NamespacedKey idKey = new NamespacedKey(SimpleFactions.plugin, "id");
		meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, subject.getId());
		item.setItemMeta(meta);
		return item;
	}
}
