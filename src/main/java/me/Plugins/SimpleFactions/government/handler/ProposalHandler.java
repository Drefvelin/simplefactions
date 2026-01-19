package me.Plugins.SimpleFactions.government.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.Plugins.SimpleFactions.government.Government;
import me.Plugins.SimpleFactions.government.proposal.Proposal;
import me.Plugins.SimpleFactions.government.proposal.TaxLawChange;
import me.Plugins.SimpleFactions.laws.LawGroup;

public class ProposalHandler {
    private Government gov;

    public ProposalHandler(Government gov) {
        this.gov = gov;
    }

    private List<Proposal> proposals = new ArrayList<>();
    
    public boolean canPropose(String member) {
        return proposals.stream().filter(p -> p.getProposer().equals(member)).count() < 2;
    }

    public boolean canBeProposed(Proposal proposal) {
        if(proposal.isLawProposal()) {
            LawGroup group = gov.getFaction().getLawHandler().getGroupByLaw(proposal.getLaw().getId());
            for(Proposal p : proposals) {
                if(!p.isLawProposal()) continue;
                LawGroup g = gov.getFaction().getLawHandler().getGroupByLaw(p.getLaw().getId());
                if(g.getId().equalsIgnoreCase(group.getId())) return false;
            }
        } else if(proposal.isTaxProposal()) {
            TaxLawChange change = proposal.getTaxChange();
            for(Proposal p : proposals) {
                TaxLawChange c = p.getTaxChange();
                if(c.getTarget().equals(change.getTarget()) && c.getId().equalsIgnoreCase(change.getId())) return false;
            }
        }
        return true;
    }

    public void propose(Proposal proposal) {
        proposals.add(proposal);
    }

    public List<Proposal> getProposals() {
        return proposals;
    }

    public void clearProposals() {
        Bukkit.getPlayer("drefvelin").sendMessage("cleared");
        proposals.clear();
    }

    public List<Proposal> getProposalsByProposer(String proposer) {
        List<Proposal> result = new ArrayList<>();
        for(Proposal p : proposals) {
            if(p.getProposer().equals(proposer)) result.add(p);
        }
        return result;
    }
}
