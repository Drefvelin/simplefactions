package me.Plugins.SimpleFactions.vehicles;

public final class VehicleTransferSession {
    private final String installationId;
    private final long expiresAtMillis;

    public VehicleTransferSession(String installationId, long expiresAtMillis) {
        this.installationId = installationId;
        this.expiresAtMillis = expiresAtMillis;
    }

    public String getInstallationId() {
        return installationId;
    }

    public long getExpiresAtMillis() {
        return expiresAtMillis;
    }

    public boolean isExpired(long nowMillis) {
        return nowMillis >= expiresAtMillis;
    }
}
