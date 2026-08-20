package me.Plugins.SimpleFactions.War.battle.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.TLibs.TLibs;
import me.Plugins.TLibs.Enums.APIType;
import me.Plugins.TLibs.Objects.API.ItemAPI;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;
import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.Loaders.BattleTemplateLoader;
import me.Plugins.SimpleFactions.War.battle.enums.BattleType;
import me.Plugins.SimpleFactions.War.battle.enums.DefenderRespawnMode;
import me.Plugins.SimpleFactions.War.battle.enums.LifeType;
import me.Plugins.SimpleFactions.War.battle.engine.BattleContestSetup;
import me.Plugins.SimpleFactions.War.battle.engine.BattleRaidSetup;
import me.Plugins.SimpleFactions.War.battle.engine.Battle;
import me.Plugins.SimpleFactions.War.battle.engine.CapturePoint;
import me.Plugins.SimpleFactions.War.battle.engine.BattleManager;
import me.Plugins.SimpleFactions.War.battle.engine.BattleSide;
import me.Plugins.SimpleFactions.War.battle.warband.Warband;
import me.Plugins.SimpleFactions.War.battle.warband.WarbandManager;
import me.Plugins.SimpleFactions.War.battle.template.BattleLocation;
import me.Plugins.SimpleFactions.War.battle.template.BattleTemplate;
import me.Plugins.SimpleFactions.War.battle.template.ContestArea;
import me.Plugins.SimpleFactions.War.battle.warband.WarbandSlot;


public class BattleInventoryManager {
	public static final String TEMPLATE_NONE_ID = "__none__";

	private static NamespacedKey templateKey() {
		return new NamespacedKey(SimpleFactions.plugin, "battle_template");
	}

	public void battleView(Player player, Battle b) {
		Inventory i = SimpleFactions.plugin.getServer().createInventory(null, 27, "§7Battle View");
		populateBattleView(i, b);
		player.openInventory(i);
	}
	public void battleList(Player player) {
		Inventory i = SimpleFactions.plugin.getServer().createInventory(null, 27, "§7Battle List");
		for(int y = 0; y<BattleManager.get().size();y++) {
			i.setItem(y, createBattleItem(BattleManager.get().get(y)));
		}
		player.openInventory(i);
	}
	public void spawnList(Player player, Battle b) {
		Inventory i = SimpleFactions.plugin.getServer().createInventory(null, 27, "§7Respawn Points");
		for(int y = 0; y<b.getPointManager().getPoints().size();y++) {
			i.setItem(y, createSpawnPointItem(b.getPointManager().getPoints().get(y), b.getSideByPlayer(player)));
		}
		player.openInventory(i);
	}
	public void warbandList(Player player) {
		Inventory i = SimpleFactions.plugin.getServer().createInventory(null, 27, "§7Warband List");
		for(int y = 0; y<WarbandManager.get().size();y++) {
			i.setItem(y, createWarbandItem(WarbandManager.get().get(y)));
		}
		player.openInventory(i);
	}
	public void updateView(Player player, Battle b, Inventory i) {
		populateBattleView(i, b);
	}

	private void populateBattleView(Inventory i, Battle b) {
		i.setItem(0, createSideButton(b));
		i.setItem(1, createLockButton(b));
		i.setItem(2, createLifeButton(b));
		if (b.getBattleType() == BattleType.SIEGE) {
			i.setItem(3, createContestDurationButton(b));
			i.setItem(8, null);
			i.setItem(23, createContestButton(b));
		} else if (b.getBattleType() == BattleType.RAID) {
			i.setItem(3, createDefenderRespawnModeButton(b));
			if (BattleRaidSetup.getEffectiveDefenderRespawnMode(b) == DefenderRespawnMode.LIVES) {
				i.setItem(8, createDefenderLivesButton(b));
			} else {
				i.setItem(8, null);
			}
			i.setItem(23, createRaidTargetButton(b));
		} else {
			i.setItem(3, createLifeCount(b));
			i.setItem(8, createSequentialCaptureButton(b));
			i.setItem(23, createPointButton(b));
		}
		i.setItem(4, createGameRuleButton("Friendly Fire", b.hasFriendlyFire()));
		i.setItem(5, createGameRuleButton("Keep Inventory", b.hasKeepInventory()));
		i.setItem(6, createGameRuleButton("TP on start", b.hasTeleport()));
		i.setItem(7, createTemplateButton(b));
		if(b.hasStarted()) {
			i.setItem(18, createStopButton(b));
		} else {
			i.setItem(18, createStartButton(b));
		}
	}

	public void contestView(Player player, Battle b) {
		Inventory i = SimpleFactions.plugin.getServer().createInventory(null, 27, "§7Contest View");
		i.setItem(0, createContestCornerItem("Min", b.getContestArea() != null ? b.getContestArea().getMin() : null));
		i.setItem(1, createContestCornerItem("Max", b.getContestArea() != null ? b.getContestArea().getMax() : null));
		i.setItem(2, createContestDurationButton(b));
		if (b.hasStarted()) {
			i.setItem(3, createContestHoldItem(b));
		}
		i.setItem(26, createBackButton());
		player.openInventory(i);
	}

	public void raidTargetView(Player player, Battle b) {
		Inventory i = SimpleFactions.plugin.getServer().createInventory(null, 27, "§7Raid Target View");
		i.setItem(0, createRaidTargetLocationItem(b));
		if (b.hasStarted() && !b.getPointManager().getPoints().isEmpty()) {
			i.setItem(1, createPointItem(b.getPointManager().getPoints().get(0)));
		}
		i.setItem(26, createBackButton());
		player.openInventory(i);
	}
	public void sideSelection(Player player, Battle b) {
		Inventory i = SimpleFactions.plugin.getServer().createInventory(null, 27, "§7Side Selection");
		for(int y = 0; y<b.getSides().size();y++) {
			i.setItem(y, createSideItem(b.getSides().get(y)));
		}
		i.setItem(26, createBackButton());
		player.openInventory(i);
	}
	public void sideView(Player player, Battle b) {
		Inventory i = SimpleFactions.plugin.getServer().createInventory(null, 27, "§7Side View");
		for(int y = 0; y<b.getSides().size();y++) {
			i.setItem(y, createSideItem(b.getSides().get(y)));
		}
		i.setItem(26, createBackButton());
		player.openInventory(i);
	}
	public void pointView(Player player, Battle b) {
		Inventory i = SimpleFactions.plugin.getServer().createInventory(null, 27, "§7Point View");
		for(int y = 0; y<b.getPoints().size();y++) {
			i.setItem(y, createPointItem(b.getPoints().get(y)));
		}
		i.setItem(26, createBackButton());
		player.openInventory(i);
	}
	public void templateView(Player player, Battle b) {
		Inventory i = SimpleFactions.plugin.getServer().createInventory(null, 27, "§7Template Selection");
		i.setItem(0, createNoneTemplateItem());
		int slot = 1;
		if (b.getBattleType() != null) {
			for (Map.Entry<String, BattleTemplate> entry : BattleTemplateLoader.getAll().entrySet()) {
				if (entry.getValue().getType() != b.getBattleType()) {
					continue;
				}
				if (slot >= 26) {
					break;
				}
				i.setItem(slot++, createTemplateItem(entry.getKey()));
			}
		}
		i.setItem(26, createBackButton());
		player.openInventory(i);
	}
	public ItemStack createSequentialCaptureButton(Battle b) {
		return createGameRuleButton("Sequential capture", b.isSequentialCapture());
	}

	public ItemStack createTemplateButton(Battle b) {
		ItemStack i = new ItemStack(Material.WRITABLE_BOOK, 1);
		ItemMeta meta = i.getItemMeta();
		String templateLabel = b.getTemplateName() != null ? b.getTemplateName() : "None";
		meta.setDisplayName("§fTemplate: §e" + templateLabel);
		List<String> lore = new ArrayList<String>();
		lore.add("§7Click to change template");
		meta.setLore(lore);
		i.setItemMeta(meta);
		return i;
	}
	public ItemStack createNoneTemplateItem() {
		ItemStack i = new ItemStack(Material.BARRIER, 1);
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName("§7None (base)");
		List<String> lore = new ArrayList<String>();
		lore.add("§7Reset battle layout to base defaults");
		meta.setLore(lore);
		meta.getPersistentDataContainer().set(templateKey(), PersistentDataType.STRING, TEMPLATE_NONE_ID);
		i.setItemMeta(meta);
		return i;
	}
	public ItemStack createTemplateItem(String templateId) {
		ItemStack i = new ItemStack(Material.BOOK, 1);
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName("§e" + templateId);
		List<String> lore = new ArrayList<String>();
		lore.add("§7Apply this template");
		lore.add("§cWipes current battle layout");
		meta.setLore(lore);
		meta.getPersistentDataContainer().set(templateKey(), PersistentDataType.STRING, templateId);
		i.setItemMeta(meta);
		return i;
	}
	public static String getTemplateIdFromItem(ItemStack item) {
		if (item == null || !item.hasItemMeta()) {
			return null;
		}
		return item.getItemMeta().getPersistentDataContainer().get(templateKey(), PersistentDataType.STRING);
	}
	public ItemStack createBackButton() {
		ItemStack i = new ItemStack(Material.BARRIER, 1);
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName("§cBACK");
		i.setItemMeta(meta);
		return i;
	}
	public ItemStack createSpawnPointItem(CapturePoint point, BattleSide s) {
		ItemStack i = new ItemStack(Material.YELLOW_CONCRETE, 1);
		List<String> lore = new ArrayList<String>();
		if(!point.getController().getId().equals(s.getId())) {
			i.setType(Material.RED_CONCRETE);
			ItemMeta meta = i.getItemMeta();
			meta.setDisplayName("§c"+point.getId());
			lore.add("§cEnemy Control");
			lore.add("§7x"+Math.round(point.getLoc().getX())+", y"+Math.round(point.getLoc().getY())+", z"+Math.round(point.getLoc().getZ()));
			meta.setLore(lore);
			i.setItemMeta(meta);
		} else {
			if(point.getCaptureProgress() >= 50) {
				i.setType(Material.GREEN_CONCRETE);
				ItemMeta meta = i.getItemMeta();
				meta.setDisplayName("§a"+point.getId());
				lore.add("§aFriendly Control");
				lore.add("§7x"+Math.round(point.getLoc().getX())+", y"+Math.round(point.getLoc().getY())+", z"+Math.round(point.getLoc().getZ()));
				meta.setLore(lore);
				i.setItemMeta(meta);
			} else {
				ItemMeta meta = i.getItemMeta();
				meta.setDisplayName("§e"+point.getId());
				lore.add("§eContested");
				lore.add("§7x"+Math.round(point.getLoc().getX())+", y"+Math.round(point.getLoc().getY())+", z"+Math.round(point.getLoc().getZ()));
				meta.setLore(lore);
				i.setItemMeta(meta);
			}
		}
		return i;
	}
	public ItemStack createWarbandItem(Warband w) {
		ItemStack i = new ItemStack(Material.SHIELD, 1);
		ItemMeta meta = i.getItemMeta();
		if(w.isFaction()) meta.setCustomModelData(2);
		else meta.setCustomModelData(1);
		meta.setDisplayName("§e"+w.getName());
		List<String> lore = new ArrayList<String>();
		if(w.isFaction()) {
			lore.add(StringFormatter.formatHex("#ba4b3a§lFaction Warband"));
		}
		lore.add("§aLeader: "+w.getLeader().getName());
		lore.add(" ");
		lore.add("§7Soldiers: "+w.getPlayers().size());
		lore.add(" ");
		if(!w.isFaction()) {
			if(w.isLocked()) {
				lore.add("§cLOCKED");
			} else {
				lore.add("§aOPEN");
			}
		} else {
			lore.add(StringFormatter.formatHex("#7fbd73Slots:"));
			for(Map.Entry<Faction, WarbandSlot> entry : w.getSlots().entrySet()) {
				WarbandSlot slot = entry.getValue();
				lore.add(entry.getKey().getName()+"§e: §7"+slot.getCurrent()+"/"+slot.getMax());
			}
		}
		
		meta.setLore(lore);
		NamespacedKey key = new NamespacedKey(SimpleFactions.plugin, "id");
		meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, w.getId());
		i.setItemMeta(meta);
		return i;
	}
	public ItemStack createBattleItem(Battle b) {
		ItemStack i = new ItemStack(Material.IRON_SWORD, 1);
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName("§e"+b.getId());
		List<String> lore = new ArrayList<String>();
		lore.add("§7Participants: "+b.getAllParticipants().size());
		meta.setLore(lore);
		i.setItemMeta(meta);
		return i;
	}
	public ItemStack createSideItem(BattleSide s) {
		ItemStack i = new ItemStack(Material.EMERALD, 1);
		ItemMeta meta = i.getItemMeta();
		int count = 0;
		List<String> lore = new ArrayList<String>();
		for(Warband w : s.getBands()) {
			lore.add("§f"+w.getId()+": "+w.getPlayers().size());
			count = count+w.getPlayers().size();
		}
		meta.setDisplayName("§e"+s.getId()+": §f"+count);
		meta.setLore(lore);
		i.setItemMeta(meta);
		return i;
	}
	public ItemStack createPointItem(CapturePoint p) {
		ItemStack i = new ItemStack(Material.GRAY_BANNER, 1);
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName("§e"+p.getId()+": §f"+p.getController().getId()+ " §7("+p.getCaptureProgress()+"%)");
		List<String> lore = new ArrayList<String>();
		lore.add("§7x"+Math.round(p.getLoc().getX())+", y"+Math.round(p.getLoc().getY())+", z"+Math.round(p.getLoc().getZ()));
		meta.setLore(lore);
		i.setItemMeta(meta);
		return i;
	}
	public ItemStack createStartButton(Battle b) {
		ItemStack i = new ItemStack(Material.IRON_SWORD, 1);
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName("§aSTART BATTLE");
		if(b.hasStarted()) {
			meta.addEnchant(Enchantment.UNBREAKING, 1, false);
			meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		}
		i.setItemMeta(meta);
		return i;
	}
	public ItemStack createStopButton(Battle b) {
		ItemStack i = new ItemStack(Material.BARRIER, 1);
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName("§cEnd Battle");
		List<String> lore = new ArrayList<String>();
		lore.add("§7Stops the battle");
		meta.setLore(lore);
		i.setItemMeta(meta);
		return i;
	}
	public ItemStack createLifeCount(Battle b) {
		ItemStack i = new ItemStack(Material.RED_DYE, 1);
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName("§eLives: §c"+b.getLives());
		List<String> lore = new ArrayList<String>();
		if(b.getLifeType().equals(LifeType.COLLECTIVE)) {
			lore.add("§7Collective: Each side has "+b.getLives()+" respawns in total");
		} else if(b.getLifeType().equals(LifeType.PER_PLAYER)) {
			lore.add("§7Each player has "+b.getLives()+" respawns");
		}	
		meta.setLore(lore);
		i.setItemMeta(meta);
		return i;
	}
	public ItemStack createLifeButton(Battle b) {
		ItemStack i = new ItemStack(Material.DIRT, 1);
		if(b.getLifeType().equals(LifeType.COLLECTIVE)) {
			i.setType(Material.GOLDEN_APPLE);
			ItemMeta meta = i.getItemMeta();
			meta.setDisplayName("§eLife Mode: §aCollective");
			List<String> lore = new ArrayList<String>();
			lore.add("§7Each side has a common pool of lives");
			meta.setLore(lore);
			i.setItemMeta(meta);
		} else if(b.getLifeType().equals(LifeType.PER_PLAYER)) {
			i.setType(Material.PLAYER_HEAD);
			ItemMeta meta = i.getItemMeta();
			meta.setDisplayName("§eLife Mode: §aPer-Player");
			List<String> lore = new ArrayList<String>();
			lore.add("§7Each player has a specific number of respawns");
			meta.setLore(lore);
			i.setItemMeta(meta);
		}
		return i;
	}
	public ItemStack createSideButton(Battle b) {
		ItemStack i = new ItemStack(Material.NETHER_STAR, 1);
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName("§fSides: §e"+b.getSides().size());
		i.setItemMeta(meta);
		return i;
	}
	public ItemStack createContestButton(Battle b) {
		ItemStack i = new ItemStack(Material.IRON_BLOCK, 1);
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName("§fContest Area");
		List<String> lore = new ArrayList<String>();
		if (b.getContestArea() != null && b.getContestArea().isConfigured()) {
			lore.add("§aConfigured");
		} else {
			lore.add("§cNot configured");
		}
		lore.add("§7Click to view contest setup");
		meta.setLore(lore);
		i.setItemMeta(meta);
		return i;
	}

	public ItemStack createContestDurationButton(Battle b) {
		ItemStack i = new ItemStack(Material.CLOCK, 1);
		ItemMeta meta = i.getItemMeta();
		int duration = BattleContestSetup.getEffectiveDurationSeconds(b);
		meta.setDisplayName("§eContest duration: §f" + duration + "s");
		List<String> lore = new ArrayList<String>();
		if (!b.hasStarted()) {
			lore.add("§7Click to cycle duration");
		} else {
			lore.add("§7Duration locked after start");
		}
		meta.setLore(lore);
		i.setItemMeta(meta);
		return i;
	}

	public ItemStack createContestHoldItem(Battle b) {
		ItemStack i = new ItemStack(Material.GOLD_BLOCK, 1);
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName("§eHold remaining: §f" + b.getContestHoldRemainingSeconds() + "s");
		i.setItemMeta(meta);
		return i;
	}

	public ItemStack createContestCornerItem(String label, BattleLocation corner) {
		ItemStack i = new ItemStack(Material.STONE_BRICKS, 1);
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName("§eContest " + label);
		List<String> lore = new ArrayList<String>();
		if (corner == null) {
			lore.add("§cNot set");
		} else {
			lore.add("§7x" + Math.round(corner.getX()) + ", y" + Math.round(corner.getY()) + ", z" + Math.round(corner.getZ()));
		}
		meta.setLore(lore);
		i.setItemMeta(meta);
		return i;
	}

	public static int cycleContestDuration(int current) {
		int[] options = { 60, 120, 180, 240, 300 };
		for (int i = 0; i < options.length; i++) {
			if (options[i] == current) {
				return options[(i + 1) % options.length];
			}
		}
		return 180;
	}

	public static int cycleDefenderLives(int current) {
		int[] options = { 5, 10, 15, 20, 25, 50 };
		for (int i = 0; i < options.length; i++) {
			if (options[i] == current) {
				return options[(i + 1) % options.length];
			}
		}
		return 25;
	}

	public ItemStack createDefenderRespawnModeButton(Battle b) {
		DefenderRespawnMode mode = BattleRaidSetup.getEffectiveDefenderRespawnMode(b);
		ItemStack i = new ItemStack(Material.RESPAWN_ANCHOR, 1);
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName("§fDefender respawn: §e" + mode.toJson());
		List<String> lore = new ArrayList<String>();
		lore.add("§7Click to toggle infinite / lives");
		meta.setLore(lore);
		i.setItemMeta(meta);
		return i;
	}

	public ItemStack createDefenderLivesButton(Battle b) {
		ItemStack i = new ItemStack(Material.APPLE, 1);
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName("§fDefender lives: §e" + BattleRaidSetup.getEffectiveDefenderLives(b));
		List<String> lore = new ArrayList<String>();
		lore.add("§7Click to cycle pool size");
		meta.setLore(lore);
		i.setItemMeta(meta);
		return i;
	}

	public ItemStack createRaidTargetButton(Battle b) {
		ItemStack i = new ItemStack(Material.TARGET, 1);
		ItemMeta meta = i.getItemMeta();
		String label = b.getRaidTarget() != null && b.getRaidTarget().getId() != null
				? b.getRaidTarget().getId()
				: "unset";
		meta.setDisplayName("§fRaid Target: §e" + label);
		List<String> lore = new ArrayList<String>();
		lore.add("§7Open raid target view");
		meta.setLore(lore);
		i.setItemMeta(meta);
		return i;
	}

	public ItemStack createRaidTargetLocationItem(Battle b) {
		ItemStack i = new ItemStack(Material.ENDER_PEARL, 1);
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName("§fTarget location");
		List<String> lore = new ArrayList<String>();
		if (b.getRaidTarget() != null && b.getRaidTarget().getLocation() != null) {
			BattleLocation location = b.getRaidTarget().getLocation();
			lore.add("§7x" + Math.round(location.getX()) + ", y" + Math.round(location.getY())
					+ ", z" + Math.round(location.getZ()));
		} else {
			lore.add("§7Not set");
		}
		meta.setLore(lore);
		i.setItemMeta(meta);
		return i;
	}

	public ItemStack createPointButton(Battle b) {
		ItemStack i = new ItemStack(Material.RED_BANNER, 1);
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName("§fCapture Points: §e"+b.getPoints().size());
		i.setItemMeta(meta);
		return i;
	}
	public ItemStack createGameRuleButton(String s, boolean b) {
		if(b) {
			ItemStack i = new ItemStack(Material.LIME_DYE, 1);
			ItemMeta meta = i.getItemMeta();
			meta.setDisplayName("§f"+s+": §bTRUE");
			i.setItemMeta(meta);
			return i;
		} else {
			ItemStack i = new ItemStack(Material.GRAY_DYE, 1);
			ItemMeta meta = i.getItemMeta();
			meta.setDisplayName("§f"+s+": §cFALSE");
			i.setItemMeta(meta);
			return i;
		}
		
	}
	ItemStack createLockButton(Battle b) {
		ItemAPI api = (ItemAPI) TLibs.getApiInstance(APIType.ITEM_API);
		ItemStack i = api.getCreator().getItemsAdderItem("mcicons:icon_unlock");;
		ItemMeta m = i.getItemMeta();
		m.setDisplayName("§aUnlocked");
		List<String> lore = new ArrayList<String>();
		if(b.isLocked()) {
			i = api.getCreator().getItemsAdderItem("mcicons:icon_lock");
			m = i.getItemMeta();
			m.setDisplayName("§cLocked");
			lore.add("§7Warbands cannot join sides");
		} else {
			lore.add("§7Warbands can join sides");
		}
		m.setLore(lore);
		i.setItemMeta(m);
		return i;
	}
	
}