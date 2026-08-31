package me.Plugins.SimpleFactions.vehicles;

public final class VehicleMaintenancePaySession {
    private final long expiresAtMillis;

    public VehicleMaintenancePaySession(long expiresAtMillis) {
        this.expiresAtMillis = expiresAtMillis;
    }

    public long getExpiresAtMillis() {
        return expiresAtMillis;
    }

    public boolean isExpired(long nowMillis) {
        return nowMillis >= expiresAtMillis;
    }
}
