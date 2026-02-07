package me.Plugins.SimpleFactions.Managers.Inventory;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Guild.loans.Loan;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public class LoanCreator {
    public ItemStack createLoansGivenButton(Guild guild) {
        ItemStack i = new ItemStack(Material.EMERALD);
        ItemMeta meta = i.getItemMeta();
        meta.setDisplayName(StringFormatter.formatHex("#4fd945Loans Given"));
        
        List<String> lore = new ArrayList<>();
        lore.add(StringFormatter.formatHex("#7a706aView all loans you have issued"));
        lore.add("");
        lore.add(StringFormatter.formatHex("#d6cf69Total Lent: #ccbb76" + 
            String.format("%.2f", guild.getLoanHandler().getTotalLent()) + "d"));
        lore.add("");
        lore.add(StringFormatter.formatHex("#50e846§lClick to view"));
        
        meta.setLore(lore);
        i.setItemMeta(meta);
        return i;
    }

    public ItemStack createLoansTakenButton(Guild guild) {
        ItemStack i = new ItemStack(Material.REDSTONE);
        ItemMeta meta = i.getItemMeta();
        meta.setDisplayName(StringFormatter.formatHex("#cf493aLoans Taken"));
        
        List<String> lore = new ArrayList<>();
        lore.add(StringFormatter.formatHex("#7a706aView all loans you have borrowed"));
        lore.add("");
        lore.add(StringFormatter.formatHex("#d6cf69Total Owed: #ccbb76" + 
            String.format("%.2f", guild.getLoanHandler().getTotalOwed()) + "d"));
        lore.add(StringFormatter.formatHex("#d6cf69Daily Interest: #ccbb76" + 
            String.format("%.2f", guild.getLoanHandler().getDailyInterest()) + "d"));
        lore.add("");
        lore.add(StringFormatter.formatHex("#cf493a§lClick to view"));
        
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
        ItemStack i = new ItemStack(Material.PAPER);
        ItemMeta meta = i.getItemMeta();
        meta.setDisplayName(StringFormatter.formatHex("#4fd945Loan to " + loan.getBorrower().getName()));
        
        List<String> lore = new ArrayList<>();
        lore.add(StringFormatter.formatHex("#7a706aBorrower: #c2bea7" + loan.getBorrower().getName()));
        lore.add("");
        lore.add(StringFormatter.formatHex("#d6cf69Original Amount: #ccbb76" + 
            String.format("%.2f", loan.getAmount()) + "d"));
        lore.add(StringFormatter.formatHex("#d6cf69Amount Paid: #4fd945" + 
            String.format("%.2f", loan.getPaid()) + "d"));
        lore.add(StringFormatter.formatHex("#d6cf69Amount Owed: #cf493a" + 
            String.format("%.2f", loan.getTotalOwed()) + "d"));
        lore.add("");
        lore.add(StringFormatter.formatHex("#d6cf69Interest Rate: #ccbb76" + 
            String.format("%.2f%%", loan.getInterestRate())));
        lore.add(StringFormatter.formatHex("#d6cf69Daily Interest: #ccbb76" + 
            String.format("%.2f", loan.getDailyInterest()) + "d"));
        lore.add("");
        lore.add(StringFormatter.formatHex("#7a706aIssue Date: " + 
            formatDate(loan.getIssueDate())));
        lore.add(StringFormatter.formatHex("#7a706aDue Date: " + 
            formatDate(loan.getDueDate())));
        
        meta.setLore(lore);
        i.setItemMeta(meta);
        return i;
    }

    public ItemStack createLoanTakenItem(Loan loan) {
        ItemStack i = new ItemStack(Material.PAPER);
        ItemMeta meta = i.getItemMeta();
        meta.setDisplayName(StringFormatter.formatHex("#cf493aLoan from " + loan.getIssuer().getName()));
        
        List<String> lore = new ArrayList<>();
        lore.add(StringFormatter.formatHex("#7a706aIssuer: #c2bea7" + loan.getIssuer().getName()));
        lore.add("");
        lore.add(StringFormatter.formatHex("#d6cf69Original Amount: #ccbb76" + 
            String.format("%.2f", loan.getAmount()) + "d"));
        lore.add(StringFormatter.formatHex("#d6cf69Amount Paid: #4fd945" + 
            String.format("%.2f", loan.getPaid()) + "d"));
        lore.add(StringFormatter.formatHex("#d6cf69Amount Owed: #cf493a" + 
            String.format("%.2f", loan.getTotalOwed()) + "d"));
        lore.add("");
        lore.add(StringFormatter.formatHex("#d6cf69Interest Rate: #ccbb76" + 
            String.format("%.2f%%", loan.getInterestRate())));
        lore.add(StringFormatter.formatHex("#d6cf69Daily Interest: #ccbb76" + 
            String.format("%.2f", loan.getDailyInterest()) + "d"));
        lore.add("");
        lore.add(StringFormatter.formatHex("#7a706aIssue Date: " + 
            formatDate(loan.getIssueDate())));
        lore.add(StringFormatter.formatHex("#7a706aDue Date: " + 
            formatDate(loan.getDueDate())));
        
        meta.setLore(lore);
        i.setItemMeta(meta);
        return i;
    }

    private String formatDate(long timestamp) {
        // Simple date formatting - you can enhance this
        long days = timestamp / (24 * 60 * 60 * 1000);
        return days + " days";
    }
}
