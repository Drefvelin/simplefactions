package me.Plugins.SimpleFactions.Managers.Inventory;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.Holder.SFInventoryHolder;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.enums.SFGUI;
import me.Plugins.SimpleFactions.government.Government;
import me.Plugins.SimpleFactions.government.election.Candidate;

public class ElectionView {

    ElectionCreator creator = new ElectionCreator();

    public void electionView(Player p, Faction f) {
        Inventory i = SimpleFactions.plugin.getServer().createInventory(new SFInventoryHolder(f.getId(), SFGUI.GOVERNMENT_VIEW), 54, "§7Government View");
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
}
