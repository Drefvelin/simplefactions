package me.Plugins.SimpleFactions.Managers;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import me.Plugins.SimpleFactions.Army.Regiment;
import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Managers.Holder.SFCombinedInventoryHolder;
import me.Plugins.SimpleFactions.Managers.Holder.SFInventoryHolder;
import me.Plugins.SimpleFactions.Managers.Holder.WarInventoryHolder;
import me.Plugins.SimpleFactions.Managers.Inventory.FactionView;
import me.Plugins.SimpleFactions.Managers.Inventory.GuildView;
import me.Plugins.SimpleFactions.Managers.Inventory.InventoryUpdater;
import me.Plugins.SimpleFactions.Managers.Inventory.MilitaryView;
import me.Plugins.SimpleFactions.Managers.Inventory.RelationView;
import me.Plugins.SimpleFactions.Managers.Inventory.TaxChange;
import me.Plugins.SimpleFactions.Managers.Inventory.TaxView;
import me.Plugins.SimpleFactions.Managers.Inventory.TierTitleView;
import me.Plugins.SimpleFactions.Managers.Inventory.WarView;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.Tiers.Tier;
import me.Plugins.SimpleFactions.War.Participant;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.enums.SFGUI;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public class InventoryManager implements Listener{
	public HashMap<Player, Faction> confirming = new HashMap<>();
	public HashMap<Player, TaxChange> taxChange = new HashMap<>();
	
	InventoryUpdater updater = new InventoryUpdater(this);
	
	public InventoryUpdater getUpdater() {
		return updater;
	}

	public void start() {
		new BukkitRunnable() {
			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				for(Map.Entry<Player, TaxChange> entry : ((HashMap<Player, TaxChange>) taxChange.clone()).entrySet()) {
					if(entry.getValue().tick()) {
						taxChange.remove(entry.getKey());
						entry.getKey().sendMessage("§cTax change timed out.");
					}
				}
			}
		}.runTaskTimer(SimpleFactions.plugin, 0, 20L);
	}
	
	/*
	 * This is not an ideal way to set up anything, however,
	 * this used to all be in one file which was 2000 lines
	 * so its a bit better albeit overly complicated!
	 */
	
	///WAAAAR
	public WarView warView = new WarView(this);
	
	public void warList(Player player) {
		warView.warList(player);
	}
	
	public void warView(Inventory i, Player player, War w, boolean open) {
		warView.warView(i, player, w, open);
	}
	
	public void warGoalView(Inventory i, Player player, War w, Faction target, Faction page, boolean open) {
		warView.warGoalView(i, player, w, target, page, open);
	}
	
	public void participantView(Inventory i, Player player, War w, Participant p, boolean open) { 
		warView.participantView(i, player, w, p, open); 
	}
	
	//Factions
	FactionView factionView = new FactionView(this);
	public void factionList(Player player) {
		factionView.factionList(player);
	}
	public void factionView(Player player, Faction f) {
		factionView.factionView(player, f);
	}

	//Guilds
	GuildView guildView = new GuildView(this);
	public void guildList(Player player) {
		guildView.guildList(player);
	}
	public void guildView(Player player, Guild guild) {
		guildView.guildView(player, guild);
	}
	
	//Tiers
	public TierTitleView tierTitleView = new TierTitleView(this);
	
	public void tierView(Inventory i, Player player, Faction f, boolean open) {
		tierTitleView.tierView(i, player, f, open);
	}
	public void titleView(Inventory i, Player player, Faction f, boolean open) {
		tierTitleView.titleView(i, player, f, open);
	}
	public void titleTypeView(Inventory i, Player player, Faction f, Tier tier, boolean open, int page) {
		tierTitleView.titleTypeView(i, player, f, tier, open, page);
	}
	
	//Military
	public MilitaryView militaryView = new MilitaryView(this);
	
	public void militaryView(Inventory i, Player player, Faction f, boolean open) {
		militaryView.militaryView(i, player, f, open);
	}
	
	//Relations
	
	public RelationView relationView = new RelationView(this);
	
	public void diplomacyView(Inventory i, Player player, Faction f, boolean open) {
		relationView.diplomacyView(i, player, f, open);
	}
	public void attitudeView(Inventory i, Player player, Faction f, boolean open) {
		relationView.attitudeView(i, player, f, open);
	}
	public void relationView(Inventory i, Player player, Faction f, boolean open) {
		relationView.relationView(i, player, f, open);
	}

	//Tax
	public TaxView taxView = new TaxView(this);

	public void taxView(Player p) {
		taxView.taxView(p);
	}

	public boolean isChanging(Player p) {
		return taxChange.containsKey(p);
	}

	public void setChanging(Player p, boolean b) {
		taxChange.put(p, new TaxChange(b));
	}

	@EventHandler
	public void setRate(AsyncPlayerChatEvent e) {
		Player p = e.getPlayer();
		if(!isChanging(p)) return;
		e.setCancelled(true);
		new BukkitRunnable() {
			@Override
			public void run() {
				Faction f = FactionManager.getByLeader(p.getName());
				if(f == null) {
					taxChange.remove(p);
					return;
				}
				TaxChange change = taxChange.get(p);
				double amount = 0;
				try {
					amount = Double.parseDouble(e.getMessage());
				} catch (Exception e) {
					p.sendMessage("§cError inputting the amount, use the format §e15.67 §cfor 15.67% tax (example)");
					return;
				}
				amount = Math.round(amount*100.0)/100.0;
				amount = Math.min(100.0, amount);
				if(change.isDomestic()) {
					double tax = f.setTaxRate(amount);
					p.sendMessage(StringFormatter.formatHex("§7Set #7a915eDomestic §7tax rate to #a39a84"+tax+"%"));
					p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
				} else {
					f.setVassalTaxRate(amount);
					p.sendMessage(StringFormatter.formatHex("§7Set #6c93bdVassal §7tax rate to #a39a84"+f.getVassalTaxRate()+"%"));
					p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
				}
				taxChange.remove(p);
				taxView.taxView(p);
			}
		}.runTask(SimpleFactions.plugin);
	}
	
	//Confirm
	public void confirmView(Player player, Faction f, String key, String data) {
		Inventory i = SimpleFactions.plugin.getServer().createInventory(null, 27, "§7Confirm Action");
		i.setItem(11, createButton("confirm", key, data));
		i.setItem(15, createButton("cancel", key, data));
		player.openInventory(i);
	}
	
	
	//Basic Items
	public ItemStack getFiller(Material mat) {
		ItemStack i = new ItemStack(mat, 1);
		ItemMeta m = i.getItemMeta();
		m.setDisplayName("§c");
		i.setItemMeta(m);
		return i;
	}
	
	public ItemStack createButton(String type, String key, String data) {
		ItemStack i = new ItemStack(Material.GREEN_CONCRETE);
		if(type.equalsIgnoreCase("cancel")) {
			i.setType(Material.RED_CONCRETE);
		}
		ItemMeta m = i.getItemMeta();
		if(type.equalsIgnoreCase("cancel")) {
			m.setDisplayName("§cCancel");
		} else {
			m.setDisplayName("§cConfirm");
		}
		NamespacedKey id = new NamespacedKey(SimpleFactions.plugin, key);
		m.getPersistentDataContainer().set(id, PersistentDataType.STRING, data);
		i.setItemMeta(m);
		return i;
	}
	
	public ItemStack createBackButton(SFGUI gui) {
		ItemStack i = new ItemStack(Material.BARRIER, 1);
		ItemMeta m = i.getItemMeta();
		m.setDisplayName("§cBack");
		NamespacedKey key = new NamespacedKey(SimpleFactions.plugin, "gui");
		m.getPersistentDataContainer().set(key, PersistentDataType.STRING, gui.toString());
		i.setItemMeta(m);
		return i;
	}
	
	@EventHandler
	public void clickButton(InventoryClickEvent e) {
		Player p = (Player) e.getWhoClicked();
		if(e.getCurrentItem() == null) return;
		Inventory inv = e.getView().getTopInventory();
		if(inv.getHolder() instanceof SFInventoryHolder) {
			e.setCancelled(true);
			SFInventoryHolder h = (SFInventoryHolder) inv.getHolder();
			Faction f = FactionManager.getByString(h.getId());
			if(e.getCurrentItem().getType().equals(Material.BARRIER)) {
				p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
				switch(h.getType()) {
					case ATTITUDE_VIEW:
						diplomacyView(null, p, f, true);
						break;
					case DIPLOMACY_VIEW:
						factionView(p, f);
						break;
					case FACTION_VIEW:
						factionList(p);
						break;
					case GUILD_VIEW:
						guildList(p);
						break;
					case MILITARY_VIEW:
						factionView(p, f);
						break;
					case RELATION_VIEW:
						diplomacyView(null, p, f, true);
						break;
					case TIER_VIEW:
						factionView(p, f);
						break;
					case TITLE_VIEW:
						factionView(p, f);
						break;
					case TITLE_TYPE_VIEW:
						titleView(null, p, f, true);
						break;
					default:
						break;
				}
			}
		}
		if(e.getView().getTitle().equalsIgnoreCase("§7Confirm Action")) {
			e.setCancelled(true);
			if(!confirming.containsKey(p)) return;
			ItemStack item = e.getCurrentItem();
			ItemMeta m = item.getItemMeta();
			NamespacedKey key = new NamespacedKey(SimpleFactions.plugin, "regiment");
			String data = m.getPersistentDataContainer().get(key, PersistentDataType.STRING);
			if(data != null) {
				Faction f = confirming.get(p);
				if(item.getType().equals(Material.RED_CONCRETE)) {
					militaryView(null, p, f, true);
					p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
					return;
				}
				Regiment r = f.getMilitary().getRegiment(data);
				r.sizeDecrease();
				p.sendMessage("§cDecrased size of "+r.getName());
				p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
				militaryView(null, p, f, true);
				return;
			}
		} else if(e.getView().getTitle().equalsIgnoreCase("§7Faction List") || e.getView().getTitle().equalsIgnoreCase("§7Faction View")) {
			factionView.click(e, inv, p);
		} else if(e.getView().getTitle().equalsIgnoreCase("§7Guild List") ||e.getView().getTitle().equalsIgnoreCase("§7Guild View")) {
			guildView.click(e, inv, p);
		} else if(e.getView().getTitle().equalsIgnoreCase("§7Military View")) {
			militaryView.click(e, inv, p);
		} else if(e.getView().getTitle().equalsIgnoreCase("§7Diplomacy View") || e.getView().getTitle().equalsIgnoreCase("§7Change Attitude") || e.getView().getTitle().equalsIgnoreCase("§7Change Relation")) {
			relationView.click(e, inv, p);
		} else if(e.getView().getTitle().equalsIgnoreCase("§7Tier View") 
				|| e.getView().getTitle().equalsIgnoreCase("§7Title View")
				|| (inv.getHolder() instanceof SFInventoryHolder && ((SFInventoryHolder) inv.getHolder()).getType().equals(SFGUI.TITLE_TYPE_VIEW))) {
			tierTitleView.click(e, inv, p);
		} else if(e.getView().getTitle().equalsIgnoreCase("§7War List")
				|| (inv.getHolder() instanceof WarInventoryHolder && ((WarInventoryHolder) inv.getHolder()).getType().equals(SFGUI.WAR_VIEW))
				|| (inv.getHolder() instanceof SFCombinedInventoryHolder && ((SFCombinedInventoryHolder) inv.getHolder()).getType().equals(SFGUI.PARTICIPANT_VIEW))
				|| (inv.getHolder() instanceof SFCombinedInventoryHolder && ((SFCombinedInventoryHolder) inv.getHolder()).getType().equals(SFGUI.WARGOAL_VIEW))) {
			warView.click(e, inv, p);
		} else if(inv.getHolder() instanceof SFInventoryHolder && ((SFInventoryHolder) inv.getHolder()).getType().equals(SFGUI.TAX_VIEW)) {
			taxView.click(e, inv, p);
		}
	}
}
