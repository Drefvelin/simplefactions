package me.Plugins.SimpleFactions.government.movement.cause;

import java.util.List;
import me.Plugins.SimpleFactions.government.movement.Movement;
import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.enums.Member;
import me.Plugins.SimpleFactions.enums.Stance;
import me.Plugins.SimpleFactions.government.movement.Action;
import me.Plugins.SimpleFactions.government.movement.Pool;
import me.Plugins.SimpleFactions.government.proposal.Proposal;

public class Cause {
    private Movement movement;
    private Action action;
    private Proposal proposal;

    private String leader;
    
    private Pool members = new Pool();

    public Cause(Movement movement, Proposal proposal, String leader) {
        this.movement = movement;
        this.proposal = proposal;
        action = proposal.getPoliticalAction().getAction();
        this.leader = leader;
    }

    public void tick() {
        movement.checkMembers(members, proposal, true);
        if(hasLeader() && !checkLeader()) {
            leader = null;
        }
    }

    public boolean checkLeader() {
        if(!members.getAllMembers().contains(leader)) return false;
        Member relation = movement.getFaction().getRelationToFaction(leader);
        switch(relation) {
            case LEADER:
            case FOREIGNER:
            case VASSAL_MEMBER:
                return false;
            case GUILD_LEADER:
            case GUILD_MEMBER:
                if(!proposal.getPoliticalAction().allowGuilds()) return false;
            case VASSAL_LEADER:
                if(!proposal.getPoliticalAction().allowFactions()) return false;
            case MEMBER:
                if(!proposal.getPoliticalAction().allowMembers()) return false;
                return true;
            default:
                break;
        }
        return false;
    }

    public Movement getMovement() {
        return movement;
    }

    public Action getAction() {
        return action;
    }

    public Proposal getProposal() {
        return proposal;
    }

    public String getLeader() {
        return leader;
    }

    public boolean hasLeader() {
        return leader != null;
    }

    public Pool getMembers() {
        return members;
    }

    public List<String> getMembersList() {
        return members.getMembers();
    }
}
