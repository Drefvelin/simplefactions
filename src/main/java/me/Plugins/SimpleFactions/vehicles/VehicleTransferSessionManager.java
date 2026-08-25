package me.Plugins.SimpleFactions.vehicles;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class VehicleTransferSessionManager {
    private final Map<UUID, VehicleTransferSession> byLeaderUuid = new HashMap<>();

    public void put(UUID leaderUuid, VehicleTransferSession session) {
        if (leaderUuid == null || session == null) {
            return;
        }
        byLeaderUuid.put(leaderUuid, session);
    }

    public VehicleTransferSession get(UUID leaderUuid) {
        if (leaderUuid == null) {
            return null;
        }
        VehicleTransferSession session = byLeaderUuid.get(leaderUuid);
        if (session == null) {
            return null;
        }
        if (session.isExpired(System.currentTimeMillis())) {
            byLeaderUuid.remove(leaderUuid);
            return null;
        }
        return session;
    }

    public void clear(UUID leaderUuid) {
        if (leaderUuid == null) {
            return;
        }
        byLeaderUuid.remove(leaderUuid);
    }
}
