package me.Plugins.SimpleFactions.Managers;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import me.Plugins.SimpleFactions.Utils.FactionCleanup;

public class PlayerManager implements Listener{
    @EventHandler
    public void joinEvent(PlayerJoinEvent e) {
        FactionCleanup.ping(e.getPlayer().getName());
    }
}
