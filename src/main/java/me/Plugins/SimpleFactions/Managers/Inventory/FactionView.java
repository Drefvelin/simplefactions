package me.Plugins.SimpleFactions.Managers.Inventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.InventoryManager;
import me.Plugins.SimpleFactions.Managers.RelationManager;
import me.Plugins.SimpleFactions.Managers.Holder.SFInventoryHolder;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.REST.RestServer;
import me.Plugins.SimpleFactions.Utils.FactionRanker;
import me.Plugins.SimpleFactions.enums.MenuItemType;
import me.Plugins.SimpleFactions.enums.RankType;
import me.Plugins.SimpleFactions.enums.SFGUI;

public class FactionView {
	public InventoryManager inv;
	
	public FactionCreator creator = new FactionCreator();
	
	FactionRanker r = new FactionRanker();
	public static HashMap<Player, RankType> currentRanking = new HashMap<>();
	
	public FactionView(InventoryManager inv) {
		this.inv = inv;
	}
	
	public void factionList(Player player) {
		
		if(!currentRanking.containsKey(player)) {
			currentRanking.put(player, RankType.PRESTIGE);
		}
		RankType t = currentRanking.get(player);
		Inventory i = SimpleFactions.plugin.getServer().createInventory(null, 54, "§7Faction List");
		Integer counter = 0;
		Integer fc = 0;
		List<Faction> factions = r.getRankedList(t);
		Collections.reverse(factions);
		List<Integer> reserved = new ArrayList<Integer>();
		reserved.add(8);
		reserved.add(17);
		reserved.add(26);
		reserved.add(35);
		reserved.add(44);
		reserved.add(53);
		while(fc < factions.size() && counter < 54) {
			if(!reserved.contains(counter)) {
				Faction f = factions.get(fc);
				ItemStack banner = creator.createListItem(player, f);
				i.setItem(counter, banner);
				fc++;
			}
			counter++;
		}
		i.setItem(8, creator.createRankButton(t));
		player.openInventory(i);
	}
	public void factionView(Player player, Faction f) {
		Inventory i = SimpleFactions.plugin.getServer().createInventory(new SFInventoryHolder(f.getId(), SFGUI.FACTION_VIEW), 54, "§7Faction View");
		if(f.getMembers().contains(player.getName())) i.setItem(1, creator.createMenuItem(player, f, MenuItemType.BANNER_GET));
		i.setItem(10, creator.createMenuItem(player, f, MenuItemType.BANNER));
		if(f.getLeader().equalsIgnoreCase(player.getName())) i.setItem(19, creator.createMenuItem(player, f, MenuItemType.BANNER_RANDOM));
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
	
	public void click(InventoryClickEvent e, Inventory inventory, Player p) {
		if(e.getView().getTitle().equalsIgnoreCase("§7Faction List")) {
			e.setCancelled(true);
			if(e.getSlot() == 8) {
				RankType t = currentRanking.get(p);
				if(t.equals(RankType.PRESTIGE)) {
					currentRanking.put(p, RankType.WEALTH);
				} else if(t.equals(RankType.WEALTH)){
					currentRanking.put(p, RankType.MEMBERS);
				} else {
					currentRanking.put(p, RankType.PRESTIGE);
				}
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
