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
import me.Plugins.SimpleFactions.Managers.Inventory.GovernmentView;
import me.Plugins.SimpleFactions.Managers.Inventory.GuildView;
import me.Plugins.SimpleFactions.Managers.Inventory.InventoryUpdater;
import me.Plugins.SimpleFactions.Managers.Inventory.LawView;
import me.Plugins.SimpleFactions.Managers.Inventory.MilitaryView;
import me.Plugins.SimpleFactions.Managers.Inventory.RelationView;
import me.Plugins.SimpleFactions.Managers.Inventory.TaxChange;
import me.Plugins.SimpleFactions.Managers.Inventory.TaxView;
import me.Plugins.SimpleFactions.Managers.Inventory.TierTitleView;
import me.Plugins.SimpleFactions.Managers.Inventory.WarView;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Objects.Handler.TaxHandler;
import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.Tiers.Tier;
import me.Plugins.SimpleFactions.War.Participant;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.enums.SFGUI;
import me.Plugins.SimpleFactions.government.Government;
import me.Plugins.SimpleFactions.government.proposal.Proposal;
import me.Plugins.SimpleFactions.government.proposal.TaxLawChange;
import me.Plugins.SimpleFactions.government.proposal.TaxTarget;

public class InventoryManager implements Listener{
	public HashMap<Player, Faction> confirming = new HashMap<>();
	public HashMap<Player, TaxChange> taxChange = new HashMap<>();
	
	InventoryUpdater updater = new InventoryUpdater(this);
	
	public InventoryUpdater getUpdater() {
		return updater;
	}

	public void start() {
		guildView.setProvinceManager(
			SimpleFactions.getInstance().getProvinceManager()
		);
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
	public void guildView(Player player, Guild guild, Inventory i) {
		guildView.guildView(player, guild, i);
	}

	//Laws
	LawView lawView = new LawView(this);
	public void lawView(Player player, Faction f, Inventory i) {
		lawView.lawView(player, f, i);
	}

	//Government
	GovernmentView governmentView = new GovernmentView(this);
	public void governmentView(Player player, Faction f, Inventory i) {
		governmentView.governmentView(player, f, i);
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

	public void setChanging(Faction faction, Player p, TaxTarget target, String id) {
		taxChange.put(p, new TaxChange(faction, target, id));
	}

	@EventHandler
	public void setRate(AsyncPlayerChatEvent e) {
		Player p = e.getPlayer();
		if(!isChanging(p)) return;
		e.setCancelled(true);
		new BukkitRunnable() {
			@Override
			public void run() {
				if(!taxChange.containsKey(p)) return;
				Faction f = taxChange.get(p).getFaction();
				if(f == null) {
					taxChange.remove(p);
					return;
				}
				if(e.getMessage().equalsIgnoreCase("cancel")) {
					p.sendMessage("§cTax change cancelled.");
					taxChange.remove(p);
					governmentView.governmentView(p, f, null);
					return;
				}
				TaxChange change = taxChange.get(p);
				double amount = 0;
				try {
					amount = Double.parseDouble(e.getMessage());
				} catch (Exception e) {
					p.sendMessage("§cError inputting the amount, use the format §e15.67 §cfor 15.67% tax (example)");
					p.sendMessage("§4Type 'cancel' to cancel.");
					return;
				}
				amount = Math.round(amount*100.0)/100.0;
				amount = Math.min(100.0, amount);
				TaxHandler handler = f.getTaxHandler();
				if(amount > handler.getMax(change.getTarget())) {
					p.sendMessage("§cThe maximum tax rate you can set for "+change.getTarget().getDisplayName()+" is §e"+handler.getMax(change.getTarget())+"%");
					p.sendMessage("§4Type 'cancel' to cancel.");
					return;
				} else if(amount < handler.getMin(change.getTarget())) {
					p.sendMessage("§cThe minimum tax rate you can set for "+change.getTarget().getDisplayName()+" is §e"+handler.getMin(change.getTarget())+"%");
					p.sendMessage("§4Type 'cancel' to cancel.");
					return;
				}
				Government gov = f.getGovernment();
				Proposal proposal = new Proposal(p.getName(), gov);
				TaxLawChange tax = new TaxLawChange(change.getTarget(), change.getId(), amount);
				proposal.setTaxProposal(tax);
				if(gov.canBeProposed(proposal)) {
					if(gov.canPropose(p)) {
						gov.propose(proposal);
						p.sendTitle("", "§aProposal Added", 20, 80, 20);
						p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
					} else if(gov.canProposeOrStartMovement(p)) {
						//movement start
						p.sendTitle("", "§cMovement Started", 20, 80, 20);
					} else {
						p.sendMessage("§cYou can no longer propose this change.");
						taxChange.remove(p);
						return;
					}
				} else {
					p.sendMessage("§cThere is already a proposal active for this target.");
					taxChange.remove(p);
					return;
				}
				//something changed so you cant do anything anymore :)
				taxChange.remove(p);
				governmentView.governmentView(p, f, null);
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
			Guild g = FactionManager.getGuildByString(h.getId());
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
					case LAW_VIEW:
						factionView(p, f);
						break;
					case LAW_SELECT:
						lawView(p, f, null);
						break;
					case GOVERNMENT_VIEW:
						factionView(p, f);
						break;
					case PROPOSAL_VIEW:
						governmentView(p, f, null);
						break;
					case LAW_PROPOSAL_VIEW:
						governmentView.proposalView(p, f, null);
						break;
					case LAW_PROPOSAL_SELECT:
						governmentView.lawProposalView(p, f, null);
						break;
					case TAX_PROPOSAL_VIEW:
						governmentView.proposalView(p, f, null);
						break;
					case SPECIFIC_TAX_PROPOSAL_VIEW:
						governmentView.taxProposalView(p, f, null);
						break;
					case PROPOSALS:
						governmentView(p, f, null);
						break;
					case COUNCIL_VIEW:
						governmentView(p, f, null);
						break;
					case COUNCIL_SELECT:
						governmentView.councilView(p, f, null);
						break;
					case LEDGER_VIEW:
						factionView(p, f);
						break;
					default:
						break;
				}
			}
			if(h.getType() == SFGUI.FACTION_LIST || h.getType() == SFGUI.FACTION_VIEW) {
				factionView.click(e, inv, p);
			} else if(h.getType() == SFGUI.GUILD_LIST || h.getType() == SFGUI.GUILD_VIEW) {
				guildView.click(e, inv, p);
			} else if(h.getType() == SFGUI.LAW_VIEW || h.getType() == SFGUI.LAW_SELECT) {
				lawView.click(e, inv, p);
			} else if(h.getType() == SFGUI.MILITARY_VIEW) {
				militaryView.click(e, inv, p);
			} else if(h.getType() == SFGUI.GOVERNMENT_VIEW 
				|| h.getType() == SFGUI.PROPOSAL_VIEW
				|| h.getType() == SFGUI.PROPOSALS
				|| h.getType() == SFGUI.LAW_PROPOSAL_VIEW
				|| h.getType() == SFGUI.LAW_PROPOSAL_SELECT
				|| h.getType() == SFGUI.TAX_PROPOSAL_VIEW
				|| h.getType() == SFGUI.SPECIFIC_TAX_PROPOSAL_VIEW
				|| h.getType() == SFGUI.COUNCIL_VIEW
				|| h.getType() == SFGUI.COUNCIL_SELECT) {
				governmentView.click(e, inv, p);
			} else if(h.getType() == SFGUI.DIPLOMACY_VIEW || h.getType() == SFGUI.ATTITUDE_VIEW || h.getType() == SFGUI.RELATION_VIEW) {
				relationView.click(e, inv, p);
			} else if(h.getType() == SFGUI.TIER_VIEW 
					|| h.getType() == SFGUI.TITLE_VIEW
					|| (inv.getHolder() instanceof SFInventoryHolder && ((SFInventoryHolder) inv.getHolder()).getType().equals(SFGUI.TITLE_TYPE_VIEW))) {
				tierTitleView.click(e, inv, p);
			} else if(h.getType() == SFGUI.WAR_LIST
					|| (inv.getHolder() instanceof WarInventoryHolder && ((WarInventoryHolder) inv.getHolder()).getType().equals(SFGUI.WAR_VIEW))
					|| (inv.getHolder() instanceof SFCombinedInventoryHolder && ((SFCombinedInventoryHolder) inv.getHolder()).getType().equals(SFGUI.PARTICIPANT_VIEW))
					|| (inv.getHolder() instanceof SFCombinedInventoryHolder && ((SFCombinedInventoryHolder) inv.getHolder()).getType().equals(SFGUI.WARGOAL_VIEW))) {
				warView.click(e, inv, p);
			} else if(inv.getHolder() instanceof SFInventoryHolder && ((SFInventoryHolder) inv.getHolder()).getType().equals(SFGUI.TAX_VIEW)) {
				taxView.click(e, inv, p);
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
		}
	}
}
