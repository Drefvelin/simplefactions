package me.Plugins.SimpleFactions.Managers.Inventory;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Guild.loans.Loan;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.Holder.SFInventoryHolder;
import me.Plugins.SimpleFactions.Managers.InventoryManager;
import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.enums.SFGUI;
import me.Plugins.SimpleFactions.keys.Keys;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public class LoanView {
    public InventoryManager inv;
    public LoanCreator creator = new LoanCreator();

    public LoanView(InventoryManager inv) {
        this.inv = inv;
    }

    public void loanMainView(Player player, Guild guild) {
        Inventory i = SimpleFactions.plugin.getServer().createInventory(
            new SFInventoryHolder(guild.getId(), SFGUI.LOAN_MAIN_VIEW), 
            9, 
            "§7Loans - " + guild.getName()
        );
        
        // Button 1: Loans Given
        i.setItem(2, creator.createLoansGivenButton(guild));
        
        // Button 2: Loans Taken
        i.setItem(4, creator.createLoansTakenButton(guild));
        
        // Button 3: Issue New Loan
        i.setItem(6, creator.createIssueNewLoanButton());
        
        // Back button
        i.setItem(8, inv.createBackButton(SFGUI.LOAN_MAIN_VIEW));
        
        player.openInventory(i);
    }

    public void loansGivenView(Player player, Guild guild) {
        Inventory i = SimpleFactions.plugin.getServer().createInventory(
            new SFInventoryHolder(guild.getId(), SFGUI.LOANS_GIVEN_VIEW), 
            54, 
            "§7Loans Given - " + guild.getName()
        );
        
        List<Loan> loansGiven = guild.getLoanHandler().getLoansGiven();
        
        int slot = 0;
        for (Loan loan : loansGiven) {
            if (slot >= 45) break; // Leave space for back button
            i.setItem(slot, creator.createLoanGivenItem(loan));
            slot++;
        }
        
        // Back button
        i.setItem(53, inv.createBackButton(SFGUI.LOANS_GIVEN_VIEW));
        
        player.openInventory(i);
    }

    public void loansTakenView(Player player, Guild guild) {
        Inventory i = SimpleFactions.plugin.getServer().createInventory(
            new SFInventoryHolder(guild.getId(), SFGUI.LOANS_TAKEN_VIEW), 
            54, 
            "§7Loans Taken - " + guild.getName()
        );
        
        List<Loan> loansTaken = guild.getLoanHandler().getLoansTaken();
        
        int slot = 0;
        for (Loan loan : loansTaken) {
            if (slot >= 45) break; // Leave space for back button
            i.setItem(slot, creator.createLoanTakenItem(loan));
            slot++;
        }
        
        // Back button
        i.setItem(53, inv.createBackButton(SFGUI.LOANS_TAKEN_VIEW));
        
        player.openInventory(i);
    }

    public void click(InventoryClickEvent e, Inventory inventory, Player p) {
        if(!(inventory.getHolder() instanceof SFInventoryHolder)) return;
        SFInventoryHolder h = (SFInventoryHolder) inventory.getHolder();
        Guild guild = FactionManager.getGuildByString(h.getId());
        
        if(h.getType() == SFGUI.LOAN_MAIN_VIEW) {
            e.setCancelled(true);
            
            // Loans Given button
            if(e.getSlot() == 2) {
                loansGivenView(p, guild);
            }
            // Loans Taken button
            else if(e.getSlot() == 4) {
                loansTakenView(p, guild);
            }
            // Issue New Loan button
            else if(e.getSlot() == 6) {
                // TODO: Implement loan creation later
                p.sendMessage(StringFormatter.formatHex("#d6cf69Loan creation coming soon!"));
            }
        }
        else if(h.getType() == SFGUI.LOANS_GIVEN_VIEW) {
            e.setCancelled(true);
            // Individual loan interactions can be added here later
        }
        else if(h.getType() == SFGUI.LOANS_TAKEN_VIEW) {
            e.setCancelled(true);
            // Individual loan interactions can be added here later
        }
    }
}
