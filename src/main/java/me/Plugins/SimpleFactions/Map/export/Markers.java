package me.Plugins.SimpleFactions.Map.export;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.settlement.Settlement;

public final class Markers {
    private Markers() {
    }

    public static void export(File out) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("map_id", Cache.mapRef);
        root.addProperty("exported_at", Instant.now().toString());
        root.addProperty("settlement_large_population_threshold", Cache.settlementLargePopulationThreshold);

        JsonArray settlements = new JsonArray();
        for (Faction faction : FactionManager.factions) {
            int factionCapital = faction.getCapital();
            for (Settlement settlement : faction.getSettlementHandler().getAll()) {
                JsonObject row = new JsonObject();
                row.addProperty("id", settlement.getId());
                row.addProperty("name", settlement.getName());
                row.addProperty("faction_id", faction.getId());
                row.addProperty("province_id", settlement.getCenterProvince());
                row.addProperty("center_x", settlement.getCenterX());
                row.addProperty("center_z", settlement.getCenterZ());

                int population = faction.getSettlementHandler().getPopulation(settlement).size();
                String markerSize = population > Cache.settlementLargePopulationThreshold ? "large" : "small";
                row.addProperty("population", population);
                row.addProperty("marker_size", markerSize);

                String kind = factionCapital == settlement.getCenterProvince()
                        ? "faction_capital"
                        : "settlement";
                row.addProperty("kind", kind);

                JsonArray provinces = new JsonArray();
                for (int province : settlement.getProvinces()) {
                    provinces.add(province);
                }
                row.add("provinces", provinces);

                settlements.add(row);
            }
        }
        root.add("settlements", settlements);

        File parent = out.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }

        try (FileWriter writer = new FileWriter(out, StandardCharsets.UTF_8)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(root, writer);
        }
    }
}
