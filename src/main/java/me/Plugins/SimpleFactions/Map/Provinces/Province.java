package me.Plugins.SimpleFactions.Map.Provinces;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.enums.GuildModifier;
import me.Plugins.SimpleFactions.enums.Terrain;

public class Province {
    private int id;
    private Map<String, ProvinceDataEntry> data = new HashMap<>();
    private Terrain terrain;
    private int fertility;
    private double prosperity = 0;
    private final Set<Integer> neighbours = new HashSet<>();

    public Province() {
        this.id = 0;
        terrain = Terrain.UNKNOWN;
        fertility = 0;
    }

    public Province(int id, String terrain, int fertility) {
        this.id = id;
        try {
            this.terrain = Terrain.valueOf(terrain.toUpperCase());
        } catch (Exception e) {
            this.terrain = Terrain.UNKNOWN;
        }
        this.fertility = fertility;
    }

    public int getId() { return id; }
    public boolean isValid() { return id != 0; }
    public Terrain getTerrain() { return terrain; }
    public int getFertility() { return fertility; }
    public ProvinceDataEntry getData(String id) {
        return data.getOrDefault(id, new ProvinceDataEntry(id));
    }
    public void setData(String id, ProvinceDataEntry entry) {
        data.put(id, entry);
    }
    public void calculateTrade(Guild guild, ProvinceDataEntry prev, boolean calculateProsperity) {
        double amount;

        if (prev == null) {
            // Capital province: no decay
            amount = guild.getModifier(GuildModifier.TRADE_POWER);
        } else {
            double factor = guild.getModifier(GuildModifier.TRADE_CARRY)
                        * getTradeCarry();

            factor = Math.min(factor, 0.95);
            amount = prev.getTrade() * factor;

            // Hard cutoff
            if (amount < 0.1) {
                return;
            }
        }

        ProvinceDataEntry entry = data.get(guild.getId());

        // Stop if we already have equal or better trade
        if (entry != null && entry.getTrade() >= amount) {
            return;
        }

        double production = amount * guild.getModifier(GuildModifier.PRODUCTION);

        if (entry == null) {
            // Create once
            entry = new ProvinceDataEntry(guild.getId());
            data.put(guild.getId(), entry);
        }

        // Update in-place
        entry.setTrade(amount);
        entry.setProduction(production);

        for (Integer n : neighbours) {
            Province neighbour =
                    SimpleFactions.getInstance().getProvinceManager().get(n);
            if (neighbour != null) {
                neighbour.calculateTrade(guild, entry, calculateProsperity);
            }
        }
    }
    public void calculateProsperity() {
        double total = 0;
        for(ProvinceDataEntry entry : data.values()) {
            total+=entry.getProduction();
        }
        total = Math.round(total * 100.0) / 100.0;
        this.prosperity = total;
    }
    public double getTradeCarry() {
        return Cache.tradeCarry.getOrDefault(terrain, 0.5);
    }
    public Set<Integer> getNeighbours() {
        return Collections.unmodifiableSet(neighbours);
    }

    public void addNeighbour(int provinceId) {
        neighbours.add(provinceId);
    }

    public double getTotalTrade() {
        double total = 0;
        for (ProvinceDataEntry entry : data.values()) {
            total += entry.getTrade();
        }
        return total;
    }

    public double getGuildTrade(String guildId) {
        ProvinceDataEntry entry = data.get(guildId);
        return entry == null ? 0 : entry.getTrade();
    }

    public double getProsperity() {
        return prosperity;
    }
}
