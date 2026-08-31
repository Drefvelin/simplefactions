package me.Plugins.SimpleFactions.vehicles;

import org.bukkit.Bukkit;

import net.tfminecraft.VehicleFramework.VehicleFramework;

public final class VehicleFrameworkDecayApi implements VehicleHealthDecayApi {
    public static final VehicleFrameworkDecayApi INSTANCE = new VehicleFrameworkDecayApi();

    private VehicleFrameworkDecayApi() {}

    @Override
    public boolean unloadedDamage(String vehicleUuid, double fractionOfMax, double minHealthFraction) {
        if (vehicleUuid == null || vehicleUuid.isBlank()) {
            return false;
        }
        if (Bukkit.getServer() == null
                || Bukkit.getPluginManager() == null
                || !Bukkit.getPluginManager().isPluginEnabled("VehicleFramework")) {
            return false;
        }
        return VehicleFramework.getVehicleManager()
                .unloadedDamage(vehicleUuid, fractionOfMax, minHealthFraction);
    }
}
