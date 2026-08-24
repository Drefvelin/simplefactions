package me.Plugins.SimpleFactions.Managers.Inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import me.Plugins.SimpleFactions.player.income.PlayerCashflow;
import me.Plugins.SimpleFactions.player.income.PlayerLedger;

class PlayerLedgerCreatorTest {
    @Test
    void loreReflectsLedgerTotals() {
        PlayerLedger ledger = new PlayerLedger();
        ledger.add(PlayerCashflow.EARNINGS, 50.0);
        ledger.add(PlayerCashflow.CITIZEN_TAX, -10.0);
        ledger.add(PlayerCashflow.VEHICLE_UPKEEP, -20.0);

        assertEquals(20.0, ledger.getNetDaily());
        assertEquals(1, ledger.getIncomeFlows().size());
        assertEquals(2, ledger.getExpenseFlows().size());

        List<String> lore = new PlayerLedgerCreator().buildLore(ledger);
        assertTrue(lore.size() >= 10);
    }
}
