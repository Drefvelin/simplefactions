package me.Plugins.SimpleFactions.Guild.loans;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import me.Plugins.SimpleFactions.Guild.Guild;

public class LoanBook {
    public static ItemStack getBaseBook(Guild guild) {
        ItemStack book = new ItemStack(Material.WRITABLE_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.addPage("[LOAN AGREEMENT]\n" + 
                        "Guild#getName hereby\n" + 
                        "issues a loan of AMOUNT to the guild who signs the document\n" + 
                        "\n" + 
                        "The loan shall be paid in full within DAYS days including PERCENT interest per week on the remaining amount.");
        meta.addPage("Technical Details:\n" + 
                        "Issuer: "+guild.getName()+"\n" + 
                        "Amount (d):\n" + 
                        "Duration(days):\n" + 
                        "Interest (%):\n" + 
                        "Daily Payments:\n" +
                        "\n\n\n" +
                        "Estimated Cost: COST\n" + 
                        "\n" + 
                        "For further reading, see next pages.");
        meta.addPage("Duration and Amount:\n" + 
                        "Failure to pay the amount + interest by the due date, the issuer may use other means to aquire the funds.\n" + 
                        "\n" + 
                        "If daily payments is activated the loan will automatically be repaid without further action for your convenience.");
        meta.addPage("If daily payments are NOT activated it is the responsibility of the loan taker to ensure they repay the loan in time.\n" + 
                        "\n" + 
                        "The loan may be repaid in full or in part at any time, regardless of wether daily payments have been activated.");
        meta.addPage("Interest:\n" + 
                        "The given interest rate represent the weekly rate. However the interest is added daily by taking RATE/7 to represent the daily rate. \n" + 
                        "\n" + 
                        "If daily payments is activated the interest is paid automatically alongside your daily payment.");
        meta.addPage("If daily payments are NOT activated interest will accrue on your total and will be subject to compound interest should the loan taker fail to keep up with interest payments.");
        meta.addPage("Signatures:\n" + 
                        "\n" + 
                        "Issuer:\n" + 
                        "-------------------\n" + 
                        "main guild sign\n" + 
                        "\n" + 
                        "-------------------\n" + 
                        "Loan Taker:\n" + 
                        "\n" + 
                        "[CLICK TO SIGN]\n" + 
                        "-------------------\n" + 
                        "This contract is binding upon signing.");
        book.setItemMeta(meta);
        return book;
    }
}
