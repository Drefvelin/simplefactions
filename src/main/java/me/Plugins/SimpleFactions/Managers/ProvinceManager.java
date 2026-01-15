package me.Plugins.SimpleFactions.Managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.Plugins.SimpleFactions.Guild.Branch.Branch;
import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Map.Provinces.Province;
import me.Plugins.SimpleFactions.Map.Provinces.ProvinceDataEntry;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.enums.GuildModifier;
import me.Plugins.SimpleFactions.laws.Law;
import me.Plugins.SimpleFactions.laws.LawGroup;

public class ProvinceManager {
    private Map<Integer, Province> provinces = new HashMap<>();
    private long stateVersion = 0;
    private long lastCalculatedVersion = -1;

    public void markDirty() {
        stateVersion++;
    }

    public void recalculateIfNeeded() {
        if (stateVersion == lastCalculatedVersion) return;

        recalculate();
        lastCalculatedVersion = stateVersion;
    }

    public List<Province> getProvinces() { return new ArrayList<>(provinces.values()); }

    public Province get(int id) {
        return provinces.getOrDefault(id, new Province());
    }

    public void start(Map<Integer, Province> map) {
        provinces = map;
    }

    public ProvinceManager createSnapshotShell() {
        ProvinceManager snap = new ProvinceManager();
        Map<Integer, Province> map = new HashMap<>();

        for (Province p : provinces.values()) {
            map.put(p.getId(), p.cloneShell());
        }

        snap.start(map);
        return snap;
    }

    public void recalculate() {
        for(Guild g : FactionManager.getAllGuilds()) {
            if (!g.hasCapital()) continue;
            recalculateGuild(g);
        }
        for(Guild g : FactionManager.getAllGuilds()) {
            if (!g.hasCapital()) continue;
            recalculateProduction(g);
        }
        recalculateProsperity();
        for(Guild guild : FactionManager.getAllGuilds()) getIncome(guild);
    }

    public void recalculateForSingleGuild(Guild g, boolean save) {
        if (!g.hasCapital()) return;
        recalculateGuild(g);
        recalculateProduction(g);
        if(save) {
            for(Guild guild : FactionManager.getAllGuilds()) {
                getIncome(guild);
            }
        }
        recalculateProsperity();
    }

    private void recalculateProsperity() {
        for (Province p : provinces.values()) {
            p.calculateProsperity();
        }
    }

    private void recalculateProduction(Guild guild) {
        Province capital = provinces.get(guild.getCapital());
        if (capital != null) {
            capital.calculateProduction(this, guild, null, 0);
        }
    }

    private void recalculateGuild(Guild guild) {
        String guildId = guild.getId();

        // 1) Clear only this guild’s data
        for (Province p : provinces.values()) {
            p.clearGuildData(guildId);
        }

        // 2) Recalculate trade graph
        Province capital = provinces.get(guild.getCapital());
        if (capital != null) {
            capital.calculateTrade(this, guild, -1, 0);
        }
    }

    public double getIncome(Guild guild, boolean save) {
        if(save) guild.getTradeBreakdown().clear();
        double income = 0;
        double upkeep = 0;
        double trade = 0;
        double upkeepFactor = guild.getModifier(GuildModifier.TRADE_UPKEEP);

        for (Province province : provinces.values()) {
            if(!province.getTerrain().generatesIncome()) continue;
            double provinceIncome = province.getIncome(guild);
            if(provinceIncome == 0) continue;
            //if(provinceIncome > guildTrade) provinceIncome = guildTrade;
            upkeep += provinceIncome*province.getTradeFactor(guild)*upkeepFactor;
            income += provinceIncome;
            Faction owner = TitleManager.getByProvince(province.getId());
            if(owner == null) continue;
            if(save) guild.getTradeBreakdown().registerIncome(owner, provinceIncome);
            trade += getTotalTrade(guild);
        }
        if(save) {
            guild.getTradeBreakdown().setUpkeep(upkeep);
            guild.getTradeBreakdown().setIncome(income);
            guild.getTradeBreakdown().setTradePower(trade);
        }
        income-=upkeep;

        // Optional rounding for display
        return Math.round(income * 100.0) / 100.0;
    }

    public double getIncome(Guild guild) {
        return getIncome(guild, true);
    }

    public double getTotalTrade(Guild guild) {
        double total = 0;
        for(Province p : provinces.values()) {
            total += p.getGuildTrade(guild);
        }
        return total;
    }

    public Map<Guild, Double> previewLawIncomeExact(LawGroup group, Law law) {
        ProvinceManager live = this;
        ProvinceManager snap = SimpleFactions.getInstance().getProvinceSnapshot();
        snap.copyAllDataFrom(live);
        Law old = group.getCurrent();
        group.setCurrent(law);
        for(Province p : snap.getProvinces()) {
            p.calculateProsperity();
        }
        Map<Guild, Double> map = new HashMap<>();
        for(Guild guild : FactionManager.getAllGuilds()) {
            double delta = snap.getIncome(guild, false)-guild.getTradeBreakdown().getNetTradeIncome();
            map.put(guild, Math.round(delta * 100.0) / 100.0);
        }
        group.setCurrent(old);
        return map;
    }

    public double previewUpgradeIncomeExact(Guild guild, Branch branch) {
        ProvinceManager live = this;
        ProvinceManager snap = SimpleFactions.getInstance().getProvinceSnapshot();

        double liveIncomeBefore = live.getIncome(guild);

        // Sync snapshot to live state
        snap.copyAllDataFrom(live);

        // Apply upgrade in snapshot context
        branch.levelUp();
        snap.recalculateForSingleGuild(guild, false);
        double snapIncomeAfter = snap.getIncome(guild, false);

        // Revert upgrade
        branch.levelDown();

        double delta = snapIncomeAfter - liveIncomeBefore;
        return Math.round(delta * 100.0) / 100.0;
    }

    public double previewDowngradeIncomeExact(Guild guild, Branch branch) {
        ProvinceManager live = this;
        ProvinceManager snap = SimpleFactions.getInstance().getProvinceSnapshot();

        double liveIncomeBefore = live.getIncome(guild);

        // Sync snapshot to live state
        snap.copyAllDataFrom(live);

        // Apply upgrade in snapshot context
        branch.levelDown();
        snap.recalculateForSingleGuild(guild, false);
        double snapIncomeAfter = snap.getIncome(guild, false);

        // Revert upgrade
        branch.levelUp();

        double delta = snapIncomeAfter - liveIncomeBefore;
        return Math.round(delta * 100.0) / 100.0;
    }

    //Simulation
    public void copyAllDataFrom(ProvinceManager source) {
        for (Province src : source.provinces.values()) {
            Province dst = provinces.get(src.getId());
            if (dst == null) continue;

            dst.clearData();

            for (Map.Entry<String, ProvinceDataEntry> e : src.getAllData().entrySet()) {
                dst.setData(e.getKey(), e.getValue().copy());
            }

            dst.setProsperity(src.getProsperity());
        }
    }
}
