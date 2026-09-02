package me.Plugins.SimpleFactions.Map.fertility;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockGrowEvent;

public final class FertilityCropGrowthListener implements Listener {
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockGrow(BlockGrowEvent event) {
        if (FertilityVanillaGrowth.shouldCancelGrowth(
                event.getBlock().getType(),
                FertilityProvinceResolver.fertilityAt(event.getBlock().getLocation()))) {
            event.setCancelled(true);
        }
    }
}
