package me.Plugins.SimpleFactions.Managers.Inventory;

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
import me.Plugins.SimpleFactions.government.election.Candidate;
import me.Plugins.SimpleFactions.government.election.Election;
import me.Plugins.SimpleFactions.government.proposal.TaxTarget;
import me.Plugins.SimpleFactions.keys.Keys;

public class ElectionView {
    public InventoryManager inv;

    ElectionCreator creator = new ElectionCreator();

    public ElectionView(InventoryManager inv) {
		this.inv = inv;
	}

    public void electionView(Player p, Faction f) {
        Inventory i = SimpleFactions.plugin.getServer().createInventory(new SFInventoryHolder(f.getId(), SFGUI.ELECTION_VIEW), 9, "§7Election View");
		Government gov = f.getGovernment();
        int x = 0;
		for(Candidate c : Candidate.values()) {
            if(gov.hasElections(c)) {
                i.setItem(x, creator.createCandidateTypeItem(p, f, c));
                x++;
            }
        }
		p.openInventory(i);
    }

    public void click(InventoryClickEvent e, Inventory inventory, Player p) {
		e.setCancelled(true);
        Faction f = FactionManager.getByLeader(p.getName());
        if(f == null) {
            p.closeInventory();
            return;
        }
		SFInventoryHolder holder = (SFInventoryHolder) inventory.getHolder();
		if(holder.getType() == SFGUI.ELECTION_VIEW) {
			ItemStack item = e.getCurrentItem();
			if(item == null || item.getItemMeta() == null) return;
			ItemMeta meta = item.getItemMeta();
			String id = meta.getPersistentDataContainer().get(Keys.STRING_KEY, PersistentDataType.STRING);
			if(id == null) return;
			try {
				Candidate c = Candidate.valueOf(id);
				Election election = f.getGovernment().getElection();
                if(election.isActive()) {
                    return; //TODO open voting GUI
                } else {
                    if(election.canBeCandidate(c, p.getName())) {
                        election.addCandidate(c, p.getName());
                        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                        p.sendMessage("§aSigned up as a candidate for "+c.getName());
                        electionView(p, f);
                    }
                }
			} catch (Exception ex) {
				// Non-specific tax type, do nothing
			}
		}
	}
}
