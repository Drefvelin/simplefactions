package me.Plugins.SimpleFactions.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import me.Plugins.SimpleFactions.Army.Military;
import me.Plugins.SimpleFactions.Army.MilitaryExpansion;
import me.Plugins.SimpleFactions.Army.Regiment;
import me.Plugins.SimpleFactions.Diplomacy.Relation;
import me.Plugins.SimpleFactions.Loaders.TitleLoader;
import me.Plugins.SimpleFactions.Loaders.WarGoalLoader;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.RelationManager;
import me.Plugins.SimpleFactions.Objects.Bank;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Objects.FactionModifier;
import me.Plugins.SimpleFactions.Objects.Modifier;
import me.Plugins.SimpleFactions.Tiers.Title;
import me.Plugins.SimpleFactions.War.Participant;
import me.Plugins.SimpleFactions.War.Side;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.WarGoal;

public class Database {
	Formatter format = new Formatter();
	private JSONObject json; // org.json.simple
    JSONParser parser = new JSONParser();

	public int getTimer(){
		File file = new File("plugins/SimpleFactions/Cache", "data.json");
		if(file.exists()){
			try {
				json = (JSONObject) parser.parse(new InputStreamReader(new FileInputStream(file), "UTF-8"));
				return (int) Math.round((Double) json.get("time"));
			} catch (IOException | ParseException e) {
				e.printStackTrace();
			}
		}
		return 0;
	}

	@SuppressWarnings("unchecked")
	public void saveTimer(int time) {
		File file = new File("plugins/SimpleFactions/Cache", "data.json");
		JSONObject json = new JSONObject(); 
		
		json.put("time", time);

		try (FileWriter writer = new FileWriter(file)) {
			writer.write(json.toJSONString());
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public void saveWar(War w) {
		try {
			JSONObject root = new JSONObject();
			root.put("id", w.getId());

			// Save attackers and defenders
			root.put("attackers", serializeSide(w.getAttackers()));
			root.put("defenders", serializeSide(w.getDefenders()));

			// Save to file
			File folder = new File("plugins/SimpleFactions/Wars");
			if (!folder.exists()) folder.mkdirs();
			File file = new File(folder, "war_" + w.getId() + ".json");

			FileWriter writer = new FileWriter(file);
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			writer.write(gson.toJson(root));
			writer.flush();
			writer.close();

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	private JSONObject serializeSide(Side side) {
		JSONObject sideJson = new JSONObject();
		sideJson.put("leader", side.getLeader().getId());

		JSONArray participantsArray = new JSONArray();
		for (Participant p : side.getMainParticipants()) {
			participantsArray.add(serializeParticipant(p));
		}
		sideJson.put("participants", participantsArray);

		return sideJson;
	}

	@SuppressWarnings("unchecked")
	private JSONObject serializeParticipant(Participant p) {
		JSONObject json = new JSONObject();

		json.put("leader", p.getLeader().getId());

		JSONArray subjects = new JSONArray();
		for (Faction s : p.getSubjects()) {
			subjects.add(s.getId());
		}
		json.put("subjects", subjects);

		JSONObject allies = new JSONObject();
		for (Map.Entry<Faction, Boolean> entry : p.getAllies().entrySet()) {
			allies.put(entry.getKey().getId(), entry.getValue());
		}
		json.put("allies", allies);

		JSONObject warGoals = new JSONObject();
		for (Map.Entry<Faction, WarGoal> entry : p.getWarGoals().entrySet()) {
			warGoals.put(entry.getKey().getId(), entry.getValue().getId());
		}
		json.put("warGoals", warGoals);

		json.put("civilWar", p.isCivilWar());

		return json;
	}

	public void deleteWar(War w){
		File folder = new File("plugins/SimpleFactions/Wars");
		if (!folder.exists()) return;
		File file = new File(folder, "war_" + w.getId() + ".json");
		if(file.exists()){
			file.delete();
		}
	}

	public List<War> loadWars() {
		List<War> wars = new ArrayList<>();
		File folder = new File("plugins/SimpleFactions/Wars");
		if (!folder.exists() || !folder.isDirectory()) return wars;

		for (File file : folder.listFiles()) {
			if (!file.getName().endsWith(".json")) continue;

			try (FileReader reader = new FileReader(file)) {
				JSONObject warJson = (JSONObject) parser.parse(reader);

				int id = ((Long) warJson.get("id")).intValue();

				JSONObject atkJson = (JSONObject) warJson.get("attackers");
				JSONObject defJson = (JSONObject) warJson.get("defenders");

				Faction atkLeader = FactionManager.getByString((String) atkJson.get("leader"));
				Faction defLeader = FactionManager.getByString((String) defJson.get("leader"));

				if (atkLeader == null || defLeader == null) continue;

				War war = new War(id, atkLeader, defLeader);

				war.getAttackers().getMainParticipants().clear();
				war.getDefenders().getMainParticipants().clear();

				loadParticipants((JSONArray) atkJson.get("participants"), war.getAttackers());
				loadParticipants((JSONArray) defJson.get("participants"), war.getDefenders());

				wars.add(war);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return wars;
	}

	private void loadParticipants(JSONArray array, Side side) {
		for (Object obj : array) {
			JSONObject pJson = (JSONObject) obj;

			Faction leader = FactionManager.getByString((String) pJson.get("leader"));
			if (leader == null) continue;

			boolean civilWar = (Boolean) pJson.get("civilWar");

			// --- Subjects ---
			List<Faction> subjects = new ArrayList<>();
			JSONArray subjectsArray = (JSONArray) pJson.get("subjects");
			for (Object s : subjectsArray) {
				Faction subject = FactionManager.getByString((String) s);
				if (subject != null) subjects.add(subject);
			}

			// --- Allies ---
			Map<Faction, Boolean> allies = new HashMap<>();
			JSONObject alliesJson = (JSONObject) pJson.get("allies");
			for (Object key : alliesJson.keySet()) {
				String allyId = (String) key;
				Boolean joined = (Boolean) alliesJson.get(key);
				Faction ally = FactionManager.getByString(allyId);
				if (ally != null) allies.put(ally, joined);
			}

			// --- War Goals ---
			Map<Faction, WarGoal> warGoals = new HashMap<>();
			JSONObject warGoalsJson = (JSONObject) pJson.get("warGoals");
			for (Object key : warGoalsJson.keySet()) {
				String targetId = (String) key;
				String goalId = (String) warGoalsJson.get(key);
				Faction target = FactionManager.getByString(targetId);
				WarGoal goal = WarGoalLoader.getByString(goalId);
				if (target != null && goal != null) warGoals.put(target, goal);
			}

			// --- Construct and Add ---
			Participant participant = new Participant(leader, subjects, allies, warGoals, civilWar);
			side.getMainParticipants().add(participant);
		}
	}


	public void loadFactions() {
		Bukkit.getLogger().info("[SimpleFactions] loading factions...");
		File folder = new File("plugins/SimpleFactions/Data");
    	for (final File file : folder.listFiles()) {
            if (!file.isDirectory()) {
            	try {
    				json = (JSONObject) parser.parse(new InputStreamReader(new FileInputStream(file), "UTF-8"));
    				String id = (String) json.get("id");
    				String name = (String) json.get("name");
    				String rgb = (String) json.get("rgb");
    				String leader = (String) json.get("leader");
    				String rulerTitle = (String) json.get("ruler title");
    				String government = (String) json.get("government");
    				String culture = (String) json.get("culture");
    				String religion = (String) json.get("religion");
    				int exCap = 0;
    				if(json.containsKey("extra node capacity")) {
    					exCap = (int) Math.round((Double) json.get("extra node capacity"));
    				}
    				List<String> patterns = new ArrayList<String>();
    				int i = 0;
    				JSONArray patternArray = (JSONArray) json.get("banner");
    				while(i < patternArray.size()) {
    					patterns.add(patternArray.get(i).toString());
    					i++;
    				}
					List<Integer> provinces = new ArrayList<Integer>();
    				i = 0;
    				JSONArray provinceArray = (JSONArray) json.get("provinces");
    				while(i < provinceArray.size()) {
    					provinces.add(((Long) provinceArray.get(i)).intValue());
    					i++;
    				}
    				List<Title> titles = new ArrayList<>();
    				i = 0;
    				JSONArray titleArray = (JSONArray) json.get("titles");
    				while(i < titleArray.size()) {
    					Title t = TitleLoader.getById((String) titleArray.get(i));
    					if(t != null) titles.add(t);
    					i++;
    				}
    				List<String> members = new ArrayList<String>();
    				i = 0;
    				JSONArray memberArray = (JSONArray) json.get("members");
    				while(i < memberArray.size()) {
    					members.add(memberArray.get(i).toString());
    					i++;
    				}
    				List<Modifier> prestigeModifiers = new ArrayList<Modifier>();
    				i = 0;
    				JSONArray prestigeArray = (JSONArray) json.get("prestige modifiers");
    				while(i < prestigeArray.size()) {
    					String type = prestigeArray.get(i).toString().split("\\(")[0];
    					Double amount = Double.parseDouble(prestigeArray.get(i).toString().split("\\(")[1].replace(")", ""));
    					Modifier m = new Modifier(type, amount);
    					prestigeModifiers.add(m);
    					i++;
    				}
    				List<Modifier> wealthModifiers = new ArrayList<Modifier>();
    				i = 0;
    				JSONArray wealthArray = (JSONArray) json.get("wealth modifiers");
    				while(i < wealthArray.size()) {
    					String type = wealthArray.get(i).toString().split("\\(")[0];
    					Double amount = Double.parseDouble(wealthArray.get(i).toString().split("\\(")[1].replace(")", ""));
    					Modifier m = new Modifier(type, amount);
    					wealthModifiers.add(m);
    					i++;
    				}
    				double tax = 5;
    				if(json.containsKey("tax rate")) tax = (Double) json.get("tax rate");
    				Faction f = new Faction(id, rgb, provinces, titles, leader, name, rulerTitle, members, patterns, government, culture, religion, exCap, prestigeModifiers, wealthModifiers, tax);
    				if(((String) json.get("bank")).equalsIgnoreCase("true")) {
    					Chunk c = Bukkit.getServer().getWorld((String) json.get("world")).getChunkAt((int) Math.round((Double) json.get("xPos")), (int) Math.round((Double) json.get("zPos")));
    					Double balance = (Double) json.get("balance");
    					f.setBank(new Bank(f, balance, c));
        				f.updateWealth();
    				}
    			
    				i = 0;
    				JSONArray relationArray = (JSONArray) json.get("relations");
    				while(i < relationArray.size()) {
    					String s = (String) relationArray.get(i);
    					FactionManager.addDBRelation(f, s);
    					i++;
    				}
    				if(json.containsKey("tier index")) {
    					int index = (int) Math.round((Double) json.get("tier index"));
    					f.getTier().setIndex(index);
    				}
					Military m = f.getMilitary();
					if (json.containsKey("military")) {
						i = 0;
    					JSONArray militaryArray = (JSONArray) json.get("military");
						while(i < militaryArray.size()) {
							String info = (String) militaryArray.get(i);
							m.getRegiment(info.split("\\.")[0]).setCurrentSlots(Integer.parseInt(info.split("\\.")[1]));
							i++;
						}
					}
					if (json.containsKey("military queue")) {
						i = 0;
    					JSONArray queueArray = (JSONArray) json.get("military queue");
						while(i < queueArray.size()) {
							String info = (String) queueArray.get(i);
							m.addQueueItem(m.getRegiment(info.split("\\.")[0]), Integer.parseInt(info.split("\\.")[1]));
							i++;
						}
					}
    				FactionManager.factions.add(f);
    				f.updateWealth();
    				//Bukkit.getLogger().info("[SimpleFactions] loaded faction "+f.getId());
    			} catch (Exception ex) {
    				ex.printStackTrace();
    			}
            }
        }
	}
	public void deleteFaction(Faction f) {
    	File file = new File("plugins/SimpleFactions/Data", f.getId()+".json");
    	if(file.exists()) file.delete();
    }
	@SuppressWarnings("unchecked")
	public void saveFaction(Faction f) {
		try {
			File file = new File("plugins/SimpleFactions/Data",f.getId()+".json");
			file.createNewFile();
        	PrintWriter pw = new PrintWriter(file, "UTF-8");
        	pw.print("{");
        	pw.print("}");
        	pw.flush();
        	pw.close();
            HashMap<String, Object> defaults = new HashMap<String, Object>();
        	json = (JSONObject) parser.parse(new InputStreamReader(new FileInputStream(file), "UTF-8"));
        	defaults.put("id", f.getId());
        	defaults.put("name", f.getName());
        	if(f.getBank() != null) {
        		defaults.put("bank", "true");
        		Bank b = f.getBank();
            	defaults.put("world", b.getChunk().getWorld().toString().replace("CraftWorld{name=", "").replace("}", ""));
            	defaults.put("xPos", b.getChunk().getX());
            	defaults.put("zPos", b.getChunk().getZ());
            	defaults.put("balance", b.getWealth());
        	} else {
        		defaults.put("bank", "false");
        	}
        	defaults.put("government", f.getGovernment());
        	defaults.put("culture", f.getCulture());
        	defaults.put("religion", f.getReligion());
        	defaults.put("rgb", f.getRGB());
        	defaults.put("tax rate", f.getTaxRate());
        	if(f.getExtraNodeCapacity() != null) {
        		defaults.put("extra node capacity", f.getExtraNodeCapacity());
        	}
        	int i = 0;
        	JSONArray provinceArray = new JSONArray();
        	while(i < f.getProvinces().size()) {
        		provinceArray.add(f.getProvinces().get(i));
        		i++;
        	}
        	defaults.put("provinces", provinceArray);
			i = 0;
        	JSONArray militaryArray = new JSONArray();
        	while(i < f.getMilitary().getRegiments().size()) {
				Regiment reg = f.getMilitary().getRegiments().get(i);
				if(reg.isLevy()) {
					i++;
					continue;
				}
        		militaryArray.add(reg.getId()+"."+reg.getCurrentSlots());
        		i++;
        	}
        	defaults.put("military", militaryArray);
			i = 0;
        	JSONArray queueArray = new JSONArray();
        	while(i < f.getMilitary().getQueue().size()) {
				MilitaryExpansion e = f.getMilitary().getQueue().get(i);
        		queueArray.add(e.getRegiment().getId()+"."+e.getTimeLeft());
        		i++;
        	}
        	defaults.put("military queue", queueArray);
        	i = 0;
        	JSONArray titleArray = new JSONArray();
        	while(i < f.getTitles().size()) {
        		titleArray.add(f.getTitles().get(i).getId());
        		i++;
        	}
        	defaults.put("titles", titleArray);
        	JSONArray relationArray = new JSONArray();
        	for(Map.Entry<String, Relation> entry : f.getRelations().entrySet()) {
        		Relation r = entry.getValue();
        		String s = entry.getKey()+"("+r.getType().getId()+"."+r.getAttitude().getId()+"."+r.getOpinion()+")";
        		relationArray.add(s);
        	}
        	defaults.put("relations", relationArray);
        	i = 0;
        	JSONArray bannerArray = new JSONArray();
        	while(i < f.getBannerPatterns().size()) {
        		bannerArray.add(f.getBannerPatterns().get(i));
        		i++;
        	}
        	defaults.put("banner", bannerArray);
        	i = 0;
        	JSONArray modifierArray = new JSONArray();
        	List<FactionModifier> modifiers = new ArrayList<>(f.getModifiers());
        	while(i < modifiers.size()) {
        		if(modifiers.get(i).isTimed()) {
        			FactionModifier m = modifiers.get(i);
        			String modString = m.getType().toString().toLowerCase()+"("+m.getAmount()+");"+m.getTime();
        			modifierArray.add(modString);
        		}
        		i++;
        	}
        	defaults.put("faction modifiers", modifierArray);
        	i = 0;
        	JSONArray members = new JSONArray();
        	while(i < f.getMembers().size()) {
        		members.add(f.getMembers().get(i));
        		i++;
        	}
        	defaults.put("members", members);
        	defaults.put("tier", format.formatId(f.getTier().getFormattedName()));
        	defaults.put("leader", f.getLeader());
        	defaults.put("ruler title", f.getRulerTitle());
        	if(f.getTier().getIndex() != -1) defaults.put("tier index", f.getTier().getIndex());
        	i = 0;
        	JSONArray prestigeModifiers = new JSONArray();
        	while(i < f.getPrestigeModifiers().size()) {
        		String type = f.getPrestigeModifiers().get(i).getType();
        		List<String> ignore = Arrays.asList("Nodes", "Wealth", "Members");
        		if(!ignore.contains(type)) {
        			Double amount = f.getPrestigeModifiers().get(i).getAmount();
            		prestigeModifiers.add(type+"("+amount+")");
        		}
        		i++;
        	}
        	defaults.put("prestige modifiers", prestigeModifiers);
        	i = 0;
        	JSONArray wealthModifiers = new JSONArray();
        	while(i < f.getWealthModifiers().size()) {
        		String type = f.getWealthModifiers().get(i).getType();
        		List<String> ignore = Arrays.asList("Nodes", "Bank");
        		if(!ignore.contains(type)) {
        			Double amount = f.getWealthModifiers().get(i).getAmount();
            		wealthModifiers.add(type+"("+amount+")");
        		}
        		i++;
        	}
        	defaults.put("wealth modifiers", wealthModifiers);
        	if(RelationManager.getOverlord(f) != null) {
        		defaults.put("overlord", RelationManager.getOverlord(f));
        	}
        	save(file, defaults);
        } catch (Throwable ex) {
			ex.printStackTrace();
        }
	}
	@SuppressWarnings("unchecked")
	public boolean save(File file, HashMap<String, Object> defaults) {
	  try {
		  JSONObject toSave = new JSONObject();
	  
	    for (String s : defaults.keySet()) {
	      Object o = defaults.get(s);
	      if (o instanceof String) {
	        toSave.put(s, getString(s, defaults));
	      } else if (o instanceof Double) {
	        toSave.put(s, getDouble(s, defaults));
	      } else if (o instanceof Integer) {
	        toSave.put(s, getInteger(s, defaults));
	      } else if (o instanceof JSONObject) {
	        toSave.put(s, getObject(s, defaults));
	      } else if (o instanceof JSONArray) {
	        toSave.put(s, getArray(s, defaults));
	      }
	    }
	  
	    TreeMap<String, Object> treeMap = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
	    treeMap.putAll(toSave);
	  
	   Gson g = new GsonBuilder().setPrettyPrinting().create();
	   String prettyJsonString = g.toJson(treeMap);
	  
	    FileWriter fw = new FileWriter(file);
	    fw.write(prettyJsonString);
	    fw.flush();
	    fw.close();
	  
	    return true;
	  } catch (Exception ex) {
	    ex.printStackTrace();
	    return false;
	  }
	}
	
	public String getRawData(String key, HashMap<String, Object> defaults) {
	    return json.containsKey(key) ? json.get(key).toString()
	       : (defaults.containsKey(key) ? defaults.get(key).toString() : key);
	  }
	
	  public String getString(String key, HashMap<String, Object> defaults) {
	    return ChatColor.translateAlternateColorCodes('&', getRawData(key, defaults));
	  }
	
	  public boolean getBoolean(String key, HashMap<String, Object> defaults) {
	    return Boolean.valueOf(getRawData(key, defaults));
	  }
	
	  public double getDouble(String key, HashMap<String, Object> defaults) {
	    try {
	      return Double.parseDouble(getRawData(key, defaults));
	    } catch (Exception ex) { }
	    return -1;
	  }
	
	  public double getInteger(String key, HashMap<String, Object> defaults) {
	    try {
	      return Integer.parseInt(getRawData(key, defaults));
	    } catch (Exception ex) { }
	    return -1;
	  }
	 
	  public JSONObject getObject(String key, HashMap<String, Object> defaults) {
	     return json.containsKey(key) ? (JSONObject) json.get(key)
	       : (defaults.containsKey(key) ? (JSONObject) defaults.get(key) : new JSONObject());
	  }
	 
	  public JSONArray getArray(String key, HashMap<String, Object> defaults) {
		     return json.containsKey(key) ? (JSONArray) json.get(key)
		       : (defaults.containsKey(key) ? (JSONArray) defaults.get(key) : new JSONArray());
	  }
}
