package me.Plugins.SimpleFactions.government.proposal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.government.Government;
import me.Plugins.SimpleFactions.government.movement.Action;
import me.Plugins.SimpleFactions.government.movement.PoliticalAction;

class ProposalWantedLeaderTest {

	@Test
	void checkTarget_allowsWhenCanBecomeLeader() {
		Proposal proposal = changeLeaderProposal(true);
		proposal.setTarget("Bob");
		assertTrue(proposal.checkTarget());
	}

	@Test
	void checkTarget_rejectsWhenCannotBecomeLeader() {
		Proposal proposal = changeLeaderProposal(false);
		proposal.setTarget("Alice");
		assertFalse(proposal.checkTarget());
	}

	private static Proposal changeLeaderProposal(boolean canLead) {
		Government government = mock(Government.class);
		Faction faction = mock(Faction.class);
		when(government.getFaction()).thenReturn(faction);
		when(faction.canBecomeLeader("Bob")).thenReturn(canLead);
		when(faction.canBecomeLeader("Alice")).thenReturn(canLead);
		Proposal proposal = new Proposal("Alice", government);
		proposal.setPoliticalActionProposal(new PoliticalAction(Action.CHANGE_LEADER));
		return proposal;
	}
}
