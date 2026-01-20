package me.Plugins.SimpleFactions.government.proposal;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import java.util.ArrayList;
import java.util.List;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Utils.EconomicImpact;
import me.Plugins.SimpleFactions.laws.LawGroup;

import me.Plugins.SimpleFactions.government.Government;
import me.Plugins.SimpleFactions.laws.Law;

public class Proposal {
    private Government gov;
    private String proposer;

    private Law law;
    private TaxLawChange tax;

    public Proposal(String proposer,Government gov) {
        this.gov = gov;
        this.proposer = proposer;
    }

    public String getProposer() {
        return proposer;
    }

    public void apply() {
        if (isLawProposal()) {
            LawGroup group = gov.getFaction().getLawHandler().getGroup(law.getGroup());
            gov.getFaction().applyLaw(law, group);
        } else if (isTaxProposal()) {
            TaxTarget target = tax.getTarget();
            gov.getFaction().getTaxHandler().setTaxRate(target, tax.getId(), tax.getNewTax());
        }
    }

    public boolean isLawProposal() {
        return law != null;
    }
    public Law getLaw() {
        return law;
    }
    public void setLawProposal(Law law) {
        this.law = law;
    }
    public boolean isTaxProposal() {
        return tax != null;
    }
    public TaxLawChange getTaxChange() {
        return tax;
    }
    public void setTaxProposal(TaxLawChange tax) {
        this.tax = tax;
    }

    public boolean affectsEconomy() {
        if (isLawProposal()) {
            return law.affectsEconomy();
        } else if (isTaxProposal()) {
            return true;
        }
        return false;
    }

    public ItemStack getAsBook() {
        return getAsBook(null);
    }

    public ItemStack getAsBook(Player p) {
        ItemStack item = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) item.getItemMeta();

        String title = isLawProposal() ? "Law Proposal" : "Tax Proposal";
        if (title.length() > 32) title = title.substring(0, 32);
        meta.setTitle(title);
        meta.setAuthor(proposer != null ? proposer : "Unknown");

        Faction f = gov != null ? gov.getFaction() : null;

        // First page: basic info
        List<String> first = new ArrayList<>();
        first.add(StringFormatter.formatHex(isLawProposal() ? "#93c9a7Law Proposal" : "#93c9a7Tax Proposal"));
        first.add("");
        first.add(StringFormatter.formatHex("#85c265Proposed by: #c2bea7" + proposer));

        if (isLawProposal() && law != null) {
            LawGroup group = f != null ? f.getLawHandler().getGroup(law.getGroup()) : null;
            String groupName = group != null ? group.getName() : (law.getGroup() != null ? law.getGroup() : "unknown");
            first.add(StringFormatter.formatHex("#b8ae61Group: #c2bea7" + groupName));
            if (group != null && group.getCurrent() != null) {
                first.add(StringFormatter.formatHex(group.getCurrent().getName() + " §7-> " + law.getName()));
            } else {
                first.add(StringFormatter.formatHex(law.getName()));
            }
        } else if (isTaxProposal() && tax != null) {
            TaxTarget target = tax.getTarget();
            String name = target != null ? target.getDisplayName() : "Unknown";
            String type = "";
            if (target == TaxTarget.GUILD_ID) {
                name = tax.getId() != null ? tax.getId() : name;
            } else if (target == TaxTarget.VASSAL_ID) {
                name = tax.getId() != null ? tax.getId() : name;
                type = " #4269a8Vassal";
            } else if (target == TaxTarget.TARIFF_ID) {
                name = tax.getId() != null ? tax.getId() : name;
                type = " #79bf6dTariff";
            }

            String oldRate = "";
            if (f != null) {
                if (target == TaxTarget.GUILD_ID || target == TaxTarget.VASSAL_ID || target == TaxTarget.TARIFF_ID) {
                    double rate = f.getTaxRate(target, tax.getId());
                    if (rate == -1.0) oldRate = String.valueOf(f.getTaxRate(target, null));
                    else oldRate = String.valueOf(rate);
                } else {
                    oldRate = String.valueOf(f.getTaxRate(target));
                }
            }

            first.add(StringFormatter.formatHex("#b8ae61Target: #c2bea7" + name + (type.isEmpty() ? "" : " §7(" + type + "§7)")));
            first.add(StringFormatter.formatHex("#b8ae61Change: #c2bea7" + oldRate + "% §7-> #c2bea7" + tax.getNewTax() + "%"));
            if (f != null && (target == TaxTarget.GUILD_ID || target == TaxTarget.VASSAL_ID || target == TaxTarget.TARIFF_ID)) {
                double baseRate = f.getTaxRate(target, null);
                first.add(StringFormatter.formatHex("#3f4040(#767a77Base Rate: #928d7a" + baseRate + "%#3f4040)"));
            }
        }

        // Second page: economic impact (if available)
        List<String> econ = new ArrayList<>();
        if (affectsEconomy() && p != null && f != null) {
            if (isLawProposal() && law != null) {
                LawGroup group = f.getLawHandler().getGroup(law.getGroup());
                EconomicImpact.applyEconomicChange(econ, p, f, group, law, true);
            } else if (isTaxProposal() && tax != null) {
                TaxTarget target = tax.getTarget();
                if (target == TaxTarget.TARIFFS || target == TaxTarget.TARIFF_ID) {
                    EconomicImpact.applyTariffImpact(econ, p, f, tax.getNewTax(), true);
                } else {
                    EconomicImpact.applyTaxImpact(econ, p, f, target, tax.getId(), tax.getNewTax(), true);
                }
            }
        } else if (affectsEconomy()) {
            econ.add(StringFormatter.formatHex("#89504eEconomic preview unavailable"));
            econ.add(StringFormatter.formatHex("#b8ae61Open this book while online to view impact"));
        }

        List<String> pages = new ArrayList<>();
        pages.add(String.join("\n", first));
        if (!econ.isEmpty()) pages.add(String.join("\n", econ));

        meta.setPages(pages);
        item.setItemMeta(meta);
        return item;
    }
}
