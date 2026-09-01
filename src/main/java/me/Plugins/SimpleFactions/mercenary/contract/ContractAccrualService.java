package me.Plugins.SimpleFactions.mercenary.contract;

import me.Plugins.SimpleFactions.Managers.WarManager;
import me.Plugins.SimpleFactions.War.core.War;

/**
 * Accrues absolute denars onto the contract. Phase 5 moves the money; this phase
 * only writes the buckets, never nets them, and never reads a ledger.
 */
public final class ContractAccrualService {
    private ContractAccrualService() {
    }

    public static void onBattleStarted(String battleId, Integer warId) {
        War war = warId == null ? null : WarManager.getById(warId);
        if (war == null || battleId == null) return;
        charge(war, battleId, war.getAttackers());
        charge(war, battleId, war.getDefenders());
    }

    public static void onBattleEnded(String battleId, Integer warId) {
        War war = warId == null ? null : WarManager.getById(warId);
        if (war == null || battleId == null) return;
        refund(war, battleId, war.getAttackers());
        refund(war, battleId, war.getDefenders());
    }

    private static void charge(War war, String battleId, me.Plugins.SimpleFactions.War.core.Side side) {
        for (MercenaryEngagements.Engagement engagement : MercenaryEngagements.on(war, side)) {
            MercenaryContract contract = engagement.contract();
            if (contract == null || !contract.isActive()) continue;
            contract.accrueBattleCharge(battleId, contract.getBattlePrice());
        }
    }

    private static void refund(War war, String battleId, me.Plugins.SimpleFactions.War.core.Side side) {
        for (MercenaryEngagements.Engagement engagement : MercenaryEngagements.on(war, side)) {
            MercenaryContract contract = engagement.contract();
            if (contract == null) continue;
            AttendanceService.Result result = AttendanceService.result(contract, battleId);
            if (result.snapshotMissing()) continue;
            double amount = result.absent() * contract.getAbsenceRefundPerSlotPerBattle();
            contract.accrueAbsenceRefund(battleId, amount);
        }
    }
}
