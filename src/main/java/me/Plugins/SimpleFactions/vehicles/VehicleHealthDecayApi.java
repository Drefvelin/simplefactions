package me.Plugins.SimpleFactions.vehicles;

public interface VehicleHealthDecayApi {
    boolean unloadedDamage(String vehicleUuid, double fractionOfMax, double minHealthFraction);
}
