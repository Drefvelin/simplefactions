package me.Plugins.SimpleFactions.Managers.Inventory;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.Loaders.TierLoader;
import me.Plugins.SimpleFactions.Loaders.TitleLoader;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.InventoryManager;
import me.Plugins.SimpleFactions.Managers.RelationManager;
import me.Plugins.SimpleFactions.Managers.TitleManager;
import me.Plugins.SimpleFactions.Managers.Holder.SFInventoryHolder;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Tiers.Tier;
import me.Plugins.SimpleFactions.Tiers.Title;
import me.Plugins.SimpleFactions.enums.SFGUI;

public class TierTitleView {
	public InventoryManager inv;
	
	public TierTitleCreator creator = new TierTitleCreator();
	
	public TierTitleView(InventoryManager inv) {
		this.inv = inv;                   
	}
	
	public void tierView(Inventory i, Player player, Faction f, boolean open) {
		if(open) {
			i = SimpleFactions.plugin.getServer().createInventory(new SFInventoryHolder(f.getId(), SFGUI.TIER_VIEW), 54, "§7Tier View");
		}
		if(f.getLeader().equalsIgnoreCase(player.getName())) {
			int slot = 0;
			for(Tier t : TierLoader.get()) {
				i.setItem(slot, creator.createTierItem(f, t, -1));
				if(t.hasAliases()) {
					for(int x = 0; x<t.getAliases().size(); x++) {
						i.setItem(slot+x+1, creator.createTierItem(f, t, x));
					}
				}
				slot +=9;
			}
		}
		i.setItem(53, inv.createBackButton(SFGUI.TIER_VIEW));
		if(open) player.openInventory(i);
	}
	public void titleView(Inventory i, Player player, Faction f, boolean open) {
		if(open) {
			i = SimpleFactions.plugin.getServer().createInventory(new SFInventoryHolder(f.getId(), SFGUI.TITLE_VIEW), 54, "§7Title View");
		}
		Faction pf = FactionManager.getByLeader(player.getName());
		String overlord = RelationManager.getOverlord(f);
		if(f.getLeader().equalsIgnoreCase(player.getName())) {
			int slot = 10;
			for(Tier t : TierLoader.get()) {
				if(TitleLoader.getByTier(t).size() > 0) {
					i.setItem(slot, creator.createTierViewItem(f, t, TitleLoader.getByTier(t).size()));
					slot++;
				}
			}
		} else if(pf != null && overlord != null && overlord.equalsIgnoreCase(pf.getId())){
			int slot = 10;
			int added = 0;
			for(Tier t : TierLoader.get()) {
				if(TitleManager.getGrantableTitles(pf, f, t).size() > 0) {
					i.setItem(slot, creator.createTierViewItem(f, t, TitleManager.getGrantableTitles(pf, f, t).size()));
					added++;
					slot++;
				}
			}
			if(added == 0) {
				i.setItem(10, creator.createNoTitleItem());
			}
		} else {
			
		}
		i.setItem(53, inv.createBackButton(SFGUI.TITLE_VIEW));
		if(open) player.openInventory(i);
	}
	public void titleTypeView(Inventory i, Player player, Faction f, Tier tier, boolean open) {
		if(open) {
			i = SimpleFactions.plugin.getServer().createInventory(new SFInventoryHolder(f.getId(), SFGUI.TITLE_TYPE_VIEW), 54, tier.getName()+"§7 View");
		}
		Faction pf = FactionManager.getByLeader(player.getName());
		String overlord = RelationManager.getOverlord(f);
		int slot = 0;
		if(f.getLeader().equalsIgnoreCase(player.getName())) {
			for(ItemStack item : creator.getSortedTitleItems(player, f, TitleLoader.getByTier(tier))) {
				i.setItem(slot, item);
				slot++;
			}
			i.setItem(52, creator.createNewTitleItem(f, tier));
		} else if(pf != null && overlord != null && overlord.equalsIgnoreCase(pf.getId())) {
			for(Title t : TitleManager.getGrantableTitles(pf, f, tier)) {
				i.setItem(slot, creator.createGrantTitleItem(player, f, t));
				slot++;
			}
		}
		i.setItem(53, inv.createBackButton(SFGUI.TITLE_TYPE_VIEW));
		if(open) player.openInventory(i);
	}
	
	public void click(InventoryClickEvent e, Inventory inventory, Player p) {
		if(e.getView().getTitle().equalsIgnoreCase("§7Tier View")) {
			e.setCancelled(true);
			Material type = e.getCurrentItem().getType();
			if(!type.equals(Material.YELLOW_CONCRETE)) return;
			if(!(inventory.getHolder() instanceof SFInventoryHolder)) return;
			SFInventoryHolder h = (SFInventoryHolder) inventory.getHolder();
			Faction f = FactionManager.getByString(h.getId());
			NamespacedKey key = new NamespacedKey(SimpleFactions.plugin, "index");
			int index = e.getCurrentItem().getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
			f.getTier().setIndex(index);
			tierView(inventory, p, f, false);
			p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
			return;
			
		} else if(e.getView().getTitle().equalsIgnoreCase("§7Title View")) {
			e.setCancelled(true);
			if(!(inventory.getHolder() instanceof SFInventoryHolder)) return;
			SFInventoryHolder h = (SFInventoryHolder) inventory.getHolder();
			Faction f = FactionManager.getByString(h.getId());
			NamespacedKey key = new NamespacedKey(SimpleFactions.plugin, "id");
			String s = e.getCurrentItem().getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
			Tier t = TierLoader.getByString(s);
			if(t == null) return;
			titleTypeView(null, p, f, t, true);
			p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
			return;
			
		} else if(inventory.getHolder() instanceof SFInventoryHolder && ((SFInventoryHolder) inventory.getHolder()).getType().equals(SFGUI.TITLE_TYPE_VIEW)) {
			e.setCancelled(true);
			Material type = e.getCurrentItem().getType();
			SFInventoryHolder h = (SFInventoryHolder) inventory.getHolder();
			Faction f = FactionManager.getByString(h.getId());
			if(type.equals(Material.WRITABLE_BOOK)) {
				NamespacedKey key = new NamespacedKey(SimpleFactions.plugin, "tier");
				String tierString = e.getCurrentItem().getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
				Tier tier = TierLoader.getByString(tierString);
				int needed = creator.getNewTitleCost(f, tier);
				int current = 0;
				if(tier.getId().equalsIgnoreCase("county")) {
					current = f.getUntitledProvinces().size();
				} else {
					current = f.getFreeTitles(tier).size();
				}
				if(current < needed) {
					return;
				}
				TitleManager.isFormingTitle.put(p, tier);
				p.sendMessage("§eType the name of the new "+tier.getName()+" §ein chat.");
				p.closeInventory();
			}
			NamespacedKey key = new NamespacedKey(SimpleFactions.plugin, "id");
			String s = e.getCurrentItem().getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
			Title t = TitleLoader.getById(s);
			if(t == null) return;
			key = new NamespacedKey(SimpleFactions.plugin, "type");
			String action = e.getCurrentItem().getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
			
			if(action.equalsIgnoreCase("claim")) {
				if(!type.equals(Material.YELLOW_CONCRETE)) return;
				String o = RelationManager.getOverlord(f);
				if(o != null) {
					Faction overlord = FactionManager.getByString(o);
					if(overlord != null) {
						if(t.getTier().getTier() > overlord.getTier().getTier()) {
							p.sendMessage("§cCannot form this title as it would make your tier higher than that of your overlord!");
							p.playSound(p, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
							return;
						}
					}
				}		
				
				f.addTitle(t);
				titleTypeView(inventory, p, f, t.getTier(), false);
				p.sendMessage("§aClaimed the title "+t.getName());
				p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
			} else if(action.equalsIgnoreCase("grant")) {
				if(!type.equals(Material.GREEN_CONCRETE)) return;
				Faction pf = FactionManager.getByLeader(p.getName());
				if(pf == null) return;
				
				f.addTitle(t);
				pf.removeTitle(t);
				if(TitleManager.getGrantableTitles(f, pf, t.getTier()).size() > 0) titleTypeView(inventory, p, f, t.getTier(), false);
				else titleView(null, p, f, true);
				p.sendMessage("§aGranted "+f.getName()+" the title "+t.getName());
				p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
			}
			return;
			
		}
	}
}
