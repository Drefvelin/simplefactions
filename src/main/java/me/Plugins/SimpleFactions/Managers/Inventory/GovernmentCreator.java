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
import me.Plugins.SimpleFactions.Objects.Handler.TaxHandler;
import me.Plugins.SimpleFactions.Utils.Formatter;
import me.Plugins.SimpleFactions.Utils.Represents;
import me.Plugins.SimpleFactions.Utils.Wealth;
import me.Plugins.SimpleFactions.enums.Rules;
import me.Plugins.SimpleFactions.enums.Stance;
import me.Plugins.SimpleFactions.government.Council;
import me.Plugins.SimpleFactions.government.Government;
import me.Plugins.SimpleFactions.government.proposal.Proposal;
import me.Plugins.SimpleFactions.government.proposal.TaxLawChange;
import me.Plugins.SimpleFactions.government.proposal.TaxTarget;
import me.Plugins.SimpleFactions.Managers.RelationManager;
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
                lore.add(StringFormatter.formatHex("#d4bb98- "+member + " §7("+Represents.represents(f, member)+")"));
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
        double effect = f.getOrCreateMainGuild().getStabilityModifier(f);
        lore.add(StringFormatter.formatHex("#b8ae61From State: " + ( effect >= 0 ? "#45c46f+" : "#d13530")+Formatter.formatDouble(effect)+"%"));
        if(gov.getStabilityMalusFromCouncil() > 0) {
            lore.add(StringFormatter.formatHex("#b8ae61Council too small: #d13530-"+Formatter.formatDouble(gov.getStabilityMalusFromCouncil())+"%"));
        }
        lore.add(StringFormatter.formatHex("#93c9a7Stances:"));
        for(Guild guild : f.getGuildHandler().getGuilds()) {
            if(guild.isBase()) continue;
            effect = guild.getStabilityModifier(f);
            lore.add(StringFormatter.formatHex(" #d4bb98- "+guild.getName()+" §7("+guild.getType().getName()+"§7): "+guild.getStance(f).getDisplay()+" §7("+( effect >= 0 ? "#45c46f+" : "#d13530")+Formatter.formatDouble(effect)+"%"+"§7)"));
        }
        for(Faction v : RelationManager.getSubjects(f)) {
            Guild guild = v.getOrCreateMainGuild();
            effect = guild.getStabilityModifier(f);
            lore.add(StringFormatter.formatHex(" #d4bb98- "+guild.getName()+" §7(#4269a8Vassal§7): "+guild.getStance(f).getDisplay()+" §7("+( effect >= 0 ? "#45c46f+" : "#d13530")+Formatter.formatDouble(effect)+"%"+"§7)"));
        }
        m.setLore(lore);
        item.setItemMeta(m);
        return item;
    }

    public ItemStack createStanceItem(Faction f, Guild guild) {
        Stance stance = guild.getStance(f);
        ItemStack item = new ItemStack(Material.YELLOW_CONCRETE);
        if(stance == Stance.OPPOSE) item = new ItemStack(Material.RED_CONCRETE);
        else if(stance == Stance.SUPPORT) item = new ItemStack(Material.GREEN_CONCRETE);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(StringFormatter.formatHex(stance.getDisplay()));
        List<String> lore = new ArrayList<String>();
        double effect = guild.getStabilityModifier(f);
        lore.add(StringFormatter.formatHex("#b8ae61Stability Effect: "+( effect >= 0 ? "#45c46f+" : "#d13530")+Formatter.formatDouble(effect)+"%"));
        lore.add("");
        lore.add(StringFormatter.formatHex("#28ed70Click to change"));
        m.setLore(lore);
        m.getPersistentDataContainer().set(Keys.STRING_KEY, PersistentDataType.STRING, guild.getId());
        item.setItemMeta(m);
        return item;
    }

    public ItemStack createTaxTypeItem(Player p, Faction f, TaxTarget target) {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(StringFormatter.formatHex("#93c9a7"+target.getDisplayName()));
        List<String> lore = new ArrayList<String>();
        if(target == TaxTarget.GUILD_ID || target == TaxTarget.VASSAL_ID || target == TaxTarget.TARIFF_ID) {
            lore.add(StringFormatter.formatHex("#28ed70Click to view options"));
        } else {
            Government gov = f.getGovernment();
            Proposal proposal = new Proposal(p.getName(), gov);
            proposal.setTaxProposal(new TaxLawChange(target, "all", 50));
            if(gov.canProposeOrStartMovement(p) && gov.canBeProposed(proposal)) {
                lore.add(StringFormatter.formatHex("#525d5dCurrent Rate: #e3d5a1"+f.getTaxRate(target)+"%"));
                lore.add(StringFormatter.formatHex("#b8ae61Create a proposal to change"));
                lore.add(StringFormatter.formatHex("#b8ae61the rate for #62ca43"+target.getDisplayName()+"."));
            }
            else lore.add(StringFormatter.formatHex("#89504eAnother proposal is active for this target."));
        }
        m.setLore(lore);
        m.getPersistentDataContainer().set(Keys.STRING_KEY, PersistentDataType.STRING, target.name());
        item.setItemMeta(m);
        return item;
    }

    public ItemStack createSpecificTaxItem(Player p, Faction f, String id, TaxTarget target) {
        String name = "";
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        if(target == TaxTarget.GUILD_ID) {
            Guild g = FactionManager.getGuildByString(id);
            if(g == null) return null;
            name = g.getName();
            item = g.getBanner().clone();
        } else if(target == TaxTarget.VASSAL_ID) {
            Faction vassal = FactionManager.getByString(id);
            if(vassal == null) return null;
            name = vassal.getName();
            item = vassal.getBanner().clone();
        } else if(target == TaxTarget.TARIFF_ID) {
            Faction faction = FactionManager.getByString(id);
            if(faction == null) return null;
            name = faction.getName();
            item = faction.getBanner().clone();
        }
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(StringFormatter.formatHex(name));
        List<String> lore = new ArrayList<String>();
        TaxHandler taxHandler = f.getTaxHandler();
        double taxRate = taxHandler.getTaxRate(target, id);
        if(taxHandler.hasSpecificTax(target, null)) {
            lore.add(StringFormatter.formatHex("#525d5dCurrent Rate: #e3d5a1"+taxHandler.getSpecificTax(target, id)+"%"));
            lore.add(StringFormatter.formatHex("#3f4040(#767a77Base Rate: #928d7a"+taxRate+"%#3f4040)"));
        } else {
            lore.add(StringFormatter.formatHex("#812222No specific "+(target == TaxTarget.TARIFF_ID ? "tariff" : "tax")+" set."));
            lore.add(StringFormatter.formatHex("#3f4040(#767a77Base Rate: #928d7a"+taxRate+"%#3f4040)"));
        }
        Government gov = f.getGovernment();
        Proposal proposal = new Proposal(p.getName(), gov);
        proposal.setTaxProposal(new TaxLawChange(target, id, 50));
        if(gov.canProposeOrStartMovement(p) && gov.canBeProposed(proposal)) lore.add(StringFormatter.formatHex("#28ed70Click to propose a change"));
        else lore.add(StringFormatter.formatHex("#89504eAnother proposal is active for this target."));
        m.setLore(lore);
        m.getPersistentDataContainer().set(Keys.STRING_KEY, PersistentDataType.STRING, id);
        m.getPersistentDataContainer().set(Keys.SECONDARY_STRING_KEY, PersistentDataType.STRING, target.name());
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

    public ItemStack createCouncilMemberItem(Player player, Faction f, int slot) {
        Council council = f.getGovernment().getCouncil();
        List<String> members = council.getMembers();
        
        String memberName = slot < members.size() ? members.get(slot) : null;
        boolean isEmpty = memberName == null;
        boolean isLeader = player.getName().equalsIgnoreCase(f.getLeader());
        boolean canModify = isLeader && (
            council.getType().equals(Rules.APPOINTED_COUNCIL) ||
            council.getType().equals(Rules.WEALTH_BASED_COUNCIL) ||
            council.getType().equals(Rules.ELECTED_COUNCIL)
        );
        
        ItemStack item;
        if(isEmpty) {
            item = canModify ? 
                new ItemStack(Material.GREEN_CONCRETE) : 
                new ItemStack(Material.YELLOW_CONCRETE);
        } else {
            item = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta skullMeta = item.getItemMeta();
            if(skullMeta instanceof org.bukkit.inventory.meta.SkullMeta) {
                ((org.bukkit.inventory.meta.SkullMeta) skullMeta).setOwner(memberName);
            }
            item.setItemMeta(skullMeta);
        }
        
        ItemMeta m = item.getItemMeta();
        
        if(isEmpty) {
            m.setDisplayName(StringFormatter.formatHex("#89504eEmpty Seat"));
            List<String> lore = new ArrayList<>();
            if(canModify) {
                lore.add(StringFormatter.formatHex("#28ed70Click to appoint a member"));
            } else {
                lore.add(StringFormatter.formatHex("#525d5dWaiting for appointment"));
            }
            m.setLore(lore);
        } else {
            m.setDisplayName(StringFormatter.formatHex("#93c9a7" + memberName));
            List<String> lore = new ArrayList<>();
            
            // Display wealth and ranking
            double wealth = Wealth.wealth(memberName);
            List<String> topByWealth = Wealth.topWealth(f, true);
            int ranking = topByWealth.indexOf(memberName) + 1;
            lore.add(StringFormatter.formatHex("#499eccRepresents§7: "+Represents.represents(f, memberName)));
            lore.add(StringFormatter.formatHex("#85c265Wealth§7: #ccbb76" + Formatter.formatDouble(wealth)+"d"));
            lore.add(StringFormatter.formatHex("#85c265Ranking§7: #7a706a" + ranking + "/" + f.getMembers().size()));
            lore.add("");
            
            // Display why they have their seat
            Rules councilType = council.getType();
            if(councilType.equals(Rules.APPOINTED_COUNCIL)) {
                lore.add(StringFormatter.formatHex("#b8ae61Appointed Member"));
            } else if(councilType.equals(Rules.WEALTH_BASED_COUNCIL)) {
                lore.add(StringFormatter.formatHex("#b8ae61Wealth-Based Selection"));
            } else if(councilType.equals(Rules.ELECTED_COUNCIL)) {
                lore.add(StringFormatter.formatHex("#b8ae61Elected Member"));
            }
            
            lore.add("");
            
            // Add modify option if leader
            if(canModify) {
                lore.add(StringFormatter.formatHex("#28ed70Click to replace"));
            }
            
            m.setLore(lore);
        }
        
        // Store member name and slot in persistent data
        m.getPersistentDataContainer().set(Keys.STRING_KEY, PersistentDataType.STRING, 
            memberName != null ? memberName : "");
        m.getPersistentDataContainer().set(Keys.SECONDARY_STRING_KEY, PersistentDataType.STRING, 
            String.valueOf(slot));
        
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
                            .previewLawIncomeExact(f, group, law);
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
            TaxLawChange taxChange = proposal.getTaxChange();
            TaxTarget target = taxChange.getTarget();
            String name = "";
            String type = "";
            if(target == TaxTarget.GUILD_ID) {
                Guild guild = FactionManager.getGuildByString(taxChange.getId());
                name = guild.getName();
                type = guild.getType().getName();
            } else if(target == TaxTarget.VASSAL_ID) {
                name = FactionManager.getByString(taxChange.getId()).getName();
                type = "#4269a8Vassal";
            } else if(target == TaxTarget.TARIFF_ID) {
                name = FactionManager.getByString(taxChange.getId()).getName();
                type = "#79bf6dTariff";
            } else{
                name = target.getDisplayName();
            }
            String oldRate = "";
            if(target == TaxTarget.GUILD_ID || target == TaxTarget.VASSAL_ID || target == TaxTarget.TARIFF_ID) {
                double rate = f.getTaxRate(target, taxChange.getId());
                if(rate == -1.0) {
                    oldRate = String.valueOf(f.getTaxRate(target, null));
                } else {
                    oldRate = String.valueOf(rate);
                }
            } else {
                oldRate = String.valueOf(f.getTaxRate(target));
            }
            double baseRate = f.getTaxRate(target, null);
            lore.add(StringFormatter.formatHex("#b8ae61Target: #c2bea7"+name+
                (type.isEmpty() ? "" : " §7("+type+"§7)")));
            lore.add(StringFormatter.formatHex("#b8ae61Change: #c2bea7"+oldRate+"% §7-> #c2bea7"+taxChange.getNewTax()+"%"));
            if(target == TaxTarget.GUILD_ID || target == TaxTarget.VASSAL_ID || target == TaxTarget.TARIFF_ID) {
                lore.add(StringFormatter.formatHex("#3f4040(#767a77Base Rate: #928d7a"+baseRate+"%#3f4040)"));
            }
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
