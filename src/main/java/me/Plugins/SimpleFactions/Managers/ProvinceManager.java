package me.Plugins.SimpleFactions.Managers;

import java.util.HashMap;
import java.util.Map;

import me.Plugins.SimpleFactions.Map.Provinces.Province;

public class ProvinceManager {
    private Map<Integer, Province> provinces = new HashMap<>();

    public Province get(int id) {
        return provinces.getOrDefault(id, new Province());
    }

    public void start(Map<Integer, Province> map) {
        provinces = map;
    }

    public void calculateTrade() {
        
    }
}
