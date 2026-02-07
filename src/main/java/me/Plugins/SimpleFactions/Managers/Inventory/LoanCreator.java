package me.Plugins.SimpleFactions.Managers.Inventory;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Guild.loans.Loan;
import me.Plugins.SimpleFactions.Utils.Formatter;
import me.Plugins.SimpleFactions.keys.Keys;
import me.Plugins.TLibs.TLibs;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class LoanCreator {
    private static final String CHECK = "✔";
	private static final String CROSS = "✖";

	private static final String GREEN = "#87d65c";
	private static final String RED   = "#d65c5c";
	private static final String GRAY  = "#6f776a";
	private static final String LIGHT_GRAY  = "#9cb68c";

    public ItemStack createLoansGivenButton(Guild guild) {
        ItemStack i = new ItemStack(Material.BLACK_DYE);
        ItemMeta meta = i.getItemMeta();
        meta.setCustomModelData(16);
        meta.setDisplayName(StringFormatter.formatHex("#7ad65eLoans Given"));
        
        List<String> lore = new ArrayList<>();
        lore.add(StringFormatter.formatHex("#7a706aView all loans you have issued"));
        lore.add("");
        lore.add(StringFormatter.formatHex("#d6cf69Total Lent: #ccbb76" + 
            String.format("%.2f", guild.getLoanHandler().getTotalLent()) + "d"));
        lore.add("");
        lore.add(StringFormatter.formatHex("#50e846Click to View"));
        
        meta.setLore(lore);
        i.setItemMeta(meta);
        return i;
    }

    public ItemStack createLoansTakenButton(Guild guild) {
        ItemStack i = new ItemStack(Material.BLACK_DYE);
        ItemMeta meta = i.getItemMeta();
        meta.setCustomModelData(17);
        meta.setDisplayName(StringFormatter.formatHex("#d45b48Loans Taken"));
        
        List<String> lore = new ArrayList<>();
        lore.add(StringFormatter.formatHex("#7a706aView all loans you have borrowed"));
        lore.add("");
        lore.add(StringFormatter.formatHex("#d6cf69Total Owed: #ccbb76" + 
            Formatter.formatDouble(guild.getLoanHandler().getTotalOwed()) + "d"));
        lore.add(StringFormatter.formatHex("#d6cf69Daily Interest: #ccbb76" + 
            Formatter.formatDouble(guild.getLoanHandler().getDailyInterest()) + "d"));
        lore.add("");
        lore.add(StringFormatter.formatHex("#50e846Click to View"));
        
        meta.setLore(lore);
        i.setItemMeta(meta);
        return i;
    }

    public ItemStack createIssueNewLoanButton() {
        ItemStack i = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta meta = i.getItemMeta();
        meta.setDisplayName(StringFormatter.formatHex("#d6cf69Issue New Loan"));
        
        List<String> lore = new ArrayList<>();
        lore.add(StringFormatter.formatHex("#7a706a§oCreate a new loan agreement"));
        
        meta.setLore(lore);
        i.setItemMeta(meta);
        return i;
    }

    public ItemStack createLoanGivenItem(Loan loan) {
        ItemStack i = new ItemStack(Material.WRITTEN_BOOK);
        ItemMeta meta = i.getItemMeta();
        meta.setDisplayName(StringFormatter.formatHex("#7ad65eLoan to " + loan.getBorrower().getName()));
        
        List<String> lore = new ArrayList<>();
        lore.add(StringFormatter.formatHex("#7a706aBorrower: #c2bea7" + loan.getBorrower().getName()));
        lore.add("");
        lore.add(StringFormatter.formatHex("#d6cf69Original Amount: #ccbb76" + 
            Formatter.formatDouble(loan.getAmount()) + "d"));
        lore.add(StringFormatter.formatHex("#d6cf69Amount Paid: #4fd945" + 
            Formatter.formatDouble(loan.getPaid()) + "d"));
        lore.add(StringFormatter.formatHex("#d6cf69Amount Owed: #cf493a" + 
            Formatter.formatDouble(loan.getTotalOwed()) + "d"));
        lore.add("");
        lore.add(StringFormatter.formatHex("#d6cf69Interest Rate: #ccbb76" + 
            Formatter.formatDouble(loan.getInterestRate()) + "%"));
        lore.add(StringFormatter.formatHex("#d6cf69Daily Interest: #ccbb76" + 
            Formatter.formatDouble(loan.getDailyInterest()) + "d"));
        lore.add(StringFormatter.formatHex("#d6cf69Automatic Payments: " + 
            (loan.isAutoPay() ? (GREEN + CHECK) : (RED + CROSS))));
        lore.add("");
        lore.add(StringFormatter.formatHex("#d1bf92Issue Date: #d4c9ae" + 
            formatDate(loan.getIssueDate())));
        lore.add(StringFormatter.formatHex("#d1bf92Due Date: #d4c9ae" + 
            formatDate(loan.getDueDate())));
        meta.getPersistentDataContainer().set(Keys.STRING_KEY, PersistentDataType.STRING, loan.getId());
        meta.setLore(lore);
        i.setItemMeta(meta);
        return i;
    }

    public ItemStack createLoanTakenItem(Loan loan) {
        ItemStack i = new ItemStack(Material.WRITTEN_BOOK);
        ItemMeta meta = i.getItemMeta();
        meta.setDisplayName(StringFormatter.formatHex("#d45b48Loan from " + loan.getIssuer().getName()));
        
        List<String> lore = new ArrayList<>();
        lore.add(StringFormatter.formatHex("#7a706aIssuer: #c2bea7" + loan.getIssuer().getName()));
        lore.add("");
        lore.add(StringFormatter.formatHex("#d6cf69Original Amount: #ccbb76" + 
            Formatter.formatDouble(loan.getAmount()) + "d"));
        lore.add(StringFormatter.formatHex("#d6cf69Amount Paid: #4fd945" + 
            Formatter.formatDouble(loan.getPaid()) + "d"));
        lore.add(StringFormatter.formatHex("#d6cf69Amount Owed: #cf493a" + 
            Formatter.formatDouble(loan.getTotalOwed()) + "d"));
        lore.add("");
        lore.add(StringFormatter.formatHex("#d6cf69Interest Rate: #ccbb76" + 
            Formatter.formatDouble(loan.getInterestRate())));
        lore.add(StringFormatter.formatHex("#d6cf69Daily Interest: #ccbb76" + 
            Formatter.formatDouble(loan.getDailyInterest()) + "d"));
        lore.add(StringFormatter.formatHex("#d6cf69Automatic Payments: " + 
            (loan.isAutoPay() ? (GREEN + CHECK) : (RED + CROSS))));
        lore.add("");
        lore.add(StringFormatter.formatHex("#d1bf92Issue Date: #d4c9ae" + 
            formatDate(loan.getIssueDate())));
        lore.add(StringFormatter.formatHex("#d1bf92Due Date: #d4c9ae" + 
            formatDate(loan.getDueDate())));
        meta.getPersistentDataContainer().set(Keys.STRING_KEY, PersistentDataType.STRING, loan.getId());
        meta.getPersistentDataContainer().set(Keys.SECONDARY_STRING_KEY, PersistentDataType.STRING, loan.getIssuer().getId());
        meta.setLore(lore);
        i.setItemMeta(meta);
        return i;
    }

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM")
                    .withZone(ZoneId.systemDefault());

    private String formatDate(long timestamp) {
        return DATE_FORMAT.format(Instant.ofEpochMilli(timestamp))
                + "/" + Cache.getFantasyYear(timestamp);
    }

    public ItemStack createPayOffLoanButton(Loan loan) {
        ItemStack i = TLibs.getItemAPI().getCreator().getItemFromPath("m.currency.pouch_of_coins");
        ItemMeta meta = i.getItemMeta();
        meta.setDisplayName(StringFormatter.formatHex("#87d65cPay Off Loan"));
        
        List<String> lore = new ArrayList<>();
        lore.add(StringFormatter.formatHex("#7a706a§oMake a payment on this loan"));
        lore.add("");
        lore.add(StringFormatter.formatHex("#50e846Click to Pay"));
        meta.getPersistentDataContainer().set(Keys.STRING_KEY, PersistentDataType.STRING, loan.getId());
        meta.getPersistentDataContainer().set(Keys.SECONDARY_STRING_KEY, PersistentDataType.STRING, loan.getIssuer().getId());
        meta.setLore(lore);
        i.setItemMeta(meta);
        return i;
    }
}
