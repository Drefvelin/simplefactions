package me.Plugins.SimpleFactions.vehicles;

import java.util.UUID;

import net.tfminecraft.DenarEconomy.DenarEconomy;
import net.tfminecraft.DenarEconomy.Enum.Accounts;

public final class DenarEconomyPlayerBank implements PlayerBank {
    public static final DenarEconomyPlayerBank INSTANCE = new DenarEconomyPlayerBank();

    private DenarEconomyPlayerBank() {}

    @Override
    public double getBankBalance(UUID playerUuid) {
        if (playerUuid == null) {
            return 0.0;
        }
        DenarEconomy.getPlayerManager().get(playerUuid);
        return DenarEconomy.getMoneyManager().getBalance(Accounts.BANK, playerUuid);
    }

    @Override
    public boolean withdrawFromBank(UUID playerUuid, double amount) {
        if (playerUuid == null || amount <= 0.0) {
            return false;
        }
        double balance = getBankBalance(playerUuid);
        if (balance < amount) {
            return false;
        }
        DenarEconomy.getMoneyManager().changeBal(playerUuid.toString(), -amount, Accounts.BANK);
        return true;
    }
}
