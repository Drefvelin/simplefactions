package me.Plugins.SimpleFactions.government.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.Plugins.SimpleFactions.government.proposal.Proposal;

public class ProposalHandler {
    private Map<String, List<Proposal>> proposals = new HashMap<>();
    
    public boolean canPropose(String member) {
        return proposals.getOrDefault(member, new ArrayList<>()).size() < 2;
    }

    public void clearProposals() {
        proposals.clear();
    }
}
