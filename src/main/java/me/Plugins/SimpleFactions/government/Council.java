package me.Plugins.SimpleFactions.government;

import java.util.ArrayList;
import java.util.List;

import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.enums.Rules;
import me.Plugins.SimpleFactions.government.handler.ProposalHandler;

public class Council {
    private Faction f;
    private List<String> members = new ArrayList<>();
    private Rules type;

    private ProposalHandler proposalHandler = new ProposalHandler();

    public boolean canPropose(String player) {
        if(!members.contains(player)) return false;
        return proposalHandler.canPropose(player);
    }
}
