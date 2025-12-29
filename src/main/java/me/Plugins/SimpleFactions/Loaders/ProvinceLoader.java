package me.Plugins.SimpleFactions.Loaders;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import me.Plugins.SimpleFactions.Map.Provinces.Province;

public class ProvinceLoader {

    public Map<Integer, Province> loadProvinces(File provinceFile, File neighbourFile) {
        Map<Integer, Province> provinces = new HashMap<>();

        loadProvinceData(provinceFile, provinces);
        loadNeighbours(neighbourFile, provinces);

        return provinces;
    }

    private void loadProvinceData(File file, Map<Integer, Province> provinces) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || !line.contains("=")) continue;

                // Example:
                // 1 = 58,132,60;plains;78
                String[] split = line.split("=");
                int id = Integer.parseInt(split[0].trim());

                String[] parts = split[1].trim().split(";");
                String terrain = parts[1];
                int fertility = Integer.parseInt(parts[2]);

                provinces.put(id, new Province(id, terrain, fertility));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load provinces file", e);
        }
    }

    private void loadNeighbours(File file, Map<Integer, Province> provinces) {
        try (FileReader reader = new FileReader(file)) {
            Gson gson = new Gson();

            Type type = new TypeToken<Map<String, List<Integer>>>() {}.getType();
            Map<String, List<Integer>> neighbourData = gson.fromJson(reader, type);

            for (Map.Entry<String, List<Integer>> entry : neighbourData.entrySet()) {
                int provinceId = Integer.parseInt(entry.getKey());
                Province province = provinces.get(provinceId);

                if (province == null) continue; // safety for mismatched data

                for (int neighbourId : entry.getValue()) {
                    province.addNeighbour(neighbourId);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load province neighbours", e);
        }
    }
}