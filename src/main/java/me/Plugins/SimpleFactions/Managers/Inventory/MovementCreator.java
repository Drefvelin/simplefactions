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
import me.Plugins.SimpleFactions.government.movement.Phase;
import me.Plugins.SimpleFactions.government.movement.cause.Cause;
import me.Plugins.SimpleFactions.keys.Keys;
import me.Plugins.TLibs.TLibs;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;
import net.Indyuce.mmoitems.stat.data.MaterialData;

public class MovementCreator {
    
    public ItemStack createMovementLeaderItem(Player p, Movement movement) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        if(!movement.hasLeader() && movement.canBeLeader(p.getName())) {
            item = new ItemStack(Material.GREEN_CONCRETE);
        } else if(!movement.hasLeader()) {
            item = new ItemStack(Material.RED_CONCRETE);
        }
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(StringFormatter.formatHex("#c5e0e3Movement Leader"));
        List<String> lore = new ArrayList<>();
        lore.add(StringFormatter.formatHex("#9c9775Leader: #c2bea7" + (movement.hasLeader() ? movement.getLeader() : "None")));
        lore.add(" ");
        lore.add(StringFormatter.formatHex("#7a7a7aThe leader coordinates the movement"));
        lore.add(StringFormatter.formatHex("#7a7a7aand represents its interests."));
        if(!movement.hasLeader()) {
            lore.add(" ");
            if(movement.canBeLeader(p.getName())) {
                lore.add(StringFormatter.formatHex("#45afc4Click to become the leader"));
            } else {
                lore.add(StringFormatter.formatHex("#c74d32You need to lead a cause to be the movement leader"));
            }
        }
        m.getPersistentDataContainer().set(Keys.STRING_KEY, PersistentDataType.STRING, movement.getId());
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
        lore.add(StringFormatter.formatHex("#9c9775Level: #c2bea7" + organization + "/"+movement.getMaxOrganization()));
        lore.add(StringFormatter.formatHex("#9c9775Gain: #c2bea7" + (movement.getOrganizationGain() > 0 ? "+" : "#d65c5c") + movement.getOrganizationGain() + " §7per day"));
        lore.add(" ");
        lore.add(StringFormatter.formatHex("#7a7a7aOrganization determines how effective"));
        lore.add(StringFormatter.formatHex("#7a7a7athe movement is at achieving its goals."));
        m.setLore(lore);
        m.getPersistentDataContainer().set(Keys.STRING_KEY, PersistentDataType.STRING, movement.getId());
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
        m.getPersistentDataContainer().set(Keys.STRING_KEY, PersistentDataType.STRING, movement.getId());
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
        m.getPersistentDataContainer().set(Keys.STRING_KEY, PersistentDataType.STRING, movement.getId());
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
        m.getPersistentDataContainer().set(Keys.STRING_KEY, PersistentDataType.STRING, movement.getId());
        item.setItemMeta(m);
        return item;
    }
    
    public ItemStack createCauseItem(Cause cause) {
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
        int memberCount = cause.getFullMemberList().size();
        lore.add(StringFormatter.formatHex("#9c9775Members: #c2bea7" + memberCount));
        for(String member : cause.getPool().getFormattedList()) {
            lore.add(StringFormatter.formatHex("#7a7a7a- " + member));
        }
        lore.add(" ");
        lore.add(StringFormatter.formatHex("#50e846Click to view details"));
        m.setLore(lore);
        m.getPersistentDataContainer().set(Keys.INT, PersistentDataType.INTEGER, cause.getIndex());
        m.getPersistentDataContainer().set(Keys.STRING_KEY, PersistentDataType.STRING, cause.getMovement().getId());
        item.setItemMeta(m);
        return item;
    }
    
    public ItemStack createCauseLeaderItem(Player p, Cause cause) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        if(!cause.hasLeader() && cause.canBeLeader(p.getName())) {
            item = new ItemStack(Material.GREEN_CONCRETE);
        } else if(!cause.hasLeader()) {
            item = new ItemStack(Material.RED_CONCRETE);
        }
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(StringFormatter.formatHex("#c5e0e3Cause Leader"));
        List<String> lore = new ArrayList<>();
        if (cause.hasLeader()) {
            lore.add(StringFormatter.formatHex("#9c9775Leader: #c2bea7" + cause.getLeader()));
        } else {
            lore.add(StringFormatter.formatHex("#c74d32No Leader Assigned"));
        }
        if(!cause.hasLeader()) {
            lore.add(" ");
            if(cause.canBeLeader(p.getName())) {
                lore.add(StringFormatter.formatHex("#45afc4Click to become the leader"));
            } else {
                lore.add(StringFormatter.formatHex("#c74d32You are not able to become leader"));
            }
        }
        m.setLore(lore);
        m.getPersistentDataContainer().set(Keys.INT, PersistentDataType.INTEGER, cause.getIndex());
        m.getPersistentDataContainer().set(Keys.STRING_KEY, PersistentDataType.STRING, cause.getMovement().getId());
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
        m.getPersistentDataContainer().set(Keys.INT, PersistentDataType.INTEGER, cause.getIndex());
        m.getPersistentDataContainer().set(Keys.STRING_KEY, PersistentDataType.STRING, cause.getMovement().getId());
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
        int memberCount = cause.getFullMemberList().size();
        lore.add(StringFormatter.formatHex("#9c9775Current Members: #c2bea7" + memberCount));
        m.setLore(lore);
        m.getPersistentDataContainer().set(Keys.INT, PersistentDataType.INTEGER, cause.getIndex());
        m.getPersistentDataContainer().set(Keys.STRING_KEY, PersistentDataType.STRING, cause.getMovement().getId());
        item.setItemMeta(m);
        return item;
    }

    public ItemStack createEndMovmentItem(Movement movement) {
        ItemStack item = TLibs.getItemAPI().getCreator().getItemsAdderItem("mcicons:icon_cancel");
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(StringFormatter.formatHex("#c74d32End Movement"));
        List<String> lore = new ArrayList<>();
        lore.add(StringFormatter.formatHex("#7a7a7aEnd this movement and disband"));
        lore.add("");
        lore.add(StringFormatter.formatHex("#d87638Click to Disband"));
        m.setLore(lore);
        m.getPersistentDataContainer().set(Keys.STRING_KEY, PersistentDataType.STRING, movement.getId());
        item.setItemMeta(m);
        return item;
    }
    
    public ItemStack createMovementListItem(Movement movement) {
        ItemStack item = new ItemStack(Material.BLAZE_POWDER);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(StringFormatter.formatHex("#d1743b" + movement.getLeader() + "'s Movement "));
        List<String> lore = new ArrayList<>();
        lore.add(StringFormatter.formatHex("#9c9775Leader: #c2bea7" + movement.getLeader()));
        lore.add(StringFormatter.formatHex("#9c9775Organization: #c2bea7" + Formatter.formatDouble(movement.getOrganization()) + 
                                            "/"+movement.getMaxOrganization()+" §7(" + (movement.getOrganizationGain() > 0 ? "+" : "#d65c5c") + movement.getOrganizationGain()) + " §7per day)");
        lore.add(StringFormatter.formatHex("#c45749Power: #d87638"+movement.getPower()+"% #7a7a7aof " + movement.getFaction().getName()));
        lore.add(" ");
        lore.add(StringFormatter.formatHex("#9c9775Causes: #c2bea7" + movement.getCauses().size()));
        lore.add(StringFormatter.formatHex("#9c9775Members: #c2bea7" + movement.getAllMembers().size()));
        lore.add(StringFormatter.formatHex("#9c9775Supporters: #c2bea7" + movement.getSupporters().getAllMembers().size()));
        lore.add(StringFormatter.formatHex("#9c9775Foreign Backers: #c2bea7" + movement.getForeignBackers().size()));
        lore.add(" ");
        lore.add(StringFormatter.formatHex("#50e846Click to view details"));
        m.getPersistentDataContainer().set(Keys.STRING_KEY, PersistentDataType.STRING, movement.getId());
        m.setLore(lore);
        item.setItemMeta(m);
        return item;
    }

    public ItemStack createPhaseItem(Phase phase, Movement movement) {
        ItemStack item = new ItemStack(Material.RED_CONCRETE);
        if(phase == movement.getPhase()) {
            item = new ItemStack(Material.GREEN_CONCRETE);
        } else if(phase.getIndex() > 0 && phase.getIndex() == movement.getPhase().getIndex()-1) {
            item = new ItemStack(Material.YELLOW_CONCRETE);
        } else if(phase.getIndex() < 4 && phase.getIndex() == movement.getPhase().getIndex()+1 && movement.getOrganization() >= movement.getPhase().getMaxOrganization()) {
            item = new ItemStack(Material.YELLOW_CONCRETE);
        }
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(StringFormatter.formatHex(phase.getDisplayName()));
        List<String> lore = new ArrayList<>();
        lore.add(StringFormatter.formatHex("#9c9775Current Phase: #c2bea7" + phase.getDisplayName()));
        lore.add(StringFormatter.formatHex("#9c9775Max Organization: #c2bea7" + phase.getMaxOrganization()));
        lore.add(" ");
        lore.add(StringFormatter.formatHex("#7a7a7aThe current phase of the movement"));
        lore.add(StringFormatter.formatHex("#7a7a7awhich determines its max organization level"));
        lore.add("");
        if(item.getType().equals(Material.RED_CONCRETE)) {
            lore.add(StringFormatter.formatHex("#c74d32Unavailable"));
            if(phase.getIndex() > 0 && movement.getPhase().getIndex() == phase.getIndex()-1) {
                lore.add(StringFormatter.formatHex("§7(Organization " + movement.getOrganization() + "/" + movement.getPhase().getMaxOrganization() + "§7)"));
            }
        } else if(item.getType().equals(Material.YELLOW_CONCRETE)) {
            lore.add(StringFormatter.formatHex("#d1b83bClick to change"));
        } else if(item.getType().equals(Material.GREEN_CONCRETE)) {
            lore.add(StringFormatter.formatHex("#65c97cCurrent Phase"));
        }
        m.setLore(lore);
        m.getPersistentDataContainer().set(Keys.STRING_KEY, PersistentDataType.STRING, movement.getId());
        m.getPersistentDataContainer().set(Keys.SECONDARY_STRING_KEY, PersistentDataType.STRING, phase.name());
        item.setItemMeta(m);
        return item;
    }

    public ItemStack createSendDemandsItem(Movement movement) {
        ItemStack item = TLibs.getItemAPI().getCreator().getItemsAdderItem("iasurvival:letter");
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(StringFormatter.formatHex("#86cfa2Send Demands"));
        List<String> lore = new ArrayList<>();
        lore.add(StringFormatter.formatHex("#7a7a7aSend the movement's demands to the faction"));
        lore.add(StringFormatter.formatHex("#7a7a7aThe faction leader will have to accept or reject the demands."));
        lore.add(StringFormatter.formatHex("#7a7a7aIf the demands are rejected a #d87638Civil War #7a7a7abegins"));
        lore.add(" ");
        if(movement.getOrganization() < 100) {
            lore.add(StringFormatter.formatHex("#c2bea7Organization must be at #73b870100 #c2bea7to send demands"));
            lore.add(StringFormatter.formatHex("#7a7a7a(Currently " + movement.getOrganization() + "/100)"));
            lore.add(StringFormatter.formatHex("#c74d32Unavailable"));
        } else {
            lore.add(StringFormatter.formatHex("#50e846Click to Send Demands"));
        }
        m.setLore(lore);
        m.getPersistentDataContainer().set(Keys.STRING_KEY, PersistentDataType.STRING, movement.getId());
        item.setItemMeta(m);
        return item;
    }

    public ItemStack createEmptyCauseSlot(int index) {
        ItemStack item = new ItemStack(Material.GRAY_CONCRETE);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(StringFormatter.formatHex("#7a7a7aEmpty Cause Slot"));
        List<String> lore = new ArrayList<>();
        lore.add(StringFormatter.formatHex("#7a7a7aThis slot is locked."));
        lore.add(StringFormatter.formatHex("#7a7a7aCauses must be created in order."));
        m.setLore(lore);
        m.getPersistentDataContainer().set(Keys.INT, PersistentDataType.INTEGER, index);
        item.setItemMeta(m);
        return item;
    }

    public ItemStack createAvailableCauseSlot(Movement movement, int index) {
        ItemStack item = new ItemStack(Material.YELLOW_CONCRETE);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(StringFormatter.formatHex("#d1b83bCreate New Cause"));
        List<String> lore = new ArrayList<>();
        lore.add(StringFormatter.formatHex("#7a7a7aAs a supporter, you can create"));
        lore.add(StringFormatter.formatHex("#7a7a7aa new cause within this movement."));
        lore.add(" ");
        lore.add(StringFormatter.formatHex("#7a7a7aYou will be removed from general"));
        lore.add(StringFormatter.formatHex("#7a7a7asupporters and become the leader"));
        lore.add(StringFormatter.formatHex("#7a7a7aof this new cause."));
        lore.add(" ");
        lore.add(StringFormatter.formatHex("#50e846Click to Create Cause"));
        m.setLore(lore);
        m.getPersistentDataContainer().set(Keys.INT, PersistentDataType.INTEGER, index);
        m.getPersistentDataContainer().set(Keys.STRING_KEY, PersistentDataType.STRING, "CREATE_CAUSE");
        item.setItemMeta(m);
        return item;
    }

    public ItemStack createDemandItem(Cause cause, Movement movement) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(StringFormatter.formatHex("#c5e0e3Demand: " + (cause.hasLeader() ? cause.getLeader() : "Leaderless")));
        List<String> lore = new ArrayList<>();
        lore.add(StringFormatter.formatHex("#9c9775Action: #c2bea7" + cause.getAction().getDisplay()));
        lore.add(" ");
        lore.add(StringFormatter.formatHex("#7a7a7aThis cause demands the following"));
        lore.add(StringFormatter.formatHex("#7a7a7apolitical change:"));
        for (String desc : cause.getProposal().getPoliticalAction().getDescription()) {
            lore.add(StringFormatter.formatHex("#7a7a7a" + desc));
        }
        lore.add(" ");
        int memberCount = cause.getFullMemberList().size();
        lore.add(StringFormatter.formatHex("#9c9775Supporters: #c2bea7" + memberCount));
        m.setLore(lore);
        m.getPersistentDataContainer().set(Keys.INT, PersistentDataType.INTEGER, cause.getIndex());
        m.getPersistentDataContainer().set(Keys.STRING_KEY, PersistentDataType.STRING, movement.getId());
        item.setItemMeta(m);
        return item;
    }

    public ItemStack createMovementPowerItem(Movement movement) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(StringFormatter.formatHex("#d1b83bMovement Power"));
        List<String> lore = new ArrayList<>();
        lore.add(StringFormatter.formatHex("#9c9775Power: #c2bea7" + movement.getPower() + "%"));
        lore.add(" ");
        lore.add(StringFormatter.formatHex("#7a7a7aThe movement's power represents"));
        lore.add(StringFormatter.formatHex("#7a7a7ahow much of the faction supports"));
        lore.add(StringFormatter.formatHex("#7a7a7athis movement's demands."));
        m.setLore(lore);
        m.getPersistentDataContainer().set(Keys.STRING_KEY, PersistentDataType.STRING, movement.getId());
        item.setItemMeta(m);
        return item;
    }

    public ItemStack createDecliningWarningItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(StringFormatter.formatHex("#d65c5cWarning"));
        List<String> lore = new ArrayList<>();
        lore.add(StringFormatter.formatHex("#c74d32Declining these demands will"));
        lore.add(StringFormatter.formatHex("#c74d32trigger a Civil War!"));
        lore.add(" ");
        lore.add(StringFormatter.formatHex("#7a7a7aThe movement will rise up against"));
        lore.add(StringFormatter.formatHex("#7a7a7athe faction leadership."));
        m.setLore(lore);
        item.setItemMeta(m);
        return item;
    }

    public ItemStack createAcceptDemandsButton(Movement movement) {
        ItemStack item = new ItemStack(Material.GREEN_CONCRETE);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(StringFormatter.formatHex("#50e846Accept Demands"));
        List<String> lore = new ArrayList<>();
        lore.add(StringFormatter.formatHex("#7a7a7aAccept the movement's demands"));
        lore.add(StringFormatter.formatHex("#7a7a7aand implement their changes."));
        lore.add(" ");
        lore.add(StringFormatter.formatHex("#45afc4Click to Accept"));
        m.setLore(lore);
        m.getPersistentDataContainer().set(Keys.STRING_KEY, PersistentDataType.STRING, movement.getId());
        item.setItemMeta(m);
        return item;
    }

    public ItemStack createDeclineDemandsButton(Movement movement) {
        ItemStack item = new ItemStack(Material.RED_CONCRETE);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(StringFormatter.formatHex("#c74d32Decline Demands"));
        List<String> lore = new ArrayList<>();
        lore.add(StringFormatter.formatHex("#7a7a7aDecline the movement's demands"));
        lore.add(StringFormatter.formatHex("#7a7a7aand face a civil war."));
        lore.add(" ");
        lore.add(StringFormatter.formatHex("#d65c5cClick to Decline"));
        m.setLore(lore);
        m.getPersistentDataContainer().set(Keys.STRING_KEY, PersistentDataType.STRING, movement.getId());
        item.setItemMeta(m);
        return item;
    }
}
