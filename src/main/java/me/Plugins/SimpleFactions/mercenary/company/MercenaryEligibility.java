package me.Plugins.SimpleFactions.mercenary.company;

/**
 * Seam for the character-trait gate on mercenary work. The lock in
 * docs/planning/war-companies/00-index.md keeps this open for Phase 2, so both
 * checks answer true; the real rule will read the evil traits listed in
 * rpcharacters/src/main/resources/evil-traits.yml.
 */
public final class MercenaryEligibility {
    private MercenaryEligibility() {
    }

    /** Whether a player may found a mercenary company. */
    public static boolean canCreate(String player) {
        return true;
    }

    /** Whether a player may enlist in a mercenary company. */
    public static boolean canJoin(String player) {
        return true;
    }
}
