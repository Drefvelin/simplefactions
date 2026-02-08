package me.Plugins.SimpleFactions.Guild.loans;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import me.Plugins.SimpleFactions.Guild.Guild;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

public class LoanBook {

    public static ItemStack getBaseBook(Guild guild) {
        ItemStack book = new ItemStack(Material.WRITABLE_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();

        // Page 1
        meta.spigot().addPage(page(
            text("[LOAN AGREEMENT]\n", "#e8b658", true),
            text("Guild " + guild.getName() + " hereby\n", "#615642", false),
            text("issues a loan of AMOUNT to the guild who signs the document\n\n", "#55ff55", false),
            text("The loan shall be paid in full within DAYS days including PERCENT interest per week on the remaining amount.", "#ffffff", false)
        ));

        // Page 2
        meta.spigot().addPage(page(
            text("Technical Details:\n", "#ffffff", true),
            text("Issuer: " + guild.getName() + "\n", "#ff5555", false),
            text("Amount (d):\n", "#aa0000", false),
            text("Duration (days):\n", "#ffffff", false),
            text("Interest (%):\n", "#ffffff", false),
            text("Daily Payments:\n", "#ffffff", false),
            text("Overdue Fee (%):\n\n\n", "#ffffff", false),
            text("Estimated Cost: COST\n\n", "#ffff55", false),
            text("For further reading, see next pages.", "#aaaaaa", false)
        ));

        // Page 3
        meta.spigot().addPage(page(
            text("Duration and Amount:\n", "#ffffff", true),
            text(
                "Failure to pay the amount + interest by the due date, the issuer may use other means to acquire the funds.\n\n",
                "#ffffff", false
            ),
            text(
                "If daily payments is activated the loan will automatically be repaid without further action for your convenience.",
                "#ffffff", false
            )
        ));

        // Page 4
        meta.spigot().addPage(page(
            text(
                "If daily payments are NOT activated it is the responsibility of the loan taker to ensure they repay the loan in time.\n\n",
                "#ffffff", false
            ),
            text(
                "The loan may be repaid in full or in part at any time, regardless of whether daily payments have been activated.",
                "#ffffff", false
            )
        ));

        // Page 5
        meta.spigot().addPage(page(
            text("Interest:\n", "#ffffff", true),
            text(
                "The given interest rate represents the weekly rate. However the interest is added daily by taking RATE/7 to represent the daily rate.\n\n",
                "#ffffff", false
            ),
            text(
                "If daily payments is activated the interest is paid automatically alongside your daily payment.",
                "#ffffff", false
            )
        ));

        // Page 6
        meta.spigot().addPage(page(
            text(
                "If daily payments are NOT activated interest will accrue on your total and will be subject to compound interest should the loan taker fail to keep up with interest payments.",
                "#ffffff", false
            )
        ));

        // Page 7
        meta.spigot().addPage(page(
            text("Overdue Fees:\n", "#ffffff", true),
            text(
                "If the loan is NOT paid back in time the issuer may attempt to acquire the payment by other means.\n\n",
                "#ffffff", false
            ),
            text(
                "For every day the loan is overdue extra interest will accrue on the remaining total according to the overdue fee percentage.",
                "#ffffff", false
            )
        ));

        // Page 8
        meta.spigot().addPage(page(
            text("Signatures:\n\n", "#ffffff", true),
            text("Issuer:\n", "#ffffff", false),
            text("-------------------\n", "#aaaaaa", false),
            text("main guild sign\n\n", "#ffffff", false),
            text("-------------------\n", "#aaaaaa", false),
            text("Loan Taker:\n\n", "#ffffff", false),
            text("[CLICK TO SIGN]\n", "#55ffff", true),
            text("-------------------\n", "#aaaaaa", false),
            text("This contract is binding upon signing.", "#ff5555", false)
        ));

        book.setItemMeta(meta);
        return book;
    }

    /* =========================
       Helpers
       ========================= */

    private static BaseComponent[] page(BaseComponent... components) {
        TextComponent root = new TextComponent();
        for (BaseComponent c : components) {
            root.addExtra(c);
        }
        return new BaseComponent[]{ root };
    }

    private static TextComponent text(String content, String hexColor, boolean bold) {
        TextComponent tc = new TextComponent(content);
        try {
            tc.setColor(ChatColor.of(hexColor));
        } catch (IllegalArgumentException ignored) {
            // Fallback if hex is rejected
            tc.setColor(ChatColor.WHITE);
        }
        tc.setBold(bold);
        return tc;
    }
}
