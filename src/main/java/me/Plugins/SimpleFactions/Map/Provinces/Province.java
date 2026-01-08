package me.Plugins.SimpleFactions.Map.Provinces;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.ProvinceManager;
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
        if(data.containsKey(id)) return data.get(id);
        Guild guild = FactionManager.getGuildByString(id);
        if(guild != null) return new ProvinceDataEntry(guild);
        return null;
    }
    public void setData(String id, ProvinceDataEntry entry) {
        data.put(id, entry);
    }
    public void clearGuildData(String guildId) {
        data.remove(guildId);
    }
    public void calculateTrade(
            ProvinceManager manager,
            Guild guild,
            ProvinceDataEntry prev,
            int distance
    ) {
        double amount;
        double carry = guild.getModifier(GuildModifier.TRADE_CARRY);
        double effectiveDistance = distance / Math.pow(carry, 1.1);
        double factor = Math.pow(getTradeCarry(), effectiveDistance);

        if (prev == null) {
            // Capital province
            amount = guild.getModifier(GuildModifier.TRADE_POWER);
        } else {
            amount = prev.getTrade() *factor;
        }

        if(amount < 0.1) return;

        ProvinceDataEntry entry = data.get(guild.getId());

        // Stop if we already have equal or better trade
        if (entry != null && entry.getTrade() >= amount) {
            return;
        }

        if (entry == null) {
            entry = new ProvinceDataEntry(guild);
            data.put(guild.getId(), entry);
        }

        entry.setTrade(amount);
        entry.setDistance(distance);

        for (Integer n : neighbours) {
            Province neighbour = manager.get(n);
            if (neighbour != null) {
                neighbour.calculateTrade(manager, guild, entry, distance+1);
            }
        }
    }

    public void calculateProduction(
            ProvinceManager manager,
            Guild guild,
            ProvinceDataEntry prev,
            int distance
    ) {
        double amount;
        double terrainFactor = Math.pow(getTradeCarry(), 0.5);
        double factor = terrainFactor*getTradeFactor(guild);

        if (prev == null) {
            // Capital province
            amount = guild.getModifier(GuildModifier.PRODUCTION);
        } else {
            amount = prev.getProduction()*factor;
        }
        if(amount < 0.1) return;

        ProvinceDataEntry entry = data.get(guild.getId());

        // Stop if we already have equal or better trade
        if (entry != null && entry.getProduction() >= amount) {
            return;
        }

        if (entry == null) {
            entry = new ProvinceDataEntry(guild);
            data.put(guild.getId(), entry);
        }

        entry.setProduction(amount);

        for (Integer n : neighbours) {
            Province neighbour = manager.get(n);
            if (neighbour != null) {
                neighbour.calculateProduction(manager, guild, entry, distance+1);
            }
        }
    }

    public double getTradeFactor(Guild guild) {
        double trade = getGuildTrade(guild.getId());
        if(trade == 0) return 0.05;
        double K = 2.5 / Math.pow(getTradeCarry(), 0.5);
        return (trade / (trade + K));
    }

    public double getIncome(Guild guild) {
        double trade = getGuildTrade(guild.getId());
        if (trade <= 0) return 0;

        double totalTrade = getTotalTrade();
        if (totalTrade <= 0) return 0;

        // 1) Reservation (competition / stealing)
        double share = trade / totalTrade;
        double reserved = prosperity * share;

        return reserved;
    }


    public void calculateProsperity() {
        double total = 0;
        int participants = 0;
        for(ProvinceDataEntry entry : data.values()) {
            double carry = entry.getGuild().getModifier(GuildModifier.TRADE_CARRY);
            double distance = entry.getDistance(); // ensure double

            double weight =
                Math.max(0.0, 1.0 + carry * 0.2 - distance * 0.1);
            double productionWeight = Math.min(1.5, 1+entry.getProduction()/100.0);

            total += entry.getProduction() * weight*productionWeight;

            if (entry.getTrade() > 0.5) { // threshold
                participants++;
            }
        }

        double exponent = 1 + Math.min(0.1, participants * 0.02);

        total = Math.pow(total, exponent);
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

    public double getTradeShare(String guildId) {
        if(!data.containsKey(guildId)) return 0;
        return getGuildTrade(guildId)/getTotalTrade();
    }

    public double getProsperity() {
        return prosperity;
    }

    public Province cloneShell() {
        Province p = new Province(id, terrain.name(), fertility);
        p.neighbours.addAll(this.neighbours);
        return p;
    }

    public void clearData() {
        data.clear();
    }

    public Map<String, ProvinceDataEntry> getAllData() {
        return data;
    }

    public void setProsperity(double p) {
        this.prosperity = p;
    }
}
