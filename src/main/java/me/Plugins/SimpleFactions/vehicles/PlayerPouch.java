package me.Plugins.SimpleFactions.vehicles;

import java.util.UUID;

public interface PlayerPouch {
    double getPouchBalance(UUID playerUuid);

    boolean withdrawFromPouch(UUID playerUuid, double amount);
}
