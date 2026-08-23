package me.Plugins.SimpleFactions;

import java.io.File;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.Map.ProvinceGrid;
import me.Plugins.SimpleFactions.Map.presence.ProvincePresenceListener;
import me.Plugins.SimpleFactions.Map.presence.ProvincePresenceService;
import me.Plugins.SimpleFactions.Map.presence.ProvincePresenceTickService;
import me.Plugins.SimpleFactions.Database.Database;
import me.Plugins.SimpleFactions.Loaders.BattleTemplateLoader;
import me.Plugins.SimpleFactions.Loaders.BranchLoader;
import me.Plugins.SimpleFactions.Loaders.ConfigLoader;
import me.Plugins.SimpleFactions.Loaders.GuildLoader;
import me.Plugins.SimpleFactions.Loaders.LawLoader;
import me.Plugins.SimpleFactions.Loaders.PoliticalActionLoader;
import me.Plugins.SimpleFactions.Loaders.ProvinceLoader;
import me.Plugins.SimpleFactions.Loaders.RankLoader;
import me.Plugins.SimpleFactions.Loaders.RegimentLoader;
import me.Plugins.SimpleFactions.Loaders.RelationLoader;
import me.Plugins.SimpleFactions.Loaders.TierLoader;
import me.Plugins.SimpleFactions.Loaders.TitleLoader;
import me.Plugins.SimpleFactions.Loaders.UpgradeLoader;
import me.Plugins.SimpleFactions.Loaders.WarGoalLoader;
import me.Plugins.SimpleFactions.Managers.BankManager;
import me.Plugins.SimpleFactions.Managers.CommandManager;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.InventoryManager;
import me.Plugins.SimpleFactions.Managers.PlayerManager;
import me.Plugins.SimpleFactions.Managers.ProvinceManager;
import me.Plugins.SimpleFactions.Managers.RelocationPrompt;
import me.Plugins.SimpleFactions.Managers.CapitalMovePrompt;
import me.Plugins.SimpleFactions.Managers.RequestManager;
import me.Plugins.SimpleFactions.Managers.SessionManager;
import me.Plugins.SimpleFactions.Managers.TitleManager;
import me.Plugins.SimpleFactions.Managers.WarManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Utils.TabCompletion;
import me.Plugins.SimpleFactions.War.schedule.BattleScheduleTickService;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.battle.campaign.CampaignBattleOutcomeService;
import me.Plugins.SimpleFactions.War.battle.engine.BattleManager;
import me.Plugins.SimpleFactions.War.battle.ui.BattleCommandManager;
import me.Plugins.SimpleFactions.War.battle.ui.BattleTabCompletion;
import me.Plugins.SimpleFactions.War.battle.warband.WarbandManager;
import me.Plugins.SimpleFactions.War.battle.template.BattleTemplateService;
import me.Plugins.SimpleFactions.War.battle.warband.WarbandMembershipListener;

public class SimpleFactions extends JavaPlugin{
	public static FileConfiguration config;
	public static SimpleFactions plugin;
	//Loaders
	private final ConfigLoader configLoader = new ConfigLoader();
	//private final CoinLoader coinLoader = new CoinLoader(); No longer in use
	private final RankLoader rankLoader = new RankLoader();
	private final RegimentLoader regimentLoader = new RegimentLoader();
	private final RelationLoader relationLoader = new RelationLoader();
	private final TierLoader tierLoader = new TierLoader();
	private static final TitleLoader titleLoader = new TitleLoader();
	private final WarGoalLoader goalLoader = new WarGoalLoader();
	private final BranchLoader branchLoader = new BranchLoader();
	private final UpgradeLoader upgradeLoader = new UpgradeLoader();
	private final GuildLoader guildLoader = new GuildLoader();
	private final LawLoader lawLoader = new LawLoader();
	private final ProvinceLoader provinceLoader = new ProvinceLoader();
	private final PoliticalActionLoader politicalActionLoader = new PoliticalActionLoader();
	private final BattleTemplateLoader battleTemplateLoader = new BattleTemplateLoader();
	
	//Managers
	private final ProvinceManager provinceManager = new ProvinceManager();
	private final CommandManager commands = new CommandManager();
	private final InventoryManager inventoryManager = new InventoryManager();
	private final BankManager bankManager = new BankManager();
	private final Database db = new Database();
	private final FactionManager factionManager = new FactionManager();
	private final TitleManager titleManager = new TitleManager();
	private final PlayerManager playerManager = new PlayerManager();
	private final SessionManager sessionManager = new SessionManager();
	private final RelocationPrompt relocationPrompt = new RelocationPrompt();
	private final CapitalMovePrompt capitalMovePrompt = new CapitalMovePrompt();
	private final ProvincePresenceListener provincePresenceListener = new ProvincePresenceListener();
	private final BattleManager battleManager = new BattleManager();
	private final WarbandManager warbandManager = new WarbandManager();
	private final BattleCommandManager battleCommandManager = new BattleCommandManager();
	private final WarbandMembershipListener warbandMembershipListener = new WarbandMembershipListener();
	private final CampaignBattleOutcomeService campaignBattleOutcomeService = new CampaignBattleOutcomeService();
	private ProvinceManager provinceSnapshot = new ProvinceManager();
	private ProvinceGrid provinceGrid;
	
	@Override
	public void onEnable() {
		config = getConfig();
		plugin = this;
		FactionManager.inv = inventoryManager;
		createFolders();
		createConfigs();
		registerListeners();
		loadConfigs();
		if (Cache.mapEnabled && !getServer().getPluginManager().isPluginEnabled("TFMCWeb")) {
			getLogger().severe(
				"[SimpleFactions] enable-map is true but TFMCWeb is not loaded. "
				+ "Map upload, province lookup, and regen require TFMCWeb + api.base-url / api.plugin-key."
			);
		}
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
		try {
			provinceGrid = ProvinceGrid.load(new File(getDataFolder(), "Input/province_id_grid.bin.gz"));
			getLogger().info(
					"Loaded province_id_grid "
							+ provinceGrid.getWidth()
							+ "x"
							+ provinceGrid.getHeight());
		} catch (Exception e) {
			getLogger().severe("Failed to load province_id_grid.bin.gz! Plugin disabled.");
			e.printStackTrace();
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		factionManager.run();
		ProvincePresenceTickService.start();
		warbandManager.start();
		battleManager.start();
		getCommand(battleCommandManager.cmd1).setExecutor(battleCommandManager);
		getCommand(battleCommandManager.cmd2).setExecutor(battleCommandManager);
		BattleTabCompletion battleTabCompletion = new BattleTabCompletion();
		getCommand(battleCommandManager.cmd1).setTabCompleter(battleTabCompletion);
		getCommand(battleCommandManager.cmd2).setTabCompleter(battleTabCompletion);
		RequestManager.start();
		WarManager.start();
		me.Plugins.SimpleFactions.War.battle.persistence.BattlePersistenceService.loadAll();
		me.Plugins.SimpleFactions.War.battle.persistence.BattlePersistenceService.startAutosave();
		BattleScheduleTickService.start();
		sessionManager.start();
		provinceSnapshot = provinceManager.createSnapshotShell();
		provinceManager.recalculate();
		inventoryManager.start();
	}
	@Override
	public void onDisable() {
		BattleManager.shutdown();
		me.Plugins.SimpleFactions.War.battle.persistence.BattlePersistenceService.stopAutosave();
		me.Plugins.SimpleFactions.War.battle.persistence.BattlePersistenceService.saveAll();
		sessionManager.end();
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
		me.Plugins.SimpleFactions.Managers.LogManager.configure(
				Cache.loggingEnabled,
				Cache.wipeLog,
				getDataFolder());
		//coinLoader.loadCoins(new File(getDataFolder(), "coins.yml"));
		rankLoader.loadRanks(new File(getDataFolder(), "ranks.yml"));
		regimentLoader.loadRegiments(new File(getDataFolder(), "regiments.yml"));
		relationLoader.loadRelationTypes(new File(getDataFolder(), "diplomacy.yml"));
		relationLoader.loadAttitudes(new File(getDataFolder(), "diplomacy.yml"));
		politicalActionLoader.load(new File(getDataFolder(), "political-actions.yml"));
		lawLoader.load(new File(getDataFolder(), "laws.yml"));
		tierLoader.load(new File(getDataFolder(), "tiers.yml"));
		goalLoader.load(new File(getDataFolder(), "wargoals.yml"));
		battleTemplateLoader.load(new File(getDataFolder(), "battle-templates.yml"));
		guildLoader.load(new File(getDataFolder(), "Guilds/guild-types.yml"));
		branchLoader.load(new File(getDataFolder(), "Guilds/branches.yml"));
		upgradeLoader.load(new File(getDataFolder(), "Guilds/upgrades.yml"));
		titleLoader.loadAll();
	}
	public void registerListeners() {
		getServer().getPluginManager().registerEvents(commands, this);
		getServer().getPluginManager().registerEvents(inventoryManager, this);
		getServer().getPluginManager().registerEvents(bankManager, this);
		getServer().getPluginManager().registerEvents(titleManager, this);
		getServer().getPluginManager().registerEvents(playerManager, this);
		getServer().getPluginManager().registerEvents(sessionManager, this);
		getServer().getPluginManager().registerEvents(relocationPrompt, this);
		getServer().getPluginManager().registerEvents(capitalMovePrompt, this);
		getServer().getPluginManager().registerEvents(factionManager, this);
		getServer().getPluginManager().registerEvents(provincePresenceListener, this);
		getServer().getPluginManager().registerEvents(battleManager, this);
		getServer().getPluginManager().registerEvents(warbandManager, this);
		getServer().getPluginManager().registerEvents(warbandMembershipListener, this);
		getServer().getPluginManager().registerEvents(campaignBattleOutcomeService, this);
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
				"regiments.yml",
				"diplomacy.yml",
				"ranks.yml",
				"config.yml",
				"tiers.yml",
				"laws.yml",
				"political-actions.yml",
				"Guilds/guild-types.yml",
				"Guilds/branches.yml",
				"Guilds/upgrades.yml",
				"battle-templates.yml",
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

	public static SimpleFactions getInstance() {
		return plugin;
	}

	public ProvinceManager getProvinceManager() {
		return provinceManager;
	}

	public ProvinceManager getProvinceSnapshot() {
		return provinceSnapshot;
	}

	public ProvinceGrid getProvinceGrid() {
		return provinceGrid;
	}

	public SessionManager getSessionManager() {
		return sessionManager;
	}

	public ProvincePresenceService getProvincePresenceService() {
		return ProvincePresenceService.getInstance();
	}

	public BattleTemplateService getBattleTemplateService() {
		return BattleTemplateService.getInstance();
	}
}
