package me.Plugins.SimpleFactions.government.movement.cause;

import java.util.List;
import me.Plugins.SimpleFactions.government.movement.Movement;
import me.Plugins.SimpleFactions.government.movement.Action;
import me.Plugins.SimpleFactions.government.movement.Pool;
import me.Plugins.SimpleFactions.government.proposal.Proposal;

public class Cause {
    private Movement movement;
    private Action action;
    private Proposal proposal;

    private String leader;
    
    private Pool members;

    public Cause(Movement movement, Proposal proposal, String leader) {
        this.movement = movement;
        this.proposal = proposal;
        action = proposal.getPoliticalAction();
        this.leader = leader;
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

    public Pool getMembers() {
        return members;
    }

    public List<String> getMembersList() {
        return members.getMembers();
    }
}
