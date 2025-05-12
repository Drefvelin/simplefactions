package me.Plugins.SimpleFactions;

import java.io.File;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import me.Plugins.SimpleFactions.Loaders.CoinLoader;
import me.Plugins.SimpleFactions.Loaders.ConfigLoader;
import me.Plugins.SimpleFactions.Loaders.RankLoader;
import me.Plugins.SimpleFactions.Loaders.RegimentLoader;
import me.Plugins.SimpleFactions.Loaders.RelationLoader;
import me.Plugins.SimpleFactions.Loaders.TierLoader;
import me.Plugins.SimpleFactions.Loaders.TitleLoader;
import me.Plugins.SimpleFactions.Loaders.WarGoalLoader;
import me.Plugins.SimpleFactions.Managers.BankManager;
import me.Plugins.SimpleFactions.Managers.CommandManager;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.InventoryManager;
import me.Plugins.SimpleFactions.Managers.RequestManager;
import me.Plugins.SimpleFactions.Managers.TitleManager;
import me.Plugins.SimpleFactions.Managers.WarManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Utils.Database;
import me.Plugins.SimpleFactions.Utils.TabCompletion;
import me.Plugins.SimpleFactions.War.War;

public class SimpleFactions extends JavaPlugin{
	public static FileConfiguration config;
	public static SimpleFactions plugin;
	//Classes
	private final ConfigLoader configLoader = new ConfigLoader();
	private final CoinLoader coinLoader = new CoinLoader();
	private final RankLoader rankLoader = new RankLoader();
	private final RegimentLoader regimentLoader = new RegimentLoader();
	private final RelationLoader relationLoader = new RelationLoader();
	private final TierLoader tierLoader = new TierLoader();
	private final TitleLoader titleLoader = new TitleLoader();
	private final WarGoalLoader goalLoader = new WarGoalLoader();
	
	private final CommandManager commands = new CommandManager();
	private final InventoryManager inventoryManager = new InventoryManager();
	private final BankManager bankManager = new BankManager();
	private final Database db = new Database();
	private final FactionManager factionManager = new FactionManager();
	private final TitleManager titleManager = new TitleManager();
	
	@Override
	public void onEnable() {
		config = getConfig();
		plugin = this;
		createFolders();
		createConfigs();
		registerListeners();
		loadConfigs();
		db.loadFactions();
		getCommand(commands.cmd1).setExecutor(commands);
		getCommand(commands.cmd1).setTabCompleter(new TabCompletion());
		factionManager.run();
		RequestManager.start();
		WarManager.start();
	}
	@Override
	public void onDisable() {
		db.saveTimer(FactionManager.getTimer());
		for(Faction f : FactionManager.factions) {
			db.saveFaction(f);
		}
		for(War w : WarManager.get()){
			db.saveWar(w);
		}
	}
	public void loadConfigs() {
		configLoader.loadConfig(new File(getDataFolder(), "config.yml"));
		coinLoader.loadCoins(new File(getDataFolder(), "coins.yml"));
		rankLoader.loadRanks(new File(getDataFolder(), "ranks.yml"));
		regimentLoader.loadRegiments(new File(getDataFolder(), "regiments.yml"));
		relationLoader.loadRelationTypes(new File(getDataFolder(), "diplomacy.yml"));
		relationLoader.loadAttitudes(new File(getDataFolder(), "diplomacy.yml"));
		tierLoader.load(new File(getDataFolder(), "tiers.yml"));
		goalLoader.load(new File(getDataFolder(), "wargoals.yml"));
		titleLoader.loadAll();
	}
	public void registerListeners() {
		getServer().getPluginManager().registerEvents(inventoryManager, this);
		getServer().getPluginManager().registerEvents(bankManager, this);
		getServer().getPluginManager().registerEvents(titleManager, this);
	}
	public void createFolders() {
		if (!getDataFolder().exists()) getDataFolder().mkdir();
		File subFolder = new File(getDataFolder(), "Data");
		if(!subFolder.exists()) subFolder.mkdir();
		subFolder = new File(getDataFolder(), "PlayerData");
		if(!subFolder.exists()) subFolder.mkdir();
		subFolder = new File(getDataFolder(), "Wars");
		if(!subFolder.exists()) subFolder.mkdir();
		subFolder = new File(getDataFolder(), "Cache");
		if(!subFolder.exists()) subFolder.mkdir();
		subFolder = new File(getDataFolder(), "MapAPI");
		if(!subFolder.exists()) subFolder.mkdir();
		subFolder = new File(getDataFolder(), "Input");
		if(!subFolder.exists()) subFolder.mkdir();
	}
	public static int getMaxExtraNodeCapacity() {
		return Cache.maxExtraNodeCapacity;
	}
	public void createConfigs() {
		String[] files = {
				"coins.yml",
				"regiments.yml",
				"diplomacy.yml",
				"ranks.yml",
				"config.yml",
				"tiers.yml",
				};
		for(String s : files) {
			File newConfigFile = new File(getDataFolder(), s);
	        if (!newConfigFile.exists()) {
	        	newConfigFile.getParentFile().mkdirs();
	            saveResource(s, false);
	        }
		}
	}
}
