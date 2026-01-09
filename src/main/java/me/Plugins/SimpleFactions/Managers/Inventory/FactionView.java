package me.Plugins.SimpleFactions.Managers.Inventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.Holder.SFInventoryHolder;
import me.Plugins.SimpleFactions.Managers.InventoryManager;
import me.Plugins.SimpleFactions.Managers.RelationManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.REST.RestServer;
import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.Utils.FactionRanker;
import me.Plugins.SimpleFactions.enums.MenuItemType;
import me.Plugins.SimpleFactions.enums.RankType;
import me.Plugins.SimpleFactions.enums.SFGUI;

public class FactionView {
	public InventoryManager inv;
	
	public FactionCreator creator = new FactionCreator();
	
	FactionRanker r = new FactionRanker();
	public static HashMap<Player, RankType> currentRanking = new HashMap<>();
	public static HashMap<Player, Integer> currentPage = new HashMap<>();

	private static final int INVENTORY_SIZE = 54;

	private static final List<Integer> RESERVED_SLOTS = List.of(
		8,   // Rank toggle
		17, 26, 35, 44, // Right-side column
		45,  // Previous page
		53   // Next page
	);

	private static final int PREV_PAGE_SLOT = 45;
	private static final int NEXT_PAGE_SLOT = 53;
	
	public FactionView(InventoryManager inv) {
		this.inv = inv;
	}
	
	public void factionList(Player player) {
		currentRanking.putIfAbsent(player, RankType.PRESTIGE);
		currentPage.putIfAbsent(player, 0);

		RankType rankType = currentRanking.get(player);
		int page = currentPage.get(player);

		List<Faction> factions = r.getRankedList(rankType);
		Collections.reverse(factions);

		// Build list of usable slots
		List<Integer> usableSlots = new ArrayList<>();
		for (int i = 0; i < INVENTORY_SIZE; i++) {
			if (!RESERVED_SLOTS.contains(i)) {
				usableSlots.add(i);
			}
		}

		int factionsPerPage = usableSlots.size();
		int startIndex = page * factionsPerPage;
		int endIndex = Math.min(startIndex + factionsPerPage, factions.size());

		Inventory inv = SimpleFactions.plugin.getServer()
				.createInventory(null, INVENTORY_SIZE, "§7Faction List");

		// Populate factions
		for (int i = startIndex; i < endIndex; i++) {
			Faction f = factions.get(i);
			int slot = usableSlots.get(i - startIndex);
			inv.setItem(slot, creator.createListItem(player, f));
		}

		// Rank toggle button
		inv.setItem(8, DefaultCreator.createRankButton(rankType));

		// Page buttons
		if (page > 0) {
			inv.setItem(PREV_PAGE_SLOT, DefaultCreator.createPreviousPageButton());
		}

		if (endIndex < factions.size()) {
			inv.setItem(NEXT_PAGE_SLOT, DefaultCreator.createNextPageButton());
		}

		player.openInventory(inv);
	}
	public void factionView(Player player, Faction f) {
		Inventory i = SimpleFactions.plugin.getServer().createInventory(new SFInventoryHolder(f.getId(), SFGUI.FACTION_VIEW), 54, "§7Faction View");
		if(f.getMembers().contains(player.getName())) i.setItem(1, creator.createMenuItem(player, f, MenuItemType.BANNER_GET));
		i.setItem(10, creator.createMenuItem(player, f, MenuItemType.BANNER));
		/*if(f.getLeader().equalsIgnoreCase(player.getName())) */i.setItem(19, creator.createMenuItem(player, f, MenuItemType.BANNER_RANDOM));
		i.setItem(11, creator.createMenuItem(player, f, MenuItemType.LEADER));
		i.setItem(12, creator.createMenuItem(player, f, MenuItemType.WEALTH));
		i.setItem(13, creator.createMenuItem(player, f, MenuItemType.PRESTIGE));
		i.setItem(14, creator.createMenuItem(player, f, MenuItemType.MEMBERS));
		i.setItem(16, creator.createMenuItem(player, f, MenuItemType.MODIFIERS));
		i.setItem(25, creator.createMenuItem(player, f, MenuItemType.TAX));
		i.setItem(29, creator.createMenuItem(player, f, MenuItemType.MILITARY));
		i.setItem(31, creator.createMenuItem(player, f, MenuItemType.DIPLOMACY));
		i.setItem(33, creator.createMenuItem(player, f, MenuItemType.TIER));
		i.setItem(34, creator.createMenuItem(player, f, MenuItemType.TITLES));
		i.setItem(53, inv.createBackButton(SFGUI.FACTION_VIEW));
		player.openInventory(i);
	}
	
	public void clickPreventions(InventoryClickEvent e, Inventory inventory, Player p) {
		if(!(inventory != null && inventory.getHolder() instanceof SFInventoryHolder)) return;
		if (e.getClickedInventory() == null) return;

		if (e.getView().getTopInventory().equals(e.getClickedInventory())) {
			e.setCancelled(true);
		}
		if (e.getAction() == InventoryAction.HOTBAR_SWAP
			|| e.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD) {
			e.setCancelled(true);
			return;
		}
		if (e.getClick().isShiftClick()) {
			e.setCancelled(true);
		}
	}
	public void click(InventoryClickEvent e, Inventory inventory, Player p) {
		clickPreventions(e, inventory, p);
		if(e.getView().getTitle().equalsIgnoreCase("§7Faction List")) {
			e.setCancelled(true);
			// Next page
			if (e.getSlot() == NEXT_PAGE_SLOT) {
				currentPage.put(p, currentPage.getOrDefault(p, 0) + 1);
				p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
				factionList(p);
				return;
			}

			// Previous page
			if (e.getSlot() == PREV_PAGE_SLOT) {
				currentPage.put(p, Math.max(0, currentPage.getOrDefault(p, 0) - 1));
				p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
				factionList(p);
				return;
			}
			if(e.getSlot() == 8) {
				RankType t = currentRanking.get(p);
				switch (t) {
					case PRESTIGE:
						currentRanking.put(p, RankType.WEALTH);
						break;
					case WEALTH:
						currentRanking.put(p, RankType.MEMBERS);
						break;
					default:
						currentRanking.put(p, RankType.PRESTIGE);
						break;
				}
				currentPage.put(p, 0);
				p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
				factionList(p);
			} else{
				ItemStack i = e.getCurrentItem();
				if(i == null) return;
				ItemMeta m = i.getItemMeta();
				NamespacedKey id = new NamespacedKey(SimpleFactions.plugin, "id");
				String factionId = m.getPersistentDataContainer().get(id, PersistentDataType.STRING);
				if(factionId == null) return;
				Faction f = FactionManager.getByString(factionId);
				if(f != null) {
					factionView(p, f);
					p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
				}
			}
		} else if(e.getView().getTitle().equalsIgnoreCase("§7Faction View")) {
			e.setCancelled(true);
			if(e.getSlot() == 29) {
				ItemStack item = e.getCurrentItem();
				ItemMeta m = item.getItemMeta();
				NamespacedKey id = new NamespacedKey(SimpleFactions.plugin, "id");
				String factionId = m.getPersistentDataContainer().get(id, PersistentDataType.STRING);
				if(factionId == null) return;
				Faction f = FactionManager.getByString(factionId);
				if(f != null) {
					inv.militaryView(null, p, f, true);
					p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
				}
			} else if(e.getSlot() == 31) {
				ItemStack item = e.getCurrentItem();
				ItemMeta m = item.getItemMeta();
				NamespacedKey id = new NamespacedKey(SimpleFactions.plugin, "id");
				String factionId = m.getPersistentDataContainer().get(id, PersistentDataType.STRING);
				if(factionId == null) return;
				Faction f = FactionManager.getByString(factionId);
				if(f != null) {
					inv.diplomacyView(null, p, f, true);
					p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
				}
			} else if(e.getSlot() == 33) {
				ItemStack item = e.getCurrentItem();
				ItemMeta m = item.getItemMeta();
				NamespacedKey id = new NamespacedKey(SimpleFactions.plugin, "id");
				String factionId = m.getPersistentDataContainer().get(id, PersistentDataType.STRING);
				if(factionId == null) return;
				Faction f = FactionManager.getByString(factionId);
				if(f != null && f.getLeader().equalsIgnoreCase(p.getName())) {
					inv.tierView(null, p, f, true);
					p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
				}
			} else if(e.getSlot() == 34) {
				ItemStack item = e.getCurrentItem();
				ItemMeta m = item.getItemMeta();
				NamespacedKey id = new NamespacedKey(SimpleFactions.plugin, "id");
				String factionId = m.getPersistentDataContainer().get(id, PersistentDataType.STRING);
				if(factionId == null) return;
				Faction f = FactionManager.getByString(factionId);
				if(f != null) {
					Faction pf = FactionManager.getByLeader(p.getName());
					if(pf == null) return;
					String overlord = RelationManager.getOverlord(f);
					if(f.getLeader().equalsIgnoreCase(p.getName()) || (overlord != null && overlord.equalsIgnoreCase(pf.getId()))) {
						inv.titleView(null, p, f, true);
						p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
					}
				}
			} else if(e.getSlot() == 19) {
				if(!(inventory.getHolder() instanceof SFInventoryHolder)) return;
				SFInventoryHolder h = (SFInventoryHolder) inventory.getHolder();
				Faction f = FactionManager.getByString(h.getId());
				f.setBannerPatterns(RestServer.fetchBannerList());
				inventory.setItem(10, creator.createMenuItem(p, f, MenuItemType.BANNER));
				p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
			} else if(e.getSlot() == 1) {
				if(!(inventory.getHolder() instanceof SFInventoryHolder)) return;
				SFInventoryHolder h = (SFInventoryHolder) inventory.getHolder();
				Faction f = FactionManager.getByString(h.getId());
				ItemStack i = new ItemStack(f.getBanner());
				p.getInventory().addItem(i);
				p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
			} else if(e.getSlot() == 25) {
				if(!(inventory.getHolder() instanceof SFInventoryHolder)) return;
				SFInventoryHolder h = (SFInventoryHolder) inventory.getHolder();
				Faction f = FactionManager.getByString(h.getId());
				if(!f.getLeader().equalsIgnoreCase(p.getName())) return;
				inv.taxView(p);;
				p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
			}
		}
	}
}
