package me.Plugins.SimpleFactions.Utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Formatter {
    String[] codes = {
        "&0", "&1", "&2", "&3", "&4", "&5", "&6", "&7",
        "&8", "&9", "&a", "&b", "&c", "&d", "&e", "&f",
        "&k", "&l", "&m", "&n", "&o", "&r"
    };

    // Removes all color codes including hex for ID use
    public String formatId(String i) {
        String s = new String(i);

        // Remove legacy color codes (&x-style)
        for (String c : codes) {
            s = s.replace(c, "");
        }

        // Remove hex codes like #ffffff
        s = s.replaceAll("#[a-fA-F0-9]{6}", "");

        // Remove Minecraft-style hex codes: §x§R§R§G§G§B§B
        s = s.replaceAll("§x(§[0-9a-fA-F]){6}", "");

        // Remove remaining formatting codes like §a, §r, etc.
        s = s.replaceAll("§[0-9a-frk-orA-FK-OR]", "");

        return s;
    }


    // Formats name for Minecraft display
    public String formatName(String i) {
        String s = i;

        // If it doesn't start with a color code, add default color
        if (!s.matches("(?i)^(&[0-9a-frk-or]|#[a-fA-F0-9]{6}).*")) {
            s = "#a3a184" + s;
        }

        // Replace legacy color codes (&x) with §
        s = s.replace("&", "§");

        // Replace _ with space
        s = s.replace("_", " ");

        System.out.println(s);
        return s;
    }
    public Double formatDouble(Double d) {
        if (d == null || d.isNaN() || d.isInfinite()) {
            return 0.0; // or any default value you'd prefer
        }
        return round(d, 2);
    }

    private double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
