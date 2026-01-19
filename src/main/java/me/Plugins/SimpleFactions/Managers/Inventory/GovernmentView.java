package me.Plugins.SimpleFactions.Managers.Inventory;

import java.util.List;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.InventoryManager;
import me.Plugins.SimpleFactions.Managers.RelationManager;
import me.Plugins.SimpleFactions.Managers.Holder.SFInventoryHolder;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.enums.SFGUI;
import me.Plugins.SimpleFactions.government.Council;
import me.Plugins.SimpleFactions.government.Government;
import me.Plugins.SimpleFactions.government.proposal.Proposal;
import me.Plugins.SimpleFactions.government.proposal.TaxTarget;
import me.Plugins.SimpleFactions.keys.Keys;
import me.Plugins.SimpleFactions.laws.Law;
import me.Plugins.SimpleFactions.laws.LawGroup;

public class GovernmentView {
    public InventoryManager inv;
	
	public GovernmentCreator creator = new GovernmentCreator();
	public LawCreator lawCreator = new LawCreator();

	private static final List<Integer> SLOTS = List.of(
		10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25
	);
	
	public GovernmentView(InventoryManager inv) {
        this.inv = inv;
    }

    public void governmentView(Player player, Faction f, Inventory i) {
        boolean open = i == null;
		if(i == null) i = SimpleFactions.plugin.getServer().createInventory(new SFInventoryHolder(f.getId(), SFGUI.GOVERNMENT_VIEW), 54, "§7Government View");
		i.clear();
		i.setItem(10, creator.createGovernmentItem(f));
		i.setItem(11, creator.createStabilityItem(f));
		i.setItem(12, creator.createCouncilItem(f));
		i.setItem(15, creator.createProposalItem(player, f));
		i.setItem(24, creator.createProposalListItem(player, f));
		Government gov = f.getGovernment();
		
		Guild g = FactionManager.getGuildByMember(player.getName());
		if(g != null && gov.canAffectStability(g)) {
			i.setItem(28, creator.createStanceItem(f, g));
		}
		i.setItem(53, inv.createBackButton(SFGUI.GOVERNMENT_VIEW));
        if(open) player.openInventory(i);
	}

	public void councilView(Player player, Faction f, Inventory i) {
        boolean open = i == null;
		if(i == null) i = SimpleFactions.plugin.getServer().createInventory(new SFInventoryHolder(f.getId(), SFGUI.COUNCIL_VIEW), 54, "§7Council");
		i.clear();
		int x = 0;
		for(int slot : SLOTS) {
			if(x >= f.getGovernment().getCouncil().getMembers().size()) break;
			i.setItem(slot, creator.createCouncilMemberItem(player, f, x));
			x++;
		}
		i.setItem(53, inv.createBackButton(SFGUI.COUNCIL_VIEW));
        if(open) player.openInventory(i);
	}

	public void proposalList(Player player, Faction f, Inventory i) {
		boolean open = i == null;
		if(i == null) i = SimpleFactions.plugin.getServer().createInventory(new SFInventoryHolder(f.getId(), SFGUI.PROPOSALS), 27, "§7Proposals");
		i.clear();
		for(Proposal proposal : f.getGovernment().getCouncil().getProposalHandler().getProposals()) {
			i.addItem(creator.createCurrentProposalItem(player, f, proposal));
		}
		i.setItem(26, inv.createBackButton(SFGUI.PROPOSALS));
		if(open) player.openInventory(i);
	}

	public void proposalView(Player player, Faction f, Inventory i) {
		boolean open = i == null;
		if(i == null) i = SimpleFactions.plugin.getServer().createInventory(new SFInventoryHolder(f.getId(), SFGUI.PROPOSAL_VIEW), 9, "§7Select Proposal Type");
		i.clear();
		i.setItem(0, creator.createProposalTypeItem("law"));
		i.setItem(1, creator.createProposalTypeItem("tax"));
		i.setItem(8, inv.createBackButton(SFGUI.PROPOSAL_VIEW));
		if(open) player.openInventory(i);
	}

	public void taxProposalView(Player player, Faction f, Inventory i) {
		boolean open = i == null;
		if(i == null) i = SimpleFactions.plugin.getServer().createInventory(new SFInventoryHolder(f.getId(), SFGUI.TAX_PROPOSAL_VIEW), 9, "§7Select Tax Type");
		i.clear();
		int x = 0;
		for(TaxTarget target : TaxTarget.values()) {
			if(!f.getTaxHandler().canCollectTax(target)) continue;
			i.setItem(x, creator.createTaxTypeItem(player, f, target));
			x++;
		}
		i.setItem(8, inv.createBackButton(SFGUI.TAX_PROPOSAL_VIEW));
		if(open) player.openInventory(i);
	}

	public void specificTaxProposalView(Player player, Faction f, Inventory i, TaxTarget target) {
		boolean open = i == null;
		if(i == null) i = SimpleFactions.plugin.getServer().createInventory(new SFInventoryHolder(f.getId(), SFGUI.SPECIFIC_TAX_PROPOSAL_VIEW), 54, "§7Select Target");
		i.clear();
		int x = 0;
		if(target == TaxTarget.GUILD_ID) {
			for(Guild g : f.getGuildHandler().getGuilds()) {
				if(g.isBase()) continue;
				i.setItem(x, creator.createSpecificTaxItem(player, f, g.getId(), target));
				if(x >= 53) break;
				x++;
			}
		} else if(target == TaxTarget.VASSAL_ID) {
			for(Faction s : RelationManager.getSubjects(f)) {
				i.setItem(x, creator.createSpecificTaxItem(player, f, s.getId(), target));
				if(x >= 53) break;
				x++;
			}
		} else if(target == TaxTarget.TARIFF_ID) {
			for(Faction fac : FactionManager.factions) {
				if(fac.getId().equalsIgnoreCase(f.getId())) continue;
				if(RelationManager.sameRealm(fac, f)) continue;
				if(x >= 53) break;
				i.setItem(x, creator.createSpecificTaxItem(player, f, fac.getId(), target));
				x++;
			}
		}
		i.setItem(53, inv.createBackButton(SFGUI.SPECIFIC_TAX_PROPOSAL_VIEW));
		if(open) player.openInventory(i);
	}

	public void lawProposalView(Player player, Faction f, Inventory i) {
		if(i == null) i = SimpleFactions.plugin.getServer().createInventory(new SFInventoryHolder(f.getId(), SFGUI.LAW_PROPOSAL_VIEW), 54, "§7Select Law Group");
		i.clear();
		for(int x = 0; x<f.getLawHandler().getGroupList().size(); x++) {
			LawGroup group = f.getLawHandler().getGroupList().get(x);
			i.setItem(SLOTS.get(x), lawCreator.createLawGroupItem(player, f, group));
		}
		player.openInventory(i);
		i.setItem(53, inv.createBackButton(SFGUI.LAW_PROPOSAL_VIEW));
	}

	public void lawProposalSelect(Player player, Faction f, LawGroup group, Inventory i) {
		if(i == null) i = SimpleFactions.plugin.getServer().createInventory(new SFInventoryHolder(f.getId(), SFGUI.LAW_PROPOSAL_SELECT), 27, "§7Select Law");
		i.clear();
		int slot = 0;
		for(Law law : group.getLaws().values()) {
			i.setItem(slot, lawCreator.createLawItem(player, f, group, law, true));
			slot++;
		}
		player.openInventory(i);
		i.setItem(26, inv.createBackButton(SFGUI.LAW_PROPOSAL_SELECT));
	}

	public void click(InventoryClickEvent e, Inventory inventory, Player p) {
		SFInventoryHolder h = (SFInventoryHolder) inventory.getHolder();
		if (h.getType() == SFGUI.GOVERNMENT_VIEW) {
			e.setCancelled(true);
			int slot = e.getSlot();
			ItemStack item = e.getCurrentItem();
			Faction f = FactionManager.getByString(((SFInventoryHolder)e.getInventory().getHolder()).getId());
			if(slot == 28) {
				String id = item.getItemMeta().getPersistentDataContainer().get(Keys.STRING_KEY, PersistentDataType.STRING);
				if(id != null) {
					Guild g = FactionManager.getGuildByString(id);
					if(g != null) {
						g.switchStance();
						governmentView(p, f, inventory);
						p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
					}
				}
			} else if(slot == 15) {
				Government gov = f.getGovernment();
				if(!gov.canProposeOrStartMovement(p)) {
					p.playSound(p, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
					return;
				}
				proposalView(p, f, null);
				p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
			} else if(slot == 24) {
				proposalList(p, f, null);
				p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
			} else if(slot == 12) {
				Government gov = f.getGovernment();
				if(!gov.hasCouncil()) return;
				councilView(p, f, null);
				p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
			}
		} else if (h.getType() == SFGUI.PROPOSAL_VIEW) {
			e.setCancelled(true);
			int slot = e.getSlot();
			Faction f = FactionManager.getByString(((SFInventoryHolder)e.getInventory().getHolder()).getId());
			if(slot == 0) {
				lawProposalView(p, f, null);
				p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
			} else if(slot == 1) {
				taxProposalView(p, f, null);
				p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
			}
		} else if (h.getType() == SFGUI.TAX_PROPOSAL_VIEW) {
			e.setCancelled(true);
			ItemStack item = e.getCurrentItem();
			ItemMeta meta = item.getItemMeta();
			if(!(inventory.getHolder() instanceof SFInventoryHolder)) return;
			SFInventoryHolder holder = (SFInventoryHolder) inventory.getHolder();
			Faction f = FactionManager.getByString(holder.getId());
			if(f == null) return;
			String id = meta.getPersistentDataContainer().get(Keys.STRING_KEY, PersistentDataType.STRING);
			if(id == null) return;
			try {
				TaxTarget target = TaxTarget.valueOf(id);
				if(target == TaxTarget.GUILD_ID) {
					specificTaxProposalView(p, f, null, TaxTarget.GUILD_ID);
					p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
					return;
				} else if(target == TaxTarget.VASSAL_ID) {
					specificTaxProposalView(p, f, null, TaxTarget.VASSAL_ID);
					p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
					return;
				} else if(target == TaxTarget.TARIFF_ID) {
					specificTaxProposalView(p, f, null, TaxTarget.TARIFF_ID);
					p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
					return;
				}
				inv.setChanging(f, p, target, "all");
				p.sendTitle("§aTax Change", "§eType a new tax for "+target.getDisplayName()+" §ein chat.", 20, 40, 20);
				p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
				p.closeInventory();
			} catch (Exception ex) {
				// TODO: handle exception
			}
		} else if (h.getType() == SFGUI.SPECIFIC_TAX_PROPOSAL_VIEW) {
			e.setCancelled(true);
			ItemStack item = e.getCurrentItem();
			ItemMeta meta = item.getItemMeta();
			if(!(inventory.getHolder() instanceof SFInventoryHolder)) return;
			SFInventoryHolder holder = (SFInventoryHolder) inventory.getHolder();
			Faction f = FactionManager.getByString(holder.getId());
			if(f == null) return;
			String id = meta.getPersistentDataContainer().get(Keys.STRING_KEY, PersistentDataType.STRING);
			if(id == null) return;

			String t = meta.getPersistentDataContainer().get(Keys.SECONDARY_STRING_KEY, PersistentDataType.STRING);
			if(t == null) return;
			TaxTarget target = TaxTarget.valueOf(t);
			String name = target == TaxTarget.GUILD_ID ? FactionManager.getGuildByString(id).getName() : FactionManager.getByString(id).getName();
			inv.setChanging(f, p, target, id);
			p.sendTitle("§aTax Change", "§eType a new tax for "+name+" §ein chat.", 20, 40, 20);
			p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
			p.closeInventory();
		} else if (h.getType() == SFGUI.LAW_PROPOSAL_VIEW) {
			e.setCancelled(true);
			ItemStack item = e.getCurrentItem();
			ItemMeta meta = item.getItemMeta();
			if(!(inventory.getHolder() instanceof SFInventoryHolder)) return;
			SFInventoryHolder holder = (SFInventoryHolder) inventory.getHolder();
			Faction f = FactionManager.getByString(holder.getId());
			if(f == null) return;
			String id = meta.getPersistentDataContainer().get(Keys.STRING_KEY, PersistentDataType.STRING);
			if(id == null) return;
			LawGroup group = f.getLawHandler().getGroup(id);
			if(group == null) return;
			lawProposalSelect(p, f, group, null);
			p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
		} else if (h.getType() == SFGUI.LAW_PROPOSAL_SELECT) {
			e.setCancelled(true);
			ItemStack item = e.getCurrentItem();
			ItemMeta meta = item.getItemMeta();
			if(!(inventory.getHolder() instanceof SFInventoryHolder)) return;
			SFInventoryHolder holder = (SFInventoryHolder) inventory.getHolder();
			Faction f = FactionManager.getByString(holder.getId());
			if(f == null) return;
			String id = meta.getPersistentDataContainer().get(Keys.STRING_KEY, PersistentDataType.STRING);
			if(id == null) return;
			Law law = f.getLawHandler().getLaw(id);
			if(law == null) return;
			Government gov = f.getGovernment();
			Proposal proposal = new Proposal(p.getName(), gov);
			proposal.setLawProposal(law);
			if(gov.canPropose(p)) {
				if(!gov.canBeProposed(proposal)) {
					p.playSound(p, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
					return;
				}
				gov.propose(proposal);
				p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
				governmentView(p, f, null);;
			} else if(gov.canProposeOrStartMovement(p)) {
				//TODO: Movement creation
			}
		} else if (h.getType() == SFGUI.PROPOSALS) {
			e.setCancelled(true);
		} else if (h.getType() == SFGUI.COUNCIL_VIEW) {
			e.setCancelled(true);
			int slot = e.getSlot();
			ItemStack item = e.getCurrentItem();
			if(item == null) return;
			Faction f = FactionManager.getByString(((SFInventoryHolder)e.getInventory().getHolder()).getId());
			if(f == null) return;
			boolean isLeader = p.getName().equalsIgnoreCase(f.getLeader());
			if(!isLeader) return;
			// Handle council member click for replace/appoint
			ItemMeta meta = item.getItemMeta();
			if(meta == null) return;
			String slotStr = meta.getPersistentDataContainer().get(Keys.SECONDARY_STRING_KEY, PersistentDataType.STRING);
			if(slotStr == null) return;
			// TODO: Handle member selection/replacement
			p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
		}
	}
}
