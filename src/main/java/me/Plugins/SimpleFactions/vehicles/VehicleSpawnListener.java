package me.Plugins.SimpleFactions.vehicles;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import net.tfminecraft.VehicleFramework.Events.VehicleSpawnEvent;

public final class VehicleSpawnListener implements Listener {
    private final InstallationVehicleOwnerSync ownerSync;

    public VehicleSpawnListener(InstallationVehicleOwnerSync ownerSync) {
        this.ownerSync = ownerSync;
    }

    @EventHandler
    public void onVehicleSpawn(VehicleSpawnEvent event) {
        if (event.getVehicle() == null) {
            return;
        }
        ownerSync.syncIfBerthed(event.getVehicle());
    }
}
