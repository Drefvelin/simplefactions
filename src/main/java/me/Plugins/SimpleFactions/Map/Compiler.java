package me.Plugins.SimpleFactions.Map;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Compiler {
	public void exportQueue(HashMap<String, List<String>> queues) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        File file = new File("plugins/SimpleFactions/MapAPI/queue.json");

        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(queues, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	
	public void exportAllFactionsToNationJson() {
		File folder = new File("plugins/SimpleFactions/Data");
		File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));

		JsonObject root = new JsonObject();
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		if (files != null && files.length > 0) {
			for (File file : files) {
				try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
					JsonObject factionData = JsonParser.parseReader(reader).getAsJsonObject();
					String id = factionData.get("id").getAsString();
					root.add(id, factionData);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		File outputFile = new File("plugins/SimpleFactions/MapAPI/nation.json");
		outputFile.getParentFile().mkdirs();

		try (FileWriter writer = new FileWriter(outputFile, StandardCharsets.UTF_8)) {
			gson.toJson(root, writer);
			System.out.println("Successfully exported nations to: " + outputFile.getPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
