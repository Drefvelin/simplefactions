package me.Plugins.SimpleFactions.Managers.Inventory;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Utils.Formatter;
import me.Plugins.SimpleFactions.government.movement.Movement;
import me.Plugins.SimpleFactions.government.movement.cause.Cause;
import me.Plugins.SimpleFactions.keys.Keys;
import me.Plugins.TLibs.TLibs;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public class MovementCreator {
    
    public ItemStack createMovementLeaderItem(Movement movement) {
        ItemStack item = TLibs.getItemAPI().getCreator().getItemFromPath("v.player_head");
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(StringFormatter.formatHex("#c5e0e3Movement Leader"));
        List<String> lore = new ArrayList<>();
        lore.add(StringFormatter.formatHex("#9c9775Leader: #c2bea7" + movement.getLeader()));
        lore.add(" ");
        lore.add(StringFormatter.formatHex("#7a7a7aThe leader coordinates the movement"));
        lore.add(StringFormatter.formatHex("#7a7a7aand represents its interests."));
        m.setLore(lore);
        item.setItemMeta(m);
        return item;
    }
    
    public ItemStack createOrganizationItem(Movement movement) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(StringFormatter.formatHex("#85c265Organization"));
        List<String> lore = new ArrayList<>();
        double organization = Formatter.formatDouble(movement.getOrganization());
        lore.add(StringFormatter.formatHex("#9c9775Level: #c2bea7" + organization + "/100"));
        lore.add(" ");
        lore.add(StringFormatter.formatHex("#7a7a7aOrganization determines how effective"));
        lore.add(StringFormatter.formatHex("#7a7a7athe movement is at achieving its goals."));
        m.setLore(lore);
        item.setItemMeta(m);
        return item;
    }
    
    public ItemStack createJoinAsSupporterButton(Player player, Movement movement) {
        ItemStack item = new ItemStack(Material.GREEN_BANNER);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(StringFormatter.formatHex("#45afc4Join as Supporter"));
        List<String> lore = new ArrayList<>();
        lore.add(StringFormatter.formatHex("#7a7a7aSupport the movement without"));
        lore.add(StringFormatter.formatHex("#7a7a7acommitting to specific causes."));
        lore.add(" ");
        int supporterCount = movement.getSupporters().getAllMembers().size();
        lore.add(StringFormatter.formatHex("#9c9775Current Supporters: #c2bea7" + supporterCount));
        m.setLore(lore);
        item.setItemMeta(m);
        return item;
    }
    
    public ItemStack createJoinAsForeignBackerButton(Player player, Movement movement) {
        ItemStack item = new ItemStack(Material.YELLOW_BANNER);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(StringFormatter.formatHex("#d1b83b Join as Foreign Backer"));
        List<String> lore = new ArrayList<>();
        lore.add(StringFormatter.formatHex("#7a7a7aProvide foreign support to"));
        lore.add(StringFormatter.formatHex("#7a7a7athe movement from outside."));
        lore.add(" ");
        int backerCount = movement.getForeignBackers().size();
        lore.add(StringFormatter.formatHex("#9c9775Current Backers: #c2bea7" + backerCount));
        m.setLore(lore);
        item.setItemMeta(m);
        return item;
    }
    
    public ItemStack createCausesButton(Movement movement) {
        ItemStack item = new ItemStack(Material.RED_BANNER);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(StringFormatter.formatHex("#c74d32View Causes"));
        List<String> lore = new ArrayList<>();
        lore.add(StringFormatter.formatHex("#7a7a7aView all causes supported by"));
        lore.add(StringFormatter.formatHex("#7a7a7athis movement."));
        lore.add(" ");
        lore.add(StringFormatter.formatHex("#9c9775Total Causes: #c2bea7" + movement.getCauses().size()));
        m.setLore(lore);
        item.setItemMeta(m);
        return item;
    }
    
    public ItemStack createCauseItem(Cause cause, int index) {
        ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(StringFormatter.formatHex("#c5e0e3"+(cause.hasLeader() ? cause.getLeader()+"'s Cause" : "Disorganized Cause")));
        List<String> lore = new ArrayList<>();
        lore.add(StringFormatter.formatHex("#9c9775Action: #c2bea7" + cause.getAction().getDisplay()));
        if (cause.hasLeader()) {
            lore.add(StringFormatter.formatHex("#9c9775Leader: #c2bea7" + cause.getLeader()));
        } else {
            lore.add(StringFormatter.formatHex("#c74d32No Leader"));
        }
        lore.add(" ");
        int memberCount = cause.getMembersList().size();
        lore.add(StringFormatter.formatHex("#9c9775Members: #c2bea7" + memberCount));
        for(String member : cause.getMembers().getFormattedList()) {
            lore.add(StringFormatter.formatHex("#7a7a7a- " + member));
        }
        lore.add(" ");
        lore.add(StringFormatter.formatHex("#7a7a7aClick to view details"));
        m.setLore(lore);
        m.getPersistentDataContainer().set(Keys.INT, PersistentDataType.INTEGER, index);
        item.setItemMeta(m);
        return item;
    }
    
    public ItemStack createCauseLeaderItem(Cause cause) {
        ItemStack item = TLibs.getItemAPI().getCreator().getItemFromPath("v.player_head");
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(StringFormatter.formatHex("#c5e0e3Cause Leader"));
        List<String> lore = new ArrayList<>();
        if (cause.hasLeader()) {
            lore.add(StringFormatter.formatHex("#9c9775Leader: #c2bea7" + cause.getLeader()));
        } else {
            lore.add(StringFormatter.formatHex("#c74d32No Leader Assigned"));
        }
        lore.add(" ");
        lore.add(StringFormatter.formatHex("#7a7a7aThe leader of this specific cause"));
        lore.add(StringFormatter.formatHex("#7a7a7awithin the movement."));
        m.setLore(lore);
        item.setItemMeta(m);
        return item;
    }
    
    public ItemStack createCauseProposalItem(Cause cause) {
        ItemStack item = cause.getProposal().getPoliticalAction().getIcon();
        ItemMeta m = item.getItemMeta();
        if (m == null) {
            m = item.getItemMeta();
        }
        m.setDisplayName(StringFormatter.formatHex("#85c265Proposal"));
        List<String> lore = new ArrayList<>();
        lore.add(StringFormatter.formatHex("#9c9775Action: #c2bea7" + cause.getAction().getDisplay()));
        lore.add(" ");
        lore.add(StringFormatter.formatHex("#7a7a7aThe political action this"));
        lore.add(StringFormatter.formatHex("#7a7a7acause is working towards."));
        m.setLore(lore);
        item.setItemMeta(m);
        return item;
    }
    
    public ItemStack createJoinCauseButton(Player player, Cause cause) {
        ItemStack item = new ItemStack(Material.GREEN_WOOL);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(StringFormatter.formatHex("#45afc4Join Cause"));
        List<String> lore = new ArrayList<>();
        lore.add(StringFormatter.formatHex("#7a7a7aCommit to supporting this"));
        lore.add(StringFormatter.formatHex("#7a7a7aspecific cause."));
        lore.add(" ");
        int memberCount = cause.getMembersList().size();
        lore.add(StringFormatter.formatHex("#9c9775Current Members: #c2bea7" + memberCount));
        m.setLore(lore);
        item.setItemMeta(m);
        return item;
    }
    
    public ItemStack createMovementListItem(Movement movement) {
        ItemStack item = new ItemStack(Material.BLAZE_POWDER);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(StringFormatter.formatHex("#d1743b" + movement.getLeader() + "'s Movement "));
        List<String> lore = new ArrayList<>();
        lore.add(StringFormatter.formatHex("#9c9775Leader: #c2bea7" + movement.getLeader()));
        lore.add(StringFormatter.formatHex("#9c9775Organization: #c2bea7" + Formatter.formatDouble(movement.getOrganization()) + "/100"));
        lore.add(" ");
        lore.add(StringFormatter.formatHex("#9c9775Causes: #c2bea7" + movement.getCauses().size()));
        lore.add(StringFormatter.formatHex("#9c9775Supporters: #c2bea7" + movement.getSupporters().getAllMembers().size()));
        lore.add(StringFormatter.formatHex("#9c9775Foreign Backers: #c2bea7" + movement.getForeignBackers().size()));
        lore.add(" ");
        lore.add(StringFormatter.formatHex("#7a7a7aClick to view details"));
        m.getPersistentDataContainer().set(Keys.STRING_KEY, PersistentDataType.STRING, movement.getLeader());
        item.setItemMeta(m);
        return item;
    }
}
