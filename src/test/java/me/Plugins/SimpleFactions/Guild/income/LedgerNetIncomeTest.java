package me.Plugins.SimpleFactions.Guild.income;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import me.Plugins.SimpleFactions.Army.Military;
import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Guild.loans.LoanHandler;
import me.Plugins.SimpleFactions.Loaders.InstallationConfigLoader;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.RelationManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Objects.Handler.GuildHandler;
import me.Plugins.SimpleFactions.installation.Installation;
import me.Plugins.SimpleFactions.installation.InstallationKind;
import me.Plugins.SimpleFactions.installation.handler.InstallationHandler;

class LedgerNetIncomeTest {
	private MockedStatic<FactionManager> factionManagerStatic;
	private MockedStatic<RelationManager> relationManagerStatic;

	@BeforeEach
	void setUp() {
		YamlConfiguration config = new YamlConfiguration();
		config.set("installations.fort.daily-upkeep", 50.0);
		config.set("installations.fort.construction-time", 10);
		config.set("installations.fort.slots.static_emplacement", 8);
		config.set("installations.port.daily-upkeep", 20.0);
		config.set("installations.port.construction-time", 10);
		config.set("installations.port.slots.ship", 10);
		config.set("installations.airport.daily-upkeep", 35.0);
		config.set("installations.airport.construction-time", 10);
		config.set("installations.airport.slots.aircraft", 10);
		InstallationConfigLoader.load(config.getConfigurationSection("installations"));

		factionManagerStatic = mockStatic(FactionManager.class);
		factionManagerStatic.when(FactionManager::getAllGuilds).thenReturn(Collections.emptyList());
		FactionManager.factions = new ArrayList<>();

		relationManagerStatic = mockStatic(RelationManager.class);
		relationManagerStatic.when(() -> RelationManager.getSubjects(any())).thenReturn(Collections.emptyList());
	}

	@AfterEach
	void tearDown() {
		if (factionManagerStatic != null) {
			factionManagerStatic.close();
		}
		if (relationManagerStatic != null) {
			relationManagerStatic.close();
		}
		FactionManager.factions = new ArrayList<>();
	}

	@Test
	void installations_includedInNetIncome_forBaseGuild() {
		Faction faction = mock(Faction.class);
		Guild guild = mock(Guild.class);
		InstallationHandler installationHandler = mock(InstallationHandler.class);
		Military military = mock(Military.class);
		Installation fort = new Installation("f1", "Fort", InstallationKind.FORT, 1, 0, 0, 0L);

		when(faction.getInstallationHandler()).thenReturn(installationHandler);
		when(installationHandler.getAll()).thenReturn(List.of(fort));
		when(faction.getMilitary()).thenReturn(military);
		when(military.getTotalUpkeep()).thenReturn(0.0);

		Ledger ledger = baseLedger(faction, guild, true);

		assertEquals(-50.0, ledger.getIncome(Cashflow.INSTALLATIONS));
		assertEquals(-50.0, ledger.getNetIncome());
	}

	@Test
	void militaryUpkeep_shownAndIncludedInNetIncome_forBaseGuild() {
		Faction faction = mock(Faction.class);
		Guild guild = mock(Guild.class);
		Military military = mock(Military.class);
		InstallationHandler installationHandler = mock(InstallationHandler.class);

		when(faction.getMilitary()).thenReturn(military);
		when(military.getTotalUpkeep()).thenReturn(120.0);
		when(faction.getInstallationHandler()).thenReturn(installationHandler);
		when(installationHandler.getAll()).thenReturn(Collections.emptyList());

		Ledger ledger = baseLedger(faction, guild, true);

		assertEquals(-120.0, ledger.getIncome(Cashflow.MILITARY_UPKEEP));
		assertEquals(-120.0, ledger.getNetIncome());
	}

	@Test
	void installationsAndMilitary_returnZero_forSubGuild() {
		Faction faction = mock(Faction.class);
		Guild guild = mock(Guild.class);
		Military military = mock(Military.class);
		InstallationHandler installationHandler = mock(InstallationHandler.class);

		when(faction.getMilitary()).thenReturn(military);
		when(military.getTotalUpkeep()).thenReturn(120.0);
		when(faction.getInstallationHandler()).thenReturn(installationHandler);
		when(installationHandler.getAll()).thenReturn(List.of(
				new Installation("f1", "Fort", InstallationKind.FORT, 1, 0, 0, 0L)));

		Ledger ledger = baseLedger(faction, guild, false);

		assertEquals(0.0, ledger.getIncome(Cashflow.INSTALLATIONS));
		assertEquals(0.0, ledger.getIncome(Cashflow.MILITARY_UPKEEP));
	}

	@Test
	void netIncome_sumsTradeInstallationsAndMilitary() {
		Faction faction = mock(Faction.class);
		Guild guild = mock(Guild.class);
		Military military = mock(Military.class);
		InstallationHandler installationHandler = mock(InstallationHandler.class);

		when(faction.getMilitary()).thenReturn(military);
		when(military.getTotalUpkeep()).thenReturn(100.0);
		when(faction.getInstallationHandler()).thenReturn(installationHandler);
		when(installationHandler.getAll()).thenReturn(List.of(
				new Installation("f1", "Fort", InstallationKind.FORT, 1, 0, 0, 0L)));

		TradeBreakdown breakdown = new TradeBreakdown();
		breakdown.setIncome(200.0);

		Ledger ledger = baseLedger(faction, guild, true, breakdown);

		assertEquals(50.0, ledger.getNetIncome());
	}

	private Ledger baseLedger(Faction faction, Guild guild, boolean baseGuild) {
		return baseLedger(faction, guild, baseGuild, new TradeBreakdown());
	}

	private Ledger baseLedger(Faction faction, Guild guild, boolean baseGuild, TradeBreakdown breakdown) {
		when(guild.isBankrupt()).thenReturn(false);
		when(guild.isBase()).thenReturn(baseGuild);
		when(guild.getFaction()).thenReturn(faction);
		when(guild.getTradeBreakdown()).thenReturn(breakdown);

		LoanHandler loanHandler = mock(LoanHandler.class);
		when(loanHandler.getLoansTaken()).thenReturn(Collections.emptyList());
		when(loanHandler.getLoansGiven()).thenReturn(Collections.emptyList());
		when(guild.getLoanHandler()).thenReturn(loanHandler);

		when(guild.getUpgrades()).thenReturn(Collections.emptyList());
		when(faction.getPenalty()).thenReturn(0.0);

		GuildHandler guildHandler = mock(GuildHandler.class);
		when(faction.getGuildHandler()).thenReturn(guildHandler);
		when(guildHandler.getGuilds()).thenReturn(List.of(guild));

		return new Ledger(guild);
	}
}
