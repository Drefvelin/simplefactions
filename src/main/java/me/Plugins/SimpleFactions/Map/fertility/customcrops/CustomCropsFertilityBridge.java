package me.Plugins.SimpleFactions.Map.fertility.customcrops;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import net.momirealms.customcrops.api.BukkitCustomCropsPlugin;
import net.momirealms.customcrops.api.core.world.CustomCropsBlockState;
import net.momirealms.customcrops.api.event.CustomCropsReloadEvent;
import net.momirealms.customcrops.api.requirement.RequirementManager;

public final class CustomCropsFertilityBridge implements Listener {
    private static boolean listenerRegistered;

    public static void tryRegister() {
        Plugin customCrops = Bukkit.getPluginManager().getPlugin("CustomCrops");
        if (customCrops == null || !customCrops.isEnabled()) {
            return;
        }
        try {
            RequirementManager<CustomCropsBlockState> manager = BukkitCustomCropsPlugin.getInstance()
                    .getRequirementManager(CustomCropsBlockState.class);
            manager.unregisterRequirement(ProvinceFertilityRequirement.TYPE);
            manager.unregisterRequirement(ProvinceFertilityRequirement.ALIAS);
            boolean registered = manager.registerRequirement(
                    ProvinceFertilityRequirement.FACTORY,
                    ProvinceFertilityRequirement.TYPE,
                    ProvinceFertilityRequirement.ALIAS);
            if (registered) {
                Bukkit.getLogger().info(
                        "[SimpleFactions] Registered CustomCrops requirement: " + ProvinceFertilityRequirement.TYPE);
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            Bukkit.getLogger().warning(
                    "[SimpleFactions] Failed to register CustomCrops fertility requirement: "
                            + exception.getMessage());
        }
    }

    public static void registerReloadListener(Plugin plugin, CustomCropsFertilityBridge bridge) {
        if (listenerRegistered) {
            return;
        }
        plugin.getServer().getPluginManager().registerEvents(bridge, plugin);
        listenerRegistered = true;
    }

    @EventHandler
    public void onCustomCropsReload(CustomCropsReloadEvent event) {
        tryRegister();
    }
}
