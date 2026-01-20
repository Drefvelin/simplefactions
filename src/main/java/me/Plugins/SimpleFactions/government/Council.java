package me.Plugins.SimpleFactions.government;

import java.util.ArrayList;
import java.util.List;

import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Utils.Wealth;
import me.Plugins.SimpleFactions.enums.Rules;
import me.Plugins.SimpleFactions.government.handler.ProposalHandler;
import me.Plugins.SimpleFactions.government.proposal.Proposal;
import me.Plugins.SimpleFactions.government.session.Session;
import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Managers.RelationManager;

public class Council {
    private Faction f;
    private List<String> members = new ArrayList<>();
    private Rules type;
    private int size;

    private ProposalHandler proposalHandler;

    public Council(Government gov, Faction f) {
        this.f = f;
        this.proposalHandler = new ProposalHandler(gov);
        this.size = f.getCouncilSize();
        this.type = f.getCouncilType();
    }

    public boolean canPropose(String player) {
        if(!members.contains(player)) return false;
        return proposalHandler.canPropose(player);
    }

    public void reorganize() {
        int newSize = f.getCouncilSize();
        Rules newType = f.getCouncilType();
        if(newSize < size && members.size() > newSize) {
            members = members.subList(0, newSize); //Trim council if size reduced
        }
        if(newType != type) {
            if(newType.equals(Rules.APPOINTED_COUNCIL)) members.clear(); //Leader appoints new council
            else if(newType.equals(Rules.WEALTH_BASED_COUNCIL)) members.clear(); //Wealth based council needs to be reselected
            //Elected council gets to keep members until the next election.
            proposalHandler.clearProposals(); //Council change always clears proposals
        }
        setCouncilSize(newSize);
        setCouncilType(newType);
        populate();
    }
    
    public void populate() {
        switch (type) {
            case APPOINTED_COUNCIL:
                return;
            case ELECTED_COUNCIL:
                //Members are elected by faction members
                //Implement election record to take from
                break;
            case WEALTH_BASED_COUNCIL:
                List<String> sortedByWealth = Wealth.topWealth(f, true);
                while(getCurrentSize() < getMaxSize() && sortedByWealth.size() > 0) {
                    String richest = sortedByWealth.remove(0);
                    if(canBeMember(richest, false)) {
                        members.add(richest);
                    }
                }
                break;
            default:
                break;
        }
    }

    public boolean hasSession() {
        return SimpleFactions.getInstance().getSessionManager().hasSession(this);
    }

    public Session getSession() {
        return SimpleFactions.getInstance().getSessionManager().getSession(this);
    }

    public boolean canHostSession() {
        return proposalHandler.hasProposals() && !hasSession();
    }

    public boolean hasProposals() {
        return proposalHandler.hasProposals();
    }

    public Rules getType() {
        return type;
    }

    public void setCouncilType(Rules type) {
        this.type = type;
    }

    public void setCouncilSize(int size) {
        this.size = size;
    }

    public boolean canBeMember(String name, boolean ignoreSize) {
        if(members.contains(name)) return false;
        if(f.getLeader().equalsIgnoreCase(name)) return false;
        if(!ignoreSize && getCurrentSize() >= getMaxSize()) return false;
        if(f.getOrCreateMainGuild().isMember(name)) return true;
        for(Faction vassal : RelationManager.getSubjects(f)) {
            if(vassal.isLeader(name)) return true;
        }
        for(Guild guild : f.getGuildHandler().getGuilds()) {
            if(guild.isLeader(name)) return true;
        }
        return true;
    }

    public void addMember(String member) {
        if(members.size() < size) members.add(member);
    }

    public void replaceMember(int slot, String newMember) {
        if(slot >= 0 && slot < members.size()) {
            members.set(slot, newMember);
        }
    }

    public boolean isMember(String name) {
        return members.contains(name);
    }

    public int getCurrentProposals(String member) {
        return proposalHandler.getProposalsByProposer(member).size();
    }

    public boolean canBeProposed(Proposal proposal) {
        return proposalHandler.canBeProposed(proposal);
    }

    public ProposalHandler getProposalHandler() {
        return proposalHandler;
    }

    public List<String> getMembers() {
        return members;
    }

    public int getMaxSize() {
        return size;
    }

    public int getCurrentSize() {
        return members.size();
    }

    public boolean couldBeBigger() {
        return members.size() < size && type.equals(Rules.APPOINTED_COUNCIL) && f.getMembers().size()-1 > members.size();
    }

    public double fillPercentage() {
        return (double)members.size()/(double)size;
    }
}
