package me.Plugins.SimpleFactions;

import java.io.File;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import me.Plugins.SimpleFactions.Database.Database;
import me.Plugins.SimpleFactions.Loaders.BranchLoader;
import me.Plugins.SimpleFactions.Loaders.CoinLoader;
import me.Plugins.SimpleFactions.Loaders.ConfigLoader;
import me.Plugins.SimpleFactions.Loaders.GuildLoader;
import me.Plugins.SimpleFactions.Loaders.ProvinceLoader;
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
import me.Plugins.SimpleFactions.Managers.PlayerManager;
import me.Plugins.SimpleFactions.Managers.ProvinceManager;
import me.Plugins.SimpleFactions.Managers.RequestManager;
import me.Plugins.SimpleFactions.Managers.TitleManager;
import me.Plugins.SimpleFactions.Managers.WarManager;
import me.Plugins.SimpleFactions.Objects.Faction;
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
	private static final TitleLoader titleLoader = new TitleLoader();
	private final WarGoalLoader goalLoader = new WarGoalLoader();
	private final BranchLoader branchLoader = new BranchLoader();
	private final GuildLoader guildLoader = new GuildLoader();
	
	private final CommandManager commands = new CommandManager();
	private final InventoryManager inventoryManager = new InventoryManager();
	private final BankManager bankManager = new BankManager();
	private final Database db = new Database();
	private final FactionManager factionManager = new FactionManager();
	private final TitleManager titleManager = new TitleManager();
	private final PlayerManager playerManager = new PlayerManager();
	private final ProvinceManager provinceManager = new ProvinceManager();
	private final ProvinceLoader provinceLoader = new ProvinceLoader();
	
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
		getCommand(commands.cmd2).setExecutor(commands);
		getCommand(commands.cmd1).setTabCompleter(new TabCompletion());
		getCommand(commands.cmd2).setTabCompleter(new TabCompletion());
		try {
			provinceManager.start(
				provinceLoader.loadProvinces(
					new File(getDataFolder(), "Input/provinces.txt"),
					new File(getDataFolder(), "Input/province_neighbors.json")
				)
			);
		} catch (Exception e) {
			getLogger().severe("Failed to load provinces! Plugin disabled.");
			e.printStackTrace();
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		factionManager.run();
		RequestManager.start();
		WarManager.start();
		inventoryManager.start();
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
		guildLoader.load(new File(getDataFolder(), "Guilds/guild-types.yml"));
		branchLoader.load(new File(getDataFolder(), "Guilds/branches.yml"));
		titleLoader.loadAll();
	}
	public void registerListeners() {
		getServer().getPluginManager().registerEvents(commands, this);
		getServer().getPluginManager().registerEvents(inventoryManager, this);
		getServer().getPluginManager().registerEvents(bankManager, this);
		getServer().getPluginManager().registerEvents(titleManager, this);
		getServer().getPluginManager().registerEvents(playerManager, this);
	}
	public void createFolders() {
		File dataFolder = getDataFolder();
		if (!dataFolder.exists()) dataFolder.mkdir();

		String[] subFolders = {
			"Data",
			"PlayerData",
			"Wars",
			"Cache",
			"MapAPI",
			"Input",
			"Guilds"
		};

		for (String name : subFolders) {
			File folder = new File(dataFolder, name);
			if (!folder.exists()) folder.mkdir();
		}
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
				"Guilds/guild-types.yml",
				"Guilds/branches.yml",
				};
		for(String s : files) {
			File newConfigFile = new File(getDataFolder(), s);
	        if (!newConfigFile.exists()) {
	        	newConfigFile.getParentFile().mkdirs();
	            saveResource(s, false);
	        }
		}
	}

	public static void reloadTitles() {
		titleLoader.reload();
	}
}
