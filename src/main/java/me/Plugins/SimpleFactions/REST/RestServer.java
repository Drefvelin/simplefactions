package me.Plugins.SimpleFactions.REST;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.Objects.Faction;

public class RestServer {
	private static final Gson gson = new Gson();
	public static String apiURL = "http://localhost:8000";
	
	public static List<String> fetchBannerList() {
        try {
            @SuppressWarnings("deprecation")
			URL url = new URL(apiURL+"/generator/banner");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream())
            );

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }

            in.close();

            Type listType = new TypeToken<ArrayList<String>>() {}.getType();
            return gson.fromJson(response.toString(), listType);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
	
	public static int claim(Player p, Faction f) {
		if(!Cache.mapEnabled) return -2;
	    int x = p.getLocation().getBlockX();
	    int z = p.getLocation().getBlockZ();
	    try {
	        String urlStr = String.format(
	        		apiURL+"/map/province/%d,%d",
	            x, z
	        );
	        @SuppressWarnings("deprecation")
			URL url = new URL(urlStr);
	        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	        connection.setRequestMethod("GET");

	        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
	        StringBuilder response = new StringBuilder();
	        String line;
	        while ((line = in.readLine()) != null) {
	            response.append(line);
	        }
	        in.close();

	        // Parse JSON and get province_id
	        JsonObject json = gson.fromJson(response.toString(), JsonObject.class);

	        return json.get("province_id").getAsInt();

	    } catch (Exception e) {
	        e.printStackTrace();
	        return -2;
	    }
	}
	
	public static void upload(String mode, File file) {
		if(!Cache.mapEnabled) return;
	    String charset = "UTF-8";
	    String uploadUrl = String.format(
	    		apiURL + "/data/upload/%s",
	            mode
	        );

	    try {
	        // 1. Read and parse nation.json file
	        BufferedReader reader = new BufferedReader(new FileReader(file));
	        JsonObject payload = JsonParser.parseReader(reader).getAsJsonObject();
	        reader.close();

	        // 3. Setup connection
	        @SuppressWarnings("deprecation")
			URL url = new URL(uploadUrl);
	        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	        connection.setUseCaches(false);
	        connection.setDoOutput(true);
	        connection.setDoInput(true);
	        connection.setRequestMethod("POST");
	        connection.setRequestProperty("Content-Type", "application/json");

	        // 4. Write modified JSON
	        OutputStream os = connection.getOutputStream();
	        byte[] input = payload.toString().getBytes(charset);
	        os.write(input, 0, input.length);
	        os.flush();
	        os.close();

	        // 5. Read response
	        int responseCode = connection.getResponseCode();
	        System.out.println("Upload + Regen Response: " + responseCode);

	        if (responseCode == HttpURLConnection.HTTP_OK) {
	            System.out.println(mode+" data uploaded");
	        } else {
	            System.out.println("Server returned: " + responseCode);
	        }

	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
	
	public static void commenceRegen(String regenType) {
		if(!Cache.mapEnabled) return;
	    String urlStr = String.format(
	        RestServer.apiURL + "/47a4921f7506514aec2d1471b424d8ae/api/regenerate/%s",
	        regenType
	    );

	    try {
	        @SuppressWarnings("deprecation")
			URL url = new URL(urlStr);
	        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	        connection.setRequestMethod("GET"); // Or POST, if your backend uses POST
	        connection.setConnectTimeout(5000);
	        connection.setReadTimeout(5000);

	        int responseCode = connection.getResponseCode();
	        System.out.println("Regeneration request response: " + responseCode);

	        if (responseCode == HttpURLConnection.HTTP_OK) {
	            System.out.println("Regeneration triggered successfully.");
	        } else {
	            System.out.println("Server responded with: " + responseCode);
	        }

	        connection.disconnect();

	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}

}
