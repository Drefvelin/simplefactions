package me.Plugins.SimpleFactions.Utils;

import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;

import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.government.proposal.TaxTarget;
import me.Plugins.SimpleFactions.laws.Law;
import me.Plugins.SimpleFactions.laws.LawGroup;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public class EconomicImpact {
    public static void applyEconomicChange(List<String> lore, Player p, Faction f, LawGroup group, Law law) {
        Guild us = FactionManager.getGuildByMember(p.getName());
        if (us != null) {

            Map<Guild, Double> deltas =
                SimpleFactions.getInstance()
                    .getProvinceManager()
                    .previewLawIncomeExact(f, group, law);

            write(lore, deltas, us);
        }
    }

    public static void applyTaxImpact(List<String> lore, Player p, Faction f, TaxTarget target, String id, double rate) {
        Guild us = FactionManager.getGuildByMember(p.getName());
        if (us != null) {

            Map<Guild, Double> deltas =
                f.getTaxHandler().getTaxChangeEffects(target, id, rate);

            write(lore, deltas, us);
        }
    }

    public static void applyTariffImpact(List<String> lore, Player p, Faction f, double newTariffRate) {
        Guild us = FactionManager.getGuildByMember(p.getName());
        if (us != null) {

            Map<Guild, Double> deltas =
                SimpleFactions.getInstance()
                    .getProvinceManager()
                    .previewTariffRateChange(f, newTariffRate);

            write(lore, deltas, us);
        }
    }


    private static void write(List<String> lore, Map<Guild, Double> deltas, Guild us) {
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
