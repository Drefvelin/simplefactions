package me.Plugins.SimpleFactions.vehicles;

import java.util.UUID;

public interface PlayerBank {
    double getBankBalance(UUID playerUuid);

    boolean withdrawFromBank(UUID playerUuid, double amount);
}
