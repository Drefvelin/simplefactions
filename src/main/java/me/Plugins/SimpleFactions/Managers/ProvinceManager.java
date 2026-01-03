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

public class ProvinceManager {
    private Map<Integer, Province> provinces = new HashMap<>();

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
        recalculateProsperity();
        for(Guild guild : FactionManager.getAllGuilds()) getIncome(guild);
        FactionManager.getMap().exportProvinces();
        FactionManager.getMap().exportGuilds();
    }


    public void recalculateProsperity() {
        for (Province p : provinces.values()) {
            p.calculateProsperity();
        }
    }


    public void recalculateGuild(Guild guild) {
        String guildId = guild.getId();

        // 1) Clear only this guild’s data
        for (Province p : provinces.values()) {
            p.clearGuildData(guildId);
        }

        // 2) Recalculate trade graph
        Province capital = provinces.get(guild.getCapital());
        if (capital != null) {
            capital.calculateTrade(this, guild, null, guild.getModifier(GuildModifier.TRADE_CARRY)+1);
        }
    }

    public double getIncome(Guild guild) {
        guild.getTradeBreakdown().clear();
        double income = 0;
        String guildId = guild.getId();
        double trade = 0;

        for (Province province : provinces.values()) {
            if(!province.getTerrain().generatesIncome()) continue;
            double guildTrade = province.getGuildTrade(guildId);
            if (guildTrade <= 0) continue;
            trade += guildTrade;

            double totalTrade = province.getTotalTrade();
            if (totalTrade <= 0) continue;

            double share = guildTrade / totalTrade;
            double provinceIncome = share * province.getProsperity();
            income += provinceIncome;
            Faction owner = TitleManager.getByProvince(province.getId());
            if(owner == null) continue;
            guild.getTradeBreakdown().registerIncome(owner, provinceIncome);
        }

        double upkeep = trade*guild.getModifier(GuildModifier.TRADE_UPKEEP);
        guild.getTradeBreakdown().setUpkeep(upkeep);
        guild.getTradeBreakdown().setIncome(income);
        guild.getTradeBreakdown().setTradePower(trade);
        income-=upkeep;

        // Optional rounding for display
        return Math.round(income * 100.0) / 100.0;
    }

    public double getTotalTrade(Guild guild) {
        double total = 0;
        for(Province p : provinces.values()) {
            total += p.getGuildTrade(guild.getId());
        }
        return total;
    }

    public double previewUpgradeIncomeExact(Guild guild, Branch branch) {
        ProvinceManager live = this;
        ProvinceManager snap = SimpleFactions.getInstance().getProvinceSnapshot();

        SimpleFactions.getInstance().getLogger().info(
            "[PREVIEW DEBUG] ===== START PREVIEW ====="
        );
        SimpleFactions.getInstance().getLogger().info(
            "[PREVIEW DEBUG] Guild=" + guild.getId()
            + " | Branch=" + branch.getId()
            + " | Level(before)=" + branch.getLevel()
        );

        // Baseline (live)
        double liveIncomeBefore = live.getIncome(guild);
        SimpleFactions.getInstance().getLogger().info(
            "[PREVIEW DEBUG] Live income BEFORE = " + liveIncomeBefore
        );

        // 1) Copy full world state
        snap.copyAllDataFrom(live);

        double snapIncomeAfterCopy = snap.getIncome(guild);
        SimpleFactions.getInstance().getLogger().info(
            "[PREVIEW DEBUG] Snapshot income AFTER COPY (should match live) = "
            + snapIncomeAfterCopy
        );

        // 2) Apply upgrade
        branch.levelUp();
        SimpleFactions.getInstance().getLogger().info(
            "[PREVIEW DEBUG] Applied upgrade | Branch level(now)=" + branch.getLevel()
        );

        // 3) Recalculate snapshot
        snap.recalculate();

        // 4) Measure snapshot
        double snapIncomeAfter = snap.getIncome(guild);
        SimpleFactions.getInstance().getLogger().info(
            "[PREVIEW DEBUG] Snapshot income AFTER RECALC = " + snapIncomeAfter
        );

        // 5) Revert upgrade
        branch.levelDown();
        SimpleFactions.getInstance().getLogger().info(
            "[PREVIEW DEBUG] Reverted upgrade | Branch level(now)=" + branch.getLevel()
        );

        // Sanity check: live world unchanged
        double liveIncomeAfter = live.getIncome(guild);
        SimpleFactions.getInstance().getLogger().info(
            "[PREVIEW DEBUG] Live income AFTER preview = " + liveIncomeAfter
        );

        if (Math.abs(liveIncomeBefore - liveIncomeAfter) > 0.001) {
            SimpleFactions.getInstance().getLogger().warning(
                "[PREVIEW DEBUG] ❌ LIVE WORLD MUTATED! Δ="
                + (liveIncomeAfter - liveIncomeBefore)
            );
        }

        double delta = snapIncomeAfter - liveIncomeBefore;

        SimpleFactions.getInstance().getLogger().info(
            "[PREVIEW DEBUG] FINAL DELTA = " + delta
        );
        SimpleFactions.getInstance().getLogger().info(
            "[PREVIEW DEBUG] ===== END PREVIEW ====="
        );

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
