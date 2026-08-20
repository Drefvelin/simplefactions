package me.Plugins.SimpleFactions.War.battle.warband;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import me.Plugins.SimpleFactions.Objects.Faction;

class WarbandMembershipServiceTest {
	private UUID leaderId;
	private UUID memberId;
	private WarbandMembershipService service;

	@BeforeEach
	void setUp() {
		WarbandMembershipService.resetForTests();
		WarbandManager.resetForTests();
		leaderId = UUID.randomUUID();
		memberId = UUID.randomUUID();
		service = WarbandMembershipService.getInstance();
	}

	@Test
	void quit_savesRejoinStateAndRemovesMember() {
		Warband warband = createOpenWarband("alpha", leaderId, memberId);

		service.handleQuit(memberId);

		assertNull(WarbandManager.getByMemberId(memberId));
		WarbandRejoinState state = service.getPendingRejoin(memberId);
		assertNotNull(state);
		assertEquals("alpha", state.getWarbandId());
		assertFalse(state.hasFaction());
		assertEquals(1, warband.getMemberCount());
	}

	@Test
	void join_restoresWhenWarbandStillOpen() {
		createOpenWarband("alpha", leaderId, memberId);
		service.handleQuit(memberId);

		assertTrue(service.attemptRejoin(memberId, "member", null));
		assertNull(service.getPendingRejoin(memberId));
		assertNotNull(WarbandManager.getByMemberId(memberId));
	}

	@Test
	void join_skipsWhenWarbandDeleted() {
		Warband warband = createOpenWarband("alpha", leaderId, memberId);
		service.handleQuit(memberId);
		WarbandManager.deleteWarband(warband);

		assertFalse(service.attemptRejoin(memberId, "member", null));
		assertNull(WarbandManager.getByMemberId(memberId));
	}

	@Test
	void evaluateRejoin_slotFullBlocksRejoin() throws Exception {
		Faction faction = mock(Faction.class);
		when(faction.getId()).thenReturn("fac1");

		Warband warband = createOpenWarband("alpha", leaderId, memberId);
		markAsFaction(warband);
		WarbandSlot slot = new WarbandSlot(1);
		slot.change(1);
		warband.getSlots().put(faction, slot);

		WarbandRejoinState state = new WarbandRejoinState("alpha", faction);

		assertFalse(service.evaluateRejoin(warband, memberId, faction, state));
	}

	@Test
	void evaluateRejoin_factionMismatchBlocksRejoin() throws Exception {
		Faction slotFaction = mock(Faction.class);
		when(slotFaction.getId()).thenReturn("fac1");
		Faction otherFaction = mock(Faction.class);
		when(otherFaction.getId()).thenReturn("fac2");

		Warband warband = createOpenWarband("alpha", leaderId, memberId);
		markAsFaction(warband);
		warband.getSlots().put(slotFaction, new WarbandSlot(5));

		WarbandRejoinState state = new WarbandRejoinState("alpha", slotFaction);

		assertFalse(service.evaluateRejoin(warband, memberId, otherFaction, state));
	}

	private Warband createOpenWarband(String id, UUID leader, UUID member) {
		Warband warband = Warband.createWithMemberIds(id, leader, false, member);
		WarbandManager.addWarband(warband);
		return warband;
	}

	private static void markAsFaction(Warband warband) throws Exception {
		Field factionField = Warband.class.getDeclaredField("faction");
		factionField.setAccessible(true);
		factionField.set(warband, true);
	}
}
