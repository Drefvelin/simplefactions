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
import me.Plugins.SimpleFactions.Managers.Holder.SFInventoryHolder;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.enums.SFGUI;
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

	private static final List<Integer> LAW_SLOTS = List.of(
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
		Guild g = FactionManager.getGuildByMember(player.getName());
		if(g != null && !g.isBase()) {
			i.setItem(28, creator.createStanceItem(f, g));
		}
		i.setItem(53, inv.createBackButton(SFGUI.GOVERNMENT_VIEW));
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
		if(i == null) i = SimpleFactions.plugin.getServer().createInventory(new SFInventoryHolder(f.getId(), SFGUI.TAX_PROPOSAL_VIEW), 9, "§7Select Proposal Type");
		i.clear();
		int x = 0;
		for(TaxTarget target : TaxTarget.values()) {
			i.setItem(x, creator.createTaxTypeItem(f, target));
			x++;
		}
		i.setItem(8, inv.createBackButton(SFGUI.TAX_PROPOSAL_VIEW));
		if(open) player.openInventory(i);
	}

	public void lawProposalView(Player player, Faction f, Inventory i) {
		if(i == null) i = SimpleFactions.plugin.getServer().createInventory(new SFInventoryHolder(f.getId(), SFGUI.LAW_PROPOSAL_VIEW), 54, "§7Select Law Group");
		i.clear();
		for(int x = 0; x<f.getLawHandler().getGroupList().size(); x++) {
			LawGroup group = f.getLawHandler().getGroupList().get(x);
			i.setItem(LAW_SLOTS.get(x), lawCreator.createLawGroupItem(player, f, group));
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
				p.closeInventory();
				p.sendTitle("§aAdded Proposal","", 20, 80, 20);
			} else if(gov.canProposeOrStartMovement(p)) {
				//TODO: Movement creation
			}
		} else if (h.getType() == SFGUI.PROPOSALS) {
			e.setCancelled(true);
		}
	}
}
