package me.Plugins.SimpleFactions.government.movement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.government.movement.cause.Cause;
import me.Plugins.SimpleFactions.government.proposal.Proposal;

public class Movement {
    private String leader;

    private double organization;

    private List<Cause> causes = new ArrayList<>();

    private Pool supporters = new Pool();
    
    private List<Faction> foreignBackers = new ArrayList<>();

    public Movement(String leader, Cause cause) {
        this.leader = leader;
        addCause(cause);
    }

    public String getLeader() {
        return leader;
    }

    public double getOrganization() {
        return organization;
    }

    public List<Cause> getCauses() {
        return causes;
    }

    public Pool getSupporters() {
        return supporters;
    }

    public List<Faction> getForeignBackers() {
        return foreignBackers;
    }

    public void addCause(Cause cause) {
        causes.add(cause);
    }
}
