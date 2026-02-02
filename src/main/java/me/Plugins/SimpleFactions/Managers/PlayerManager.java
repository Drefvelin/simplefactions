package me.Plugins.SimpleFactions.Managers;

import org.bukkit.Chunk;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Guild.income.Cashflow;
import me.Plugins.SimpleFactions.Guild.income.entry.FactionEntry;
import me.Plugins.SimpleFactions.Guild.income.entry.PlayerEntry;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Objects.FactionModifier;
import me.Plugins.SimpleFactions.Utils.FactionCleanup;
import me.Plugins.SimpleFactions.enums.FactionModifiers;
import me.Plugins.SimpleFactions.government.proposal.TaxTarget;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;
import me.Plugins.TLibs.Utils.ParseUtils;
import net.tfminecraft.DenarEconomy.DenarEconomy;
import net.tfminecraft.DenarEconomy.Data.PlayerData;
import net.tfminecraft.DenarEconomy.Enum.Accounts;
import net.tfminecraft.DenarEconomy.Item.Coin;
import net.tfminecraft.DenarEconomy.Managers.MoneyManager;
import net.tfminecraft.DenarEconomy.event.PlayerBankPulseEvent;
import net.tfminecraft.DenarEconomy.event.PlayerDepositMaterialsEvent;
import net.tfminecraft.DenarEconomy.event.PlayerEarnMoneyEvent;

public class PlayerManager implements Listener{
    @EventHandler
    public void joinEvent(PlayerJoinEvent e) {
        FactionCleanup.ping(e.getPlayer().getName());
    }

    @EventHandler
    public void earnMoney(PlayerEarnMoneyEvent e) {
        double paidTax = 0;
        double amount = e.getAmount();
        String p = e.getPlayer();
        if(FactionManager.getByMember(p) != null) {
			Faction f = FactionManager.getByMember(p);
			if(f.getTaxRate(TaxTarget.CITIZENS, null, true) > 0) {
				if(f.getBank() != null) {
					paidTax = f.getTaxRate(TaxTarget.CITIZENS, null, true)/100.0*amount;
					f.giveTax(p, paidTax);
				}
			}
		}
        e.setAmount(paidTax);
    }

    @EventHandler
    public void depositMaterials(PlayerDepositMaterialsEvent e) {
        ItemStack i = e.getItem();
        Player p = e.getPlayer();
        Coin c = DenarEconomy.getMoneyManager().getCoin(i);
		if(c == null) return;
		if(c.canWithdraw()) return;
		if(FactionManager.getByMember(p.getName()) == null) return;
		Faction f = FactionManager.getByMember(p.getName());
		if(f.getBank() == null) return;
		if(!f.getBank().getChunk().equals(p.getLocation().getChunk())) return;
		DenarEconomy.getMoneyManager().addMoneyToAccount(p.getUniqueId().toString(), c.getValue()*i.getAmount(), false, true, Accounts.BANK);
		p.playSound(p, Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
		i.setAmount(0);
    }

    @EventHandler
    public void bankPulse(PlayerBankPulseEvent e) {
        if(!isInBankChunk(e.getPlayer())) {
            e.setCancelled(true);
        }
    } 

    private boolean isInBankChunk(Player p) {
        Faction f = FactionManager.getByMember(p.getName());
        Guild g = FactionManager.getGuildByMember(p.getName());

        if (f == null && g == null) {
            p.sendMessage("§a[DenarEconomy] §cYou must be in a faction");
            return false;
        }

        Chunk playerChunk = p.getLocation().getChunk();

        boolean inFactionBank = false;
        boolean inGuildBank = false;

        if (f != null && f.getBank() != null && f.getBank().getChunk() != null) {
            inFactionBank = f.getBank().getChunk().equals(playerChunk);
        }

        if (g != null && g.getBank() != null && g.getBank().getChunk() != null) {
            inGuildBank = g.getBank().getChunk().equals(playerChunk);
        }

        if (!inFactionBank && !inGuildBank) {
            p.sendMessage("§a[DenarEconomy] §cYou must be inside the bank chunk to deposit/withdraw");
            return false;
        }

        return true;
    }
}
