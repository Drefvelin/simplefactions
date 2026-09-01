package me.Plugins.SimpleFactions.mercenary.company;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.Guild.upgrade.Upgrade;
import me.Plugins.SimpleFactions.Managers.Inventory.CompanyCreator;
import me.Plugins.SimpleFactions.Utils.Formatter;

/** The company screens, checked through the lore builders as the ledger screen is. */
class CompanyGuiTest {
    private final CompanyCreator creator = new CompanyCreator();
    private CompanyFixture fixture;

    @BeforeEach
    void setUp() {
        CompanyFixture.installMercenaryPrototype();
        CompanyFixture.installCompanyUpgrades();
        Cache.mercenarySlotUpkeep = 8.0;
        Cache.mercenaryFormationCost = 100.0;
        Cache.mercenaryFormationSeconds = 86400;
        fixture = new CompanyFixture(1000);
    }

    @AfterEach
    void tearDown() {
        CompanyFixture.clearCompanyUpgrades();
        CompanyFixture.clearRegiments();
        CompanyFixture.clearGlobalGuilds();
    }

    private MercenaryCompany formedCompany() {
        MercenaryCompanyService.requestFormation(fixture.guild, fixture.leader(), "Hired Blades");
        MercenaryCompany company = fixture.company();
        for (int i = 0; i < Cache.mercenaryFormationSeconds; i++) {
            company.tick();
        }
        return company;
    }

    @Test
    void burnLineEqualsTheSumOfItsParts() {
        MercenaryCompany company = formedCompany();
        company.enlist("Bjorn");
        assertTrue(company.enqueueExpansion().ok());
        for (int i = 0; i < 86400; i++) {
            company.tick();
        }
        company.getUpgrade("company_health").setLevel(3);

        assertEquals(2, company.getSlots());
        assertEquals(16.0, company.getSlotUpkeep());
        assertEquals(30.0, company.getUpgradeUpkeep());
        assertEquals(0.0, company.getWageUpkeep());
        assertEquals(46.0, company.getDailyBurn());

        List<String> lore = creator.buildCompanyLore(fixture.guild, company);
        assertTrue(lore.stream().anyMatch(line ->
                line.contains("Daily burn") && line.contains(Formatter.formatMoney(46.0))));
        assertTrue(lore.stream().anyMatch(line ->
                line.contains("Slots") && line.contains(Formatter.formatMoney(16.0))));
        assertTrue(lore.stream().anyMatch(line ->
                line.contains("Upgrades") && line.contains(Formatter.formatMoney(30.0))));
        assertTrue(lore.stream().anyMatch(line ->
                line.contains("Wages") && line.contains(Formatter.formatMoney(0.0))));
    }

    @Test
    void companyLoreNamesTheLiveLeaderAndSlotFill() {
        MercenaryCompany company = formedCompany();
        company.enlist("Bjorn");
        fixture.setLeader("Sigrid");

        List<String> lore = creator.buildCompanyLore(fixture.guild, company);
        assertTrue(lore.stream().anyMatch(line -> line.contains("Leader") && line.contains("Sigrid")));
        assertTrue(lore.stream().anyMatch(line -> line.contains("Slots") && line.contains("1/1")));
        assertTrue(lore.stream().anyMatch(line -> line.contains("Home") && line.contains("None")));
    }

    @Test
    void everyUpgradeItemCarriesTheBuffScopeWarning() {
        MercenaryCompany company = formedCompany();
        List<Upgrade> upgrades = company.getUpgrades();
        assertEquals(3, upgrades.size());
        for (Upgrade upgrade : upgrades) {
            List<String> lore = creator.buildUpgradeLore(upgrade);
            assertTrue(lore.stream().anyMatch(line ->
                            line.contains("Only while fighting as a hired mercenary")),
                    "missing buff scope warning on " + upgrade.getId());
            assertTrue(lore.stream().anyMatch(line -> line.contains("Level: ")
                    && line.contains("10")));
        }
    }

    @Test
    void maxedUpgradeSaysSoAndKeepsTheWarning() {
        MercenaryCompany company = formedCompany();
        Upgrade upgrade = company.getUpgrade("company_mana");
        upgrade.setLevel(10);

        List<String> lore = creator.buildUpgradeLore(upgrade);
        assertTrue(lore.stream().anyMatch(line -> line.contains("Maximum level reached")));
        assertTrue(lore.stream().anyMatch(line ->
                line.contains("Only while fighting as a hired mercenary")));
    }

    @Test
    void emptySlotLoreExplainsWhyExpansionIsFrozen() {
        MercenaryCompany company = formedCompany();

        List<String> empty = creator.buildSlotLore(null);
        assertTrue(empty.stream().anyMatch(line -> line.contains("Empty")));
        assertTrue(empty.stream().anyMatch(line -> line.contains("blocks expansion")));

        List<String> taken = creator.buildSlotLore("Bjorn");
        assertTrue(taken.stream().anyMatch(line -> line.contains("Bjorn")));
        assertFalse(taken.stream().anyMatch(line -> line.contains("Empty")));

        assertEquals("Fill every slot before adding another.",
                company.getExpansionBlockedReason());
        List<String> lore = creator.buildCompanyLore(fixture.guild, company);
        assertTrue(lore.stream().anyMatch(line -> line.contains("Cannot expand")));
        assertTrue(lore.stream().anyMatch(line ->
                line.contains("Fill every slot before adding another.")));
    }

    @Test
    void foundingButtonQuotesTheChartersCostAndTime() {
        List<String> lore = creator.buildFoundingLore();
        assertTrue(lore.stream().anyMatch(line ->
                line.contains("Cost") && line.contains(Formatter.formatMoney(100.0))));
        assertTrue(lore.stream().anyMatch(line -> line.contains("/company found")));

        MercenaryCompanyService.requestFormation(fixture.guild, fixture.leader(), "Hired Blades");
        List<String> forming = creator.buildFormingLore(fixture.company());
        assertTrue(forming.stream().anyMatch(line -> line.contains("Founding")));
        assertTrue(forming.stream().anyMatch(line -> line.contains("Ready in")));
    }

    @Test
    void noPlayerFacingEmDashes() {
        MercenaryCompany company = formedCompany();
        List<String> lines = new java.util.ArrayList<>();
        lines.addAll(creator.buildFoundingLore());
        lines.addAll(creator.buildFormingLore(company));
        lines.addAll(creator.buildCompanyLore(fixture.guild, company));
        lines.addAll(creator.buildSlotLore(null));
        lines.addAll(creator.buildSlotLore("Bjorn"));
        for (Upgrade upgrade : company.getUpgrades()) {
            lines.addAll(creator.buildUpgradeLore(upgrade));
        }
        for (String line : lines) {
            assertFalse(line.contains("\u2014"), "em dash in company lore: " + line);
        }
    }
}
