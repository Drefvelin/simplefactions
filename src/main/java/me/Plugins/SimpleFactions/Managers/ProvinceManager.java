package me.Plugins.SimpleFactions.Managers;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import me.Plugins.SimpleFactions.Guild.Branch.Branch;
import me.Plugins.SimpleFactions.Guild.Branch.BranchModifier;
import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Guild.snapshot.GuildModifierSnapshot;
import me.Plugins.SimpleFactions.Map.Provinces.Province;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.enums.GuildModifier;

public class ProvinceManager {
    private Map<Integer, Province> provinces = new HashMap<>();

    public Province get(int id) {
        return provinces.getOrDefault(id, new Province());
    }

    public void start(Map<Integer, Province> map) {
        provinces = map;
    }

    public void recalculate() {
        for(Faction f : FactionManager.factions) {
            for(Guild g : f.getGuildHandler().getGuilds()) {
                if(!g.hasCapital()) continue;
                calculateTrade(g, false);
            }
        }
        for(Province p : provinces.values()) {
            p.calculateProsperity();
        }
    }

    public void calculateTrade(Guild guild, boolean calculateProsperity) {
        if(!provinces.containsKey(guild.getCapital())) return;
        Province capital = provinces.get(guild.getCapital());
        capital.calculateTrade(guild, null, calculateProsperity);
    }

    public double getIncome(Guild guild) {
        double income = 0;
        String guildId = guild.getId();

        for (Province province : provinces.values()) {
            double guildTrade = province.getGuildTrade(guildId);
            if (guildTrade <= 0) continue;

            double totalTrade = province.getTotalTrade();
            if (totalTrade <= 0) continue;

            double share = guildTrade / totalTrade;
            income += share * province.getProsperity();
        }

        // Optional rounding for display
        return Math.round(income * 100.0) / 100.0;
    }

    public Map<Integer, Double> simulateTrade(int capital, GuildModifierSnapshot mods) {
        Map<Integer, Double> trade = new HashMap<>();
        Queue<Integer> queue = new ArrayDeque<>();

        trade.put(capital, mods.get(GuildModifier.TRADE_POWER));
        queue.add(capital);

        while (!queue.isEmpty()) {
            int current = queue.poll();
            double currentTrade = trade.get(current);

            if (currentTrade < 0.1) continue;

            Province p = provinces.get(current);
            if (p == null) continue;

            for (int n : p.getNeighbours()) {
                Province neighbour = provinces.get(n);
                if (neighbour == null) continue;

                double factor = mods.get(GuildModifier.TRADE_CARRY)
                            * neighbour.getTradeCarry();

                factor = Math.min(factor, 0.95);

                double nextTrade = currentTrade * factor;
                if (nextTrade < 0.1) continue;

                double existing = trade.getOrDefault(n, 0.0);
                if (nextTrade > existing) {
                    trade.put(n, nextTrade);
                    queue.add(n);
                }
            }
        }

        return trade;
    }

    public double simulateIncome(Guild guild, GuildModifierSnapshot mods) {
        Map<Integer, Double> tradeMap =
                simulateTrade(guild.getCapital(), mods);

        double income = 0;

        for (Map.Entry<Integer, Double> e : tradeMap.entrySet()) {
            Province p = provinces.get(e.getKey());
            if (p == null) continue;

            double guildTrade = e.getValue();
            double existingGuildTrade = p.getGuildTrade(guild.getId());
            double totalTrade =
                    p.getTotalTrade()
                - existingGuildTrade
                + guildTrade;
            if (totalTrade <= 0) continue;

            double share = guildTrade / totalTrade;
            income += share * p.getProsperity();
        }

        return Math.round(income * 100.0) / 100.0;
    }

    public double previewUpgradeIncome(Guild guild, Branch branch) {
        GuildModifierSnapshot snap = new GuildModifierSnapshot(guild);

        for (GuildModifier m : branch.getModifiers().keySet()) {
            BranchModifier bm = branch.getModifier(m);
            double delta = bm.getCurrent(branch.getLevel() + 1)
                        - bm.getCurrent(branch.getLevel());

            snap.add(m, delta);
        }

        double current = getIncome(guild);
        double simulated = simulateIncome(guild, snap);

        return simulated - current;
    }
}
