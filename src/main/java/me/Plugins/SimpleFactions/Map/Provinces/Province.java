package me.Plugins.SimpleFactions.Map.Provinces;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.enums.Terrain;

public class Province {
    private int id;
    private Map<String, ProvinceDataEntry> data = new HashMap<>();
    private Terrain terrain;
    private int fertility;
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
    public double getTradeCarry() {
        return Cache.tradeCarry.getOrDefault(terrain, 0.5);
    }
    public Set<Integer> getNeighbours() {
        return Collections.unmodifiableSet(neighbours);
    }

    public void addNeighbour(int provinceId) {
        neighbours.add(provinceId);
    }
}
