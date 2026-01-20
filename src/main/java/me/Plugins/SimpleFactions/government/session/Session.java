package me.Plugins.SimpleFactions.government.session;

import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.government.Council;
import me.Plugins.SimpleFactions.government.proposal.Proposal;
import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.Managers.SessionManager;

public class Session {
    private Player leader;
    private Block lantern;
    private Faction f;
    private Council c;
    private boolean started = false;
    private int time = 0;

    private Map<Proposal, VoteResult> proposals = new LinkedHashMap<>();
    private SessionHologram hologram;
    
    public Session(Player leader, Faction f) {
        this.leader = leader;
        this.f = f;
        c = f.getGovernment().getCouncil();
        leader.sendTitle("§aSelect Location", "§7Click on a §eLantern §7to select Location", 20, 80, 20);
    }

    public void tick() {
        time++;
        if (time >= 60 && !started) {
            cancel();
        }
    }
    
    public void onLanternClick(Block lanternBlock) {
        if (started) return;
        
        this.lantern = lanternBlock;
        start();
    }

    public boolean isStarted() {
        return started;
    }

    private void nextProposal() {
        Proposal proposal = c.getProposalHandler().pop();
        if (proposal == null) {
            //End Session
            endSession();
            return;
        }
        proposals.put(proposal, VoteResult.IN_PROGRESS);
        updateHologram();
    }

    public Proposal getCurrentProposal() {
        for (Proposal p : proposals.keySet()) {
            if (proposals.get(p).equals(VoteResult.IN_PROGRESS)) {
                return p;
            }
        }
        return null;
    }
    
    public void start() {
        leader.sendTitle("§aSession Started!", "§7The §eLantern §7will show proposals and count votes", 20, 80, 20);
        leader.playSound(leader, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        hologram = new SessionHologram(lantern);
        hologram.create();
        nextProposal();
        started = true;
    }

    public void end() {
        endSession();
    }
    
    private void updateHologram() {
        if (hologram == null) return;
        int current = proposals.size();
        int total = current; // This would need to be adjusted if you know total proposal count
        hologram.updateProposalInfo(current, total);
        // Vote counts would be updated based on actual votes cast
        hologram.updateVotes(0, 0, 0);
    }
    
    private void endSession() {
        if (hologram != null) {
            hologram.destroy();
        }
        SessionManager sessionManager = SimpleFactions.getInstance().getSessionManager();
        sessionManager.endSession(f.getGovernment().getCouncil());
    }
    
    private void cancel() {
        if (leader != null) {
            leader.sendMessage("§cSession cancelled: No lantern selected within 60 seconds.");
        }
        endSession();
    }
    
    public Player getLeader() {
        return leader;
    }
    
    public Block getLantern() {
        return lantern;
    }
    
    public Faction getFaction() {
        return f;
    }
}
