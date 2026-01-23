package me.Plugins.SimpleFactions.Managers.Inventory;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Utils.Represents;
import me.Plugins.SimpleFactions.government.Government;
import me.Plugins.SimpleFactions.government.election.Candidate;
import me.Plugins.SimpleFactions.government.election.Election;
import me.Plugins.TLibs.TLibs;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public class ElectionCreator {
    public ItemStack createCandidateTypeItem(Player p, Faction f, Candidate c) {
        Election e = f.getGovernment().getElection();
        ItemStack item = TLibs.getItemAPI().getCreator().getItemFromPath("ia.iasurvival:letter");
        ItemMeta m = item.getItemMeta();
        Government gov = f.getGovernment();
        m.setDisplayName(StringFormatter.formatHex("#84c468"+c.getName()+" Candidates"));
        List<String> lore = new ArrayList<String>();
        if(!gov.hasElection()) {
            lore.add(StringFormatter.formatHex("#ab483fCannot cast votes yet"));
            lore.add(StringFormatter.formatHex("#ad9072Next Election: #e3d5a1"+gov.getTimeUntilNextElection()));
            lore.add("");
        }
        for(String candidate : e.getCandidates(c)) {
            lore.add(StringFormatter.formatHex("§f- #e3d5a1"+candidate + Represents.represents(f, candidate)));
        }
        if(!e.canBeCandidate(c, p.getName())) {
            lore.add("");
            e.applyReasons(lore, c, p);
        }
        m.setLore(lore);
        item.setItemMeta(m);
        return item;
    }
}
