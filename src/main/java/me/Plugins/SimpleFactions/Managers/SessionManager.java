package me.Plugins.SimpleFactions.Managers;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;

import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.government.Council;
import me.Plugins.SimpleFactions.government.proposal.Proposal;
import me.Plugins.SimpleFactions.government.session.Session;
import me.Plugins.SimpleFactions.Managers.FactionManager;

public class SessionManager implements Listener{
    private Map<Council, Session> sessions = new HashMap<>();

    public void start() {
        tickCycle();
    }

    public void tickCycle() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for(Session session : sessions.values()) {
                    session.tick();
                }
            }
        }.runTaskTimer(SimpleFactions.getInstance(), 20, 20); //Run every second
    }
    
    public void newSession(Player player, Faction f) {
        sessions.put(f.getGovernment().getCouncil(), new Session(player, f));
    }
    
    public void endSession(Council council) {
        sessions.remove(council);
    }
    
    public boolean hasSession(Council council) {
        return sessions.containsKey(council);
    }
    
    public Session getSession(Council council) {
        return sessions.get(council);
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if(!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;
        Block clickedBlock = event.getClickedBlock();
        
        if (clickedBlock == null) return;
        if (!clickedBlock.getType().equals(Material.LANTERN)) return;
        
        Faction faction = FactionManager.getByLeader(player.getName());
        if (faction == null) return;
        
        Council council = faction.getGovernment().getCouncil();
        Session session = getSession(council);
        player.sendMessage("aaaa");
        if (session != null && session.getLeader().equals(player)) {
            if(session.isStarted() && clickedBlock.equals(session.getLantern())) {
                event.setCancelled(true);
                Proposal currentProposal = session.getCurrentProposal();
                if(currentProposal == null) {
                    return;
                }
                player.openBook(currentProposal.getAsBook(player));
                return;
            } else if(session.isStarted() && !clickedBlock.equals(session.getLantern())) {
                return;
            }
            event.setCancelled(true);
            session.onLanternClick(clickedBlock);
        }
    }
}