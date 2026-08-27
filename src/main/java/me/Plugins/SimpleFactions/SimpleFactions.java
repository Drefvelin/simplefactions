package me.Plugins.SimpleFactions;

import java.io.File;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
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
import me.Plugins.SimpleFactions.Loaders.InstallationConfigLoader;
import me.Plugins.SimpleFactions.installation.InstallationProtectionListener;
import me.Plugins.SimpleFactions.Loaders.VehiclesConfigLoader;
import me.Plugins.SimpleFactions.Loaders.WarGoalLoader;
import me.Plugins.SimpleFactions.Managers.BankManager;
import me.Plugins.SimpleFactions.Managers.CommandManager;
import me.Plugins.SimpleFactions.Managers.LedgerCommandManager;
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
import me.Plugins.SimpleFactions.War.campaign.runtime.BattleScheduleTickService;
import me.Plugins.SimpleFactions.War.campaign.ui.CampaignViewRefreshService;
import me.Plugins.SimpleFactions.War.core.War;
import me.Plugins.SimpleFactions.War.core.WarCommandManager;
import me.Plugins.SimpleFactions.War.core.WarTabCompletion;
import me.Plugins.SimpleFactions.War.battle.campaign.CampaignBattleOutcomeService;
import me.Plugins.SimpleFactions.War.battle.engine.core.BattleManager;
import me.Plugins.SimpleFactions.War.battle.engine.core.BattleProvinceBlockProtectionListener;
import me.Plugins.SimpleFactions.War.battle.ui.BattleCommandManager;
import me.Plugins.SimpleFactions.War.battle.ui.BattleTabCompletion;
import me.Plugins.SimpleFactions.War.campaign.raid.RaidCommandManager;
import me.Plugins.SimpleFactions.War.campaign.raid.RaidTabCompletion;
import me.Plugins.SimpleFactions.War.battle.warband.WarbandManager;
import me.Plugins.SimpleFactions.War.battle.warband.WarbandMembershipListener;
import me.Plugins.SimpleFactions.War.battle.template.BattleTemplateService;
import me.Plugins.SimpleFactions.War.campaign.raid.CampaignRaidBattleEndService;
import me.Plugins.SimpleFactions.War.campaign.raid.CampaignRaidIntruderListener;
import me.Plugins.SimpleFactions.War.campaign.raid.CampaignRaidIntruderTickService;
import me.Plugins.SimpleFactions.War.campaign.raid.CampaignRaidWarbandListener;
import me.Plugins.SimpleFactions.vehicles.BattleVehicleEligibilityListener;
import me.Plugins.SimpleFactions.vehicles.InstallationVehicleOwnerSync;
import me.Plugins.SimpleFactions.vehicles.InstallationVehicleService;
import me.Plugins.SimpleFactions.vehicles.InstallationVehicleUnberthService;
import me.Plugins.SimpleFactions.vehicles.PlayerVehicleRegistry;
import me.Plugins.SimpleFactions.vehicles.VehicleIntegrationListener;
import me.Plugins.SimpleFactions.vehicles.VehicleRegistryClaimListener;
import me.Plugins.SimpleFactions.vehicles.VehicleRegistryClaimService;
import me.Plugins.SimpleFactions.vehicles.VehicleRegistryPersistence;
import me.Plugins.SimpleFactions.vehicles.VehicleSpawnListener;
import me.Plugins.SimpleFactions.vehicles.VehicleTransferConsentService;
import me.Plugins.SimpleFactions.vehicles.VehicleTransferListener;
import me.Plugins.SimpleFactions.vehicles.VehicleTransferSession;
import me.Plugins.SimpleFactions.vehicles.VehicleTransferSessionManager;
import me.Plugins.SimpleFactions.vehicles.VehicleUpkeepService;
import me.Plugins.SimpleFactions.player.PlayerEconomyManager;

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
	private final LedgerCommandManager ledgerCommandManager = new LedgerCommandManager(inventoryManager);
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
	private final BattleProvinceBlockProtectionListener battleProvinceBlockProtectionListener =
			new BattleProvinceBlockProtectionListener();
	private final WarbandManager warbandManager = new WarbandManager();
	private final BattleCommandManager battleCommandManager = new BattleCommandManager();
	private final RaidCommandManager raidCommandManager = new RaidCommandManager();
	private final WarCommandManager warCommandManager = new WarCommandManager();
	private final WarbandMembershipListener warbandMembershipListener = new WarbandMembershipListener();
	private final CampaignRaidWarbandListener campaignRaidWarbandListener = new CampaignRaidWarbandListener();
	private final CampaignRaidIntruderListener campaignRaidIntruderListener = new CampaignRaidIntruderListener();
	private final CampaignRaidBattleEndService campaignRaidBattleEndService = new CampaignRaidBattleEndService();
	private final CampaignBattleOutcomeService campaignBattleOutcomeService = new CampaignBattleOutcomeService();
	private final InstallationProtectionListener installationProtectionListener =
			new InstallationProtectionListener();
	private ProvinceManager provinceSnapshot = new ProvinceManager();
	private ProvinceGrid provinceGrid;
	private final PlayerVehicleRegistry vehicleRegistry = new PlayerVehicleRegistry();
	private VehicleRegistryPersistence vehicleRegistryPersistence;
	private final InstallationVehicleOwnerSync installationVehicleOwnerSync =
			new InstallationVehicleOwnerSync(vehicleRegistry);
	private final InstallationVehicleService installationVehicleService =
			new InstallationVehicleService(vehicleRegistry, installationVehicleOwnerSync);
	private final InstallationVehicleUnberthService installationVehicleUnberthService =
			new InstallationVehicleUnberthService(vehicleRegistry);
	private final VehicleTransferSessionManager vehicleTransferSessionManager =
			new VehicleTransferSessionManager();
	private final VehicleTransferConsentService vehicleTransferConsentService =
			new VehicleTransferConsentService(
					installationVehicleService,
					vehicleRegistry,
					vehicleTransferSessionManager);
	private final VehicleRegistryClaimService vehicleRegistryClaimService =
			new VehicleRegistryClaimService(vehicleRegistry);
	private final VehicleRegistryClaimListener vehicleRegistryClaimListener =
			new VehicleRegistryClaimListener(vehicleRegistryClaimService);
	private final VehicleIntegrationListener vehicleIntegrationListener = new VehicleIntegrationListener();
	private final VehicleTransferListener vehicleTransferListener = new VehicleTransferListener(
			vehicleTransferSessionManager,
			vehicleRegistry,
			installationVehicleService,
			vehicleTransferConsentService);
	private final VehicleSpawnListener vehicleSpawnListener =
			new VehicleSpawnListener(installationVehicleOwnerSync);
	private final BattleVehicleEligibilityListener battleVehicleEligibilityListener =
			new BattleVehicleEligibilityListener(vehicleRegistry);
	private boolean vehicleIntegrationRegistered = false;
	private final PlayerEconomyManager playerEconomyManager = new PlayerEconomyManager();
	private final VehicleUpkeepService vehicleUpkeepService = new VehicleUpkeepService(
		vehicleRegistry,
		playerEconomyManager);
	
	@Override
	public void onEnable() {
		config = getConfig();
		plugin = this;
		FactionManager.inv = inventoryManager;
		createFolders();
		createConfigs();
		registerListeners();
		loadConfigs();
		vehicleRegistryPersistence = new VehicleRegistryPersistence(
			new File(getDataFolder(), "Cache"),
			vehicleRegistry);
		vehicleRegistryPersistence.load();
		registerVehicleIntegrationHooks();
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
		getCommand(ledgerCommandManager.cmd).setExecutor(ledgerCommandManager);
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
		CampaignRaidIntruderTickService.start();
		warbandManager.start();
		battleManager.start();
		getCommand(battleCommandManager.cmd1).setExecutor(battleCommandManager);
		getCommand(battleCommandManager.cmd2).setExecutor(battleCommandManager);
		BattleTabCompletion battleTabCompletion = new BattleTabCompletion();
		getCommand(battleCommandManager.cmd1).setTabCompleter(battleTabCompletion);
		getCommand(battleCommandManager.cmd2).setTabCompleter(battleTabCompletion);
		RaidTabCompletion raidTabCompletion = new RaidTabCompletion();
		getCommand(raidCommandManager.cmd).setExecutor(raidCommandManager);
		getCommand(raidCommandManager.cmd).setTabCompleter(raidTabCompletion);
		WarTabCompletion warTabCompletion = new WarTabCompletion();
		getCommand(WarCommandManager.CMD).setExecutor(warCommandManager);
		getCommand(WarCommandManager.CMD).setTabCompleter(warTabCompletion);
		RequestManager.start();
		WarManager.start();
		me.Plugins.SimpleFactions.War.battle.persistence.BattlePersistenceService.loadAll();
		me.Plugins.SimpleFactions.War.battle.persistence.BattlePersistenceService.startAutosave();
		BattleScheduleTickService.start();
		CampaignViewRefreshService.start();
		sessionManager.start();
		provinceSnapshot = provinceManager.createSnapshotShell();
		provinceManager.recalculate();
		inventoryManager.start();
	}
	@Override
	public void onDisable() {
		CampaignViewRefreshService.stop();
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
		if (vehicleRegistryPersistence != null) {
			vehicleRegistryPersistence.save();
		}
	}
	public void loadConfigs() {
		configLoader.loadConfig(new File(getDataFolder(), "config.yml"));
		VehiclesConfigLoader.load(new File(getDataFolder(), "vehicles.yml"));
		InstallationConfigLoader.load(new File(getDataFolder(), "installations.yml"));
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
		getServer().getPluginManager().registerEvents(battleProvinceBlockProtectionListener, this);
		getServer().getPluginManager().registerEvents(warbandManager, this);
		getServer().getPluginManager().registerEvents(warbandMembershipListener, this);
		getServer().getPluginManager().registerEvents(campaignRaidWarbandListener, this);
		getServer().getPluginManager().registerEvents(campaignRaidIntruderListener, this);
		getServer().getPluginManager().registerEvents(campaignRaidBattleEndService, this);
		getServer().getPluginManager().registerEvents(campaignBattleOutcomeService, this);
		getServer().getPluginManager().registerEvents(installationProtectionListener, this);
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
			"Guilds",
			"logs"
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
				"vehicles.yml",
				"installations.yml",
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

	public static void reloadConfigs() {
		plugin.loadConfigs();
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

	public static PlayerVehicleRegistry getVehicleRegistry() {
		return plugin.vehicleRegistry;
	}

	public VehicleTransferSessionManager getVehicleTransferSessionManager() {
		return vehicleTransferSessionManager;
	}

	public VehicleTransferConsentService getVehicleTransferConsentService() {
		return vehicleTransferConsentService;
	}

	public InstallationVehicleUnberthService getInstallationVehicleUnberthService() {
		return installationVehicleUnberthService;
	}

	public static PlayerEconomyManager getPlayerEconomyManager() {
		return plugin.playerEconomyManager;
	}

	public VehicleUpkeepService getVehicleUpkeepService() {
		return vehicleUpkeepService;
	}

	public void saveVehicleRegistry() {
		if (vehicleRegistryPersistence != null) {
			vehicleRegistryPersistence.save();
		}
	}

	private void registerVehicleIntegrationHooks() {
		getServer().getPluginManager().registerEvents(new Listener() {
			@EventHandler
			public void onPluginEnable(PluginEnableEvent event) {
				if ("VFBuilders".equalsIgnoreCase(event.getPlugin().getName())
						|| "VehicleFramework".equalsIgnoreCase(event.getPlugin().getName())) {
					registerVehicleIntegration();
				}
			}
		}, this);
		registerVehicleIntegration();
	}

	private void registerVehicleIntegration() {
		if (vehicleIntegrationRegistered) {
			return;
		}
		if (!getServer().getPluginManager().isPluginEnabled("VehicleFramework")) {
			return;
		}
		getServer().getPluginManager().registerEvents(vehicleIntegrationListener, this);
		getServer().getPluginManager().registerEvents(vehicleRegistryClaimListener, this);
		getServer().getPluginManager().registerEvents(vehicleTransferListener, this);
		getServer().getPluginManager().registerEvents(vehicleSpawnListener, this);
		getServer().getPluginManager().registerEvents(battleVehicleEligibilityListener, this);
		vehicleIntegrationRegistered = true;
		if (getServer().getPluginManager().isPluginEnabled("VFBuilders")) {
			getLogger().info("[SimpleFactions] VFBuilders vehicle integration enabled");
		} else {
			getLogger().info("[SimpleFactions] VehicleFramework vehicle integration enabled");
		}
	}

	public BattleTemplateService getBattleTemplateService() {
		return BattleTemplateService.getInstance();
	}
}
