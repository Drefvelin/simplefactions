package me.Plugins.SimpleFactions.Managers.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Utils.Formatter;
import me.Plugins.SimpleFactions.enums.Stance;
import me.Plugins.SimpleFactions.government.Government;
import me.Plugins.SimpleFactions.government.proposal.Proposal;
import me.Plugins.SimpleFactions.keys.Keys;
import me.Plugins.SimpleFactions.laws.Law;
import me.Plugins.SimpleFactions.laws.LawGroup;
import me.Plugins.TLibs.TLibs;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public class GovernmentCreator {
    public ItemStack createGovernmentItem(Faction f) {
        ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(StringFormatter.formatHex("#93c9a7Government:"));
        List<String> lore = new ArrayList<String>();
        Government gov = f.getGovernment();
        lore.add(StringFormatter.formatHex("#9c9775§l"+f.getRulerTitle()+": #c2bea7"+f.getLeader()));
        lore.add(StringFormatter.formatHex("#85c265Administrative Power§7: §e"+Formatter.formatDouble(gov.getPower())+"/"+Formatter.formatDouble(gov.getMaxPower())+" §7(§e+"
                +Formatter.formatDouble(gov.getPowerGain())+"§7/day)"));
        lore.add(" ");
        lore.add(StringFormatter.formatHex("#b8ae61Ruling System: #d4c9ae"+f.getGovernmentString()));
        lore.add(StringFormatter.formatHex("#b8ae61Leader Elections: "+(gov.hasLeaderElections() ? "#45afc4✔" : "#c74d32✖")));
        m.setLore(lore);
        item.setItemMeta(m);
        return item;
    }

    public ItemStack createCouncilItem(Faction f) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(StringFormatter.formatHex("#93c9a7Council:"));
        List<String> lore = new ArrayList<String>();
        Government gov = f.getGovernment();
        lore.add(StringFormatter.formatHex("#85c265Council Type§7: #45c46f"+gov.getCouncil().getType().getDisplay()));
        lore.add(StringFormatter.formatHex("#85c265Council Size§7: §e"+gov.getCouncil().getCurrentSize()+"/"+gov.getCouncil().getMaxSize()));
        lore.add(" ");
        lore.add(StringFormatter.formatHex("#b8ae61Council Elections: "+(gov.hasCouncilElections() ? "#45afc4✔" : "#c74d32✖")));
        if(gov.getCouncil().getCurrentSize() > 0) {
            lore.add(StringFormatter.formatHex("#93c9a7Members:"));
            for(String member : gov.getCouncilMembers()) {
                if(member.equalsIgnoreCase(f.getLeader())) continue;
                lore.add(StringFormatter.formatHex("#d4bb98- "+member));
            }
        }
        m.setLore(lore);
        item.setItemMeta(m);
        return item;
    }

    public ItemStack createStabilityItem(Faction f) {
        ItemStack item = new ItemStack(Material.BLACK_DYE);
        ItemMeta m = item.getItemMeta();
        m.setCustomModelData(21);
        Government gov = f.getGovernment();
        m.setDisplayName(StringFormatter.formatHex("#85c265Stability§7: §e"+gov.getStabilityString()+"%"));
        List<String> lore = new ArrayList<String>();
        lore.add(StringFormatter.formatHex("#b8ae61Base: #45c46f+"+Formatter.formatDouble(gov.STABILITY_BASE)+"%"));
        double effect = f.getOrCreateMainGuild().getStabilityModifier();
        lore.add(StringFormatter.formatHex("#b8ae61From State: " + ( effect >= 0 ? "#45c46f+" : "#d13530")+Formatter.formatDouble(effect)+"%"));
        lore.add(StringFormatter.formatHex("#93c9a7Guild Effects:"));
        for(Guild guild : f.getGuildHandler().getGuilds()) {
            if(guild.isBase()) continue;
            effect = guild.getStabilityModifier();
            lore.add(StringFormatter.formatHex(" #d4bb98- "+guild.getName()+" §7("+guild.getType().getName()+"§7): "+guild.getStance().getDisplay()+" §7("+( effect >= 0 ? "#45c46f+" : "#d13530")+Formatter.formatDouble(effect)+"%"+"§7)"));
        }
        m.setLore(lore);
        item.setItemMeta(m);
        return item;
    }

    public ItemStack createStanceItem(Faction f, Guild guild) {
        Stance stance = guild.getStance();
        ItemStack item = new ItemStack(Material.YELLOW_CONCRETE);
        if(stance == Stance.OPPOSE) item = new ItemStack(Material.RED_CONCRETE);
        else if(stance == Stance.SUPPORT) item = new ItemStack(Material.GREEN_CONCRETE);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(StringFormatter.formatHex(stance.getDisplay()));
        List<String> lore = new ArrayList<String>();
        double effect = guild.getStabilityModifier();
        lore.add(StringFormatter.formatHex("#b8ae61Stability Effect: "+( effect >= 0 ? "#45c46f+" : "#d13530")+Formatter.formatDouble(effect)+"%"));
        lore.add("");
        lore.add(StringFormatter.formatHex("#28ed70Click to change"));
        m.setLore(lore);
        m.getPersistentDataContainer().set(Keys.STRING_KEY, PersistentDataType.STRING, guild.getId());
        item.setItemMeta(m);
        return item;
    }

    public ItemStack createProposalItem(Player p, Faction f) {
        ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta m = item.getItemMeta();
        Government gov = f.getGovernment();
        m.setDisplayName(StringFormatter.formatHex(gov.isCouncilMember(p) ? "#28ed70New Proposal" : "#89504eNew Movement"));
        List<String> lore = new ArrayList<String>();
        if(gov.isCouncilMember(p)) {
            lore.add(StringFormatter.formatHex("#b8ae61Create a new proposal to change"));
            lore.add(StringFormatter.formatHex("#b8ae61your faction's laws or taxes."));
            lore.add(StringFormatter.formatHex("#525d5dCurrently Active Proposals: #e3d5a1"+gov.getCouncil().getCurrentProposals(p.getName())+"/2"));
        } else {
            lore.add(StringFormatter.formatHex("#b8ae61Create a new movement to demand"));
            lore.add(StringFormatter.formatHex("#b8ae61changes to your faction's laws or taxes."));
            lore.add(StringFormatter.formatHex("#b8ae61With enough support you can send an"));
            lore.add(StringFormatter.formatHex("#b8ae61ultimatum to the council."));
            lore.add(StringFormatter.formatHex("§7(#812222Potential Civil War§7)"));
        }
        if(gov.canProposeOrStartMovement(p)) lore.add(StringFormatter.formatHex("#28ed70Click to start"));
        else lore.add(StringFormatter.formatHex("#89504eYou cannot start a new proposal/movement at this time."));
        m.setLore(lore);
        item.setItemMeta(m);
        return item;
    }

    public ItemStack createProposalListItem(Player p, Faction f) {
        ItemStack item = new ItemStack(Material.BOOKSHELF);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(StringFormatter.formatHex("#93c9a7Current Proposals"));
        List<String> lore = new ArrayList<String>();
        Government gov = f.getGovernment();
        int count = gov.getCouncil().getProposalHandler().getProposals().size();
        lore.add(StringFormatter.formatHex("#85c265Active Proposals§7: §e"+count));
        lore.add("");
        lore.add(StringFormatter.formatHex("#28ed70Click to view"));
        m.setLore(lore);
        item.setItemMeta(m);
        return item;
    }

    public ItemStack createCurrentProposalItem(Player p, Faction f, Proposal proposal) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(StringFormatter.formatHex(proposal.isLawProposal() ? "#93c9a7Law Proposal" : "#93c9a7Tax Proposal"));
        List<String> lore = new ArrayList<String>();
        lore.add(StringFormatter.formatHex("#85c265Proposed by: #c2bea7"+proposal.getProposer()));
        if(proposal.isLawProposal()) {
            Law law = proposal.getLaw();
            LawGroup group = f.getLawHandler().getGroupByLaw(law.getId());
            lore.add(StringFormatter.formatHex("#b8ae61Group: #c2bea7"+group.getName()));
            lore.add(StringFormatter.formatHex(group.getCurrent().getName()+" §7-> "+law.getName()));
            // ---- Economic preview ----
            if (law.affectsEconomy()) {
                Guild us = FactionManager.getGuildByMember(p.getName());
                if (us != null) {
                    Map<Guild, Double> deltas =
                        SimpleFactions.getInstance()
                            .getProvinceManager()
                            .previewLawIncomeExact(group, law);
                    lore.add("");
                    lore.add(StringFormatter.formatHex("#a6c793Estimated Economic Impact:"));

                    boolean shownAny = false;

                    // ---- Our guild first ----
                    Double ourDelta = deltas.get(us);
                    if (ourDelta != null && Math.abs(ourDelta) > 0) {
                        lore.add(StringFormatter.formatHex(
                            "  " + us.getName() + "§7: " +
                            (ourDelta > 0 ? "#87d65c+" : "#d65c5c") +
                            String.format("%.2f", ourDelta) +
                            "d/day"
                        ));
                        shownAny = true;
                    }
                    lore.add("");
                    lore.add(StringFormatter.formatHex("#78856dOther Notable Impacts:"));
                    // ---- Other most impacted guilds ----
                    deltas.entrySet().stream()
                        .filter(e -> !e.getKey().equals(us))
                        .filter(e -> Math.abs(e.getValue()) > 0)
                        .sorted((a, b) ->
                            Double.compare(Math.abs(b.getValue()), Math.abs(a.getValue()))
                        )
                        .limit(5)
                        .forEach(e -> {
                            lore.add(StringFormatter.formatHex(
                                "  " + e.getKey().getName() + " §7("+e.getKey().getFaction().getName()+"§7): " +
                                (e.getValue() > 0 ? "#87d65c+" : "#d65c5c") +
                                String.format("%.2f", e.getValue()) +
                                "d/day"
                            ));
                        });

                    if (!shownAny && deltas.values().stream().allMatch(v -> Math.abs(v) == 0)) {
                        lore.add(StringFormatter.formatHex("  #9cb68cNo economic change"));
                    }
                }
            }
        } else if(proposal.isTaxProposal()) {
            lore.add(StringFormatter.formatHex("#b8ae61Target: #c2bea7"+proposal.getTaxChange().getTarget()));
            //lore.add(StringFormatter.formatHex("#b8ae61Change: #c2bea7"+proposal.getTaxChange().getOldRate()+"% §7-> "+proposal.getTaxChange().getNewRate()+"%"));
        }
        m.setLore(lore);
        item.setItemMeta(m);
        return item;
    }

    public ItemStack createProposalTypeItem(String type) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta m = item.getItemMeta();
        if(type.equalsIgnoreCase("law")) {
            m.setDisplayName(StringFormatter.formatHex("#93c9a7Law Proposal"));
            List<String> lore = new ArrayList<String>();
            lore.add(StringFormatter.formatHex("#b8ae61Create a proposal to change"));
            lore.add(StringFormatter.formatHex("#b8ae61a law in your faction."));
            m.setLore(lore);
        } else if(type.equalsIgnoreCase("tax")) {
            m.setDisplayName(StringFormatter.formatHex("#93c9a7Tax Proposal"));
            List<String> lore = new ArrayList<String>();
            lore.add(StringFormatter.formatHex("#b8ae61Create a proposal to change"));
            lore.add(StringFormatter.formatHex("#b8ae61the tax rate in your faction."));
            m.setLore(lore);
        }
        item.setItemMeta(m);
        return item;
    }
}
