package me.Plugins.SimpleFactions.government.movement.cause;

import java.util.LinkedHashMap;
import java.util.Map;

import me.Plugins.SimpleFactions.government.movement.Pool;
import me.Plugins.SimpleFactions.government.movement.Pools;
import me.Plugins.SimpleFactions.government.proposal.Proposal;

public class Cause {
    private Proposal proposal;
    
    private Map<Pools, Pool> pools = new LinkedHashMap<>();
}
