package me.Plugins.SimpleFactions.War.civilwar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.mockStatic;

import org.bukkit.Bukkit;

import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.installation.Installation;
import me.Plugins.SimpleFactions.installation.InstallationKind;
import me.Plugins.SimpleFactions.installation.handler.InstallationHandler;

class CivilWarLandSplitApplyTest {

	@Test
	void apply_transfersInstallsNotDissolved() {
		Faction host = mock(Faction.class);
		Faction rebels = mock(Faction.class);
		InstallationHandler hostHandler = new InstallationHandler(host);
		InstallationHandler rebelHandler = new InstallationHandler(rebels);
		when(host.getId()).thenReturn("host");
		when(rebels.getId()).thenReturn("rebels");
		when(host.getInstallationHandler()).thenReturn(hostHandler);
		when(rebels.getInstallationHandler()).thenReturn(rebelHandler);
		hostHandler.acceptTransferred(new Installation("port-1", "Harbour", InstallationKind.PORT, 20, 0, 0, 1L));

		CivilWarLandSplitService.LandSplitPlan plan =
				new CivilWarLandSplitService.LandSplitPlan(java.util.List.of(20), java.util.List.of(10));

		try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
			bukkit.when(() -> Bukkit.getPlayerExact(org.mockito.ArgumentMatchers.anyString())).thenReturn(null);
			CivilWarLandSplitService.apply(host, rebels, plan);
		}

		assertNull(hostHandler.getById("port-1"));
		assertNotNull(rebelHandler.getById("port-1"));
		assertEquals(20, rebelHandler.getById("port-1").getProvince());
	}
}