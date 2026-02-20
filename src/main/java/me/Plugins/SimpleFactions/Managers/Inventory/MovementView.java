package me.Plugins.SimpleFactions.Managers.Inventory;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.InventoryManager;
import me.Plugins.SimpleFactions.Managers.Holder.SFInventoryHolder;
import me.Plugins.SimpleFactions.enums.Member;
import me.Plugins.SimpleFactions.enums.SFGUI;
import me.Plugins.SimpleFactions.government.movement.Movement;
import me.Plugins.SimpleFactions.government.movement.Phase;
import me.Plugins.SimpleFactions.government.movement.cause.Cause;
import me.Plugins.SimpleFactions.government.proposal.Proposal;
import me.Plugins.SimpleFactions.keys.Keys;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public class MovementView {
    public InventoryManager inv;
    
    public MovementCreator creator = new MovementCreator();
    
    private static final List<Integer> SLOTS = List.of(
        10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25
    );
    
    public MovementView(InventoryManager inv) {
        this.inv = inv;
    }
    
    public void movementView(Player player, Faction f, Movement movement, Inventory i) {
        boolean open = i == null;
        if (i == null) {
            i = Bukkit.createInventory(new SFInventoryHolder(f.getId(), SFGUI.MOVEMENT_VIEW), 54, "Movement: " + movement.getLeader());
        }
        i.clear();
        
        // Movement leader icon
        i.setItem(10, creator.createMovementLeaderItem(player, movement));
        
        // Organization icon
        i.setItem(11, creator.createOrganizationItem(movement));
        
        // Causes button (red banner)
        i.setItem(13, creator.createCausesButton(movement));
        
        // Join as supporter button
        i.setItem(15, creator.createJoinAsSupporterButton(player, movement));
        
        // Join as foreign backer button
        i.setItem(16, creator.createJoinAsForeignBackerButton(player, movement));

        if(movement.isLeader(player.getName())) {
            i.setItem(19, creator.createSendDemandsItem(movement));
            i.setItem(34, creator.createEndMovmentItem(movement));
        }

        int x = 28;
        for(Phase phase : Phase.values()) {
            i.setItem(x, creator.createPhaseItem(phase, movement));
            x++;
        }
        
        // Back button
        i.setItem(53, inv.createBackButton(SFGUI.MOVEMENT_VIEW));
        
        if (open) {
            player.openInventory(i);
        }
    }
    
    public void causesView(Player player, Faction f, Movement movement, Inventory i) {
        boolean open = i == null;
        if (i == null) {
            i = Bukkit.createInventory(new SFInventoryHolder(f.getId(), SFGUI.CAUSES_VIEW), 54, "Causes");
        }
        i.clear();
        
        List<Cause> causes = movement.getCauses();
        final int MAX_CAUSES = 3;
        
        // Display up to 3 cause slots
        for (int index = 0; index < MAX_CAUSES; index++) {
            int slot = SLOTS.get(index);
            
            if (index < causes.size()) {
                // Active cause
                i.setItem(slot, creator.createCauseItem(causes.get(index)));
            } else if (index == causes.size() && canPlayerCreateCause(player, movement)) {
                // Available slot for new cause
                i.setItem(slot, creator.createAvailableCauseSlot(movement, index));
            } else {
                // Empty slot (locked)
                i.setItem(slot, creator.createEmptyCauseSlot(index));
            }
        }
        
        // Back button
        i.setItem(53, inv.createBackButton(SFGUI.CAUSES_VIEW));
        
        if (open) {
            player.openInventory(i);
        }
    }
    
    public void causeView(Player player, Faction f, Movement movement, Cause cause, Inventory i) {
        boolean open = i == null;
        if (i == null) {
            i = Bukkit.createInventory(new SFInventoryHolder(f.getId(), SFGUI.CAUSE_VIEW), 54, "Cause Details");
        }
        i.clear();
        
        // Cause leader icon
        i.setItem(10, creator.createCauseLeaderItem(player, cause));
        
        // Proposal icon
        i.setItem(12, creator.createCauseProposalItem(cause));
        
        // Join cause button
        i.setItem(14, creator.createJoinCauseButton(player, cause));
        
        // Back button
        i.setItem(53, inv.createBackButton(SFGUI.CAUSE_VIEW));
        
        if (open) {
            player.openInventory(i);
        }
    }
    
    public void movementListView(Player player, Faction f, Inventory i) {
        boolean open = i == null;
        if (i == null) {
            i = Bukkit.createInventory(new SFInventoryHolder(f.getId(), SFGUI.MOVEMENT_LIST), 54, "Active Movements");
        }
        i.clear();
        
        int x = 0;
        List<Movement> movements = f.getGovernment().getMovements();
        for (int slot : SLOTS) {
            if (x >= movements.size()) break;
            i.setItem(slot, creator.createMovementListItem(movements.get(x)));
            x++;
        }
        
        // Back button
        i.setItem(53, inv.createBackButton(SFGUI.MOVEMENT_LIST));
        
        if (open) {
            player.openInventory(i);
        }
    }

    public void demandsView(Player player, Faction f, Movement movement, Inventory i) {
        boolean open = i == null;
        if (i == null) {
            i = Bukkit.createInventory(new SFInventoryHolder(f.getId(), SFGUI.MOVEMENT_DEMANDS), 54, "Movement Demands");
        }
        i.clear();
        
        // Display each cause/demand
        int slot = 10;
        for (Cause cause : movement.getCauses()) {
            i.setItem(slot, creator.createDemandItem(cause, movement));
            slot++;
        }
        
        // Power display
        i.setItem(13, creator.createMovementPowerItem(movement));
        
        // Warning item
        i.setItem(22, creator.createDecliningWarningItem());
        
        // Accept button (green concrete)
        i.setItem(29, creator.createAcceptDemandsButton(movement));
        
        // Decline button (red concrete)
        i.setItem(33, creator.createDeclineDemandsButton(movement));
        
        if (open) {
            player.openInventory(i);
        }
    }
    
    public void click(InventoryClickEvent e, Inventory inventory, Player p) {
        ItemStack item = e.getCurrentItem();
        if (item == null) return;
        
        SFInventoryHolder holder = (SFInventoryHolder) inventory.getHolder();
        if (holder == null) return;
        
        Faction f = FactionManager.getByString(holder.getId());
        if (f == null) return;
        
        e.setCancelled(true);
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        
        SFGUI gui = holder.getType();
        Movement movement = null;
        if (meta.getPersistentDataContainer().has(Keys.STRING_KEY, PersistentDataType.STRING)) {
            String id = meta.getPersistentDataContainer().get(Keys.STRING_KEY, PersistentDataType.STRING);
            movement = f.getGovernment().getMovementById(id);
        }

        if (movement == null) {
            return;
        }
        
        switch (gui) {
            case MOVEMENT_LIST:
                handleMovementListClick(e, movement, p, f, inventory, meta);
                break;
            case MOVEMENT_VIEW:
                handleMovementViewClick(e, movement, p, f, inventory, meta);
                break;
            case CAUSES_VIEW:
                handleCausesViewClick(e, movement,  p, f, inventory, meta);
                break;
            case CAUSE_VIEW:
                handleCauseViewClick(e, movement, p, f, inventory, meta);
                break;
            case MOVEMENT_DEMANDS:
                handleDemandsViewClick(e, movement, p, f, inventory, meta);
                break;
            default:
                break;
        }
    }
    
    private void handleMovementListClick(InventoryClickEvent e, Movement movement, Player p, Faction f, Inventory inventory, ItemMeta meta) {
        // Check if a movement was clicked
        movementView(p, f, movement, null);
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1, 1);
    }
    
    private void handleMovementViewClick(InventoryClickEvent e, Movement movement, Player p, Faction f, Inventory inventory, ItemMeta meta) {
        int slot = e.getSlot();
        //leader
        if (slot == 10) {
            if(!movement.hasLeader() && movement.canBeLeader(p.getName())) {
                movement.setLeader(p.getName());
                movementView(p, f, movement, inventory);
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1, 1);
            }
        }
        
        // Causes button
        if (slot == 13) {
            causesView(p, f, movement, null);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1, 1);
        }
        // Join as supporter button
        else if (slot == 15) {
            if(!quickJoinCheck(p, movement)) return;
            Object joiningAs = getJoiningAs(p, movement);
            if(joiningAs == null) return;
            movement.join(joiningAs, null);
            movementView(p, f, movement, inventory);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1, 1);
        }
        // Join as foreign backer button
        else if (slot == 16) {
            Faction playerFaction = FactionManager.getByMember(p.getName());
            if (playerFaction != null && !playerFaction.getId().equals(f.getId())) {
                movement.joinAsForeignBacker(playerFaction);
                movementView(p, f, movement, inventory);
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1, 1);
            }
        }
        // Send demands button
        else if (slot == 19) {
            if (movement.isLeader(p.getName()) && movement.getOrganization() >= 100) {
                Player factionLeader = Bukkit.getPlayer(f.getLeader());
                if (factionLeader != null && factionLeader.isOnline()) {
                    demandsView(factionLeader, f, movement, null);
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1.5f);
                    p.sendMessage(StringFormatter.formatHex("&7Demands sent to " + f.getLeader()));
                } else {
                    p.sendMessage(StringFormatter.formatHex("&cThe faction leader must be online to send demands!"));
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                }
            }
        }
        // Phase buttons (slots 28-31)
        else if (slot >= 28 && slot <= 31) {
            ItemStack item = e.getCurrentItem();
            if (item != null && item.getType() == Material.YELLOW_CONCRETE) {
                if (meta.getPersistentDataContainer().has(Keys.SECONDARY_STRING_KEY, PersistentDataType.STRING)) {
                    String phaseName = meta.getPersistentDataContainer().get(Keys.SECONDARY_STRING_KEY, PersistentDataType.STRING);
                    try {
                        Phase targetPhase = Phase.valueOf(phaseName);
                        if (movement.canChangeToPhase(targetPhase)) {
                            movement.setPhase(targetPhase);
                            movementView(p, f, movement, inventory);
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1.5f);
                            p.sendMessage(StringFormatter.formatHex("&7Phase changed to " + targetPhase.getDisplayName()));
                        }
                    } catch (IllegalArgumentException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
        else if (slot == 34) {
            if(movement.isLeader(p.getName())) {
                // End movement
                movement.getFaction().getGovernment().endMovement(movement);
            }
            movementListView(p, f, null);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1, 1);
        }
    }
    
    private void handleCausesViewClick(InventoryClickEvent e, Movement movement, Player p, Faction f, Inventory inventory, ItemMeta meta) {
        ItemStack item = e.getCurrentItem();
        if (item == null) return;
        
        // Check if clicking on an available cause slot
        if (item.getType() == Material.YELLOW_CONCRETE && 
            meta.getPersistentDataContainer().has(Keys.STRING_KEY, PersistentDataType.STRING)) {
            String action = meta.getPersistentDataContainer().get(Keys.STRING_KEY, PersistentDataType.STRING);
            if ("CREATE_CAUSE".equals(action)) {
                createNewCause(p, movement, f);
                causesView(p, f, movement, inventory);
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
                return;
            }
        }
        
        // Check if a cause was clicked
        if (meta.getPersistentDataContainer().has(Keys.INT, PersistentDataType.INTEGER)) {
            int index = meta.getPersistentDataContainer().get(Keys.INT, PersistentDataType.INTEGER);
            if (index < movement.getCauses().size()) {
                Cause cause = movement.getCauses().get(index);
                if (cause != null) {
                    causeView(p, f, movement, cause, null);
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1, 1);
                }
            }
        }
    }
    
    private void handleCauseViewClick(InventoryClickEvent e, Movement movement, Player p, Faction f, Inventory inventory, ItemMeta meta) {
        int slot = e.getSlot();
        Cause cause = null;
        if (meta.getPersistentDataContainer().has(Keys.INT, PersistentDataType.INTEGER)) {
            int index = meta.getPersistentDataContainer().get(Keys.INT, PersistentDataType.INTEGER);
            cause = movement.getCauses().get(index);
        }
        if (cause == null) {
            return;
        }
        if(slot == 10) {
            if(!cause.hasLeader() && cause.canBeLeader(p.getName())) {
                cause.setLeader(p.getName());
                causeView(p, f, movement, cause, inventory);
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1, 1);
            }
        }
        // Join cause button
        if (slot == 14) {
            if(!quickJoinCheck(p, movement)) return;
            Object joiningAs = getJoiningAs(p, movement);
            if(joiningAs == null) return;
            movement.join(joiningAs, cause);
            causeView(p, f, movement, cause, inventory);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1, 1);
        }
    }

    private void handleDemandsViewClick(InventoryClickEvent e, Movement movement, Player p, Faction f, Inventory inventory, ItemMeta meta) {
        int slot = e.getSlot();
        
        // Accept button (green concrete)
        if (slot == 29) {
            if (p.getName().equalsIgnoreCase(f.getLeader())) {
                p.sendMessage(StringFormatter.formatHex("&aYou have accepted the movement's demands."));
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 2f);
                
                // Notify movement leader if online
                if (movement.hasLeader()) {
                    Player movementLeader = Bukkit.getPlayer(movement.getLeader());
                    if (movementLeader != null && movementLeader.isOnline()) {
                        movementLeader.sendMessage(StringFormatter.formatHex("&a" + f.getLeader() + " has accepted your demands!"));
                    }
                }
                
                // End movement
                f.getGovernment().endMovement(movement);
                p.closeInventory();
            }
        }
        // Decline button (red concrete)
        else if (slot == 33) {
            if (p.getName().equalsIgnoreCase(f.getLeader())) {
                p.sendMessage(StringFormatter.formatHex("&cYou have declined the movement's demands."));
                p.sendMessage(StringFormatter.formatHex("&7A civil war has begun!"));
                p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1, 0.8f);
                
                // Notify movement leader if online
                if (movement.hasLeader()) {
                    Player movementLeader = Bukkit.getPlayer(movement.getLeader());
                    if (movementLeader != null && movementLeader.isOnline()) {
                        movementLeader.sendMessage(StringFormatter.formatHex("&c" + f.getLeader() + " has declined your demands!"));
                        movementLeader.sendMessage(StringFormatter.formatHex("&7A civil war has begun!"));
                    }
                }
                
                // End movement (civil war effects will be added later)
                f.getGovernment().endMovement(movement);
                p.closeInventory();
            }
        }
    }

    private boolean quickJoinCheck(Player p, Movement movement) {
        if(movement.isMember(p.getName())) return false;
        Member relation = movement.getFaction().getRelationToFaction(p.getName());
        switch(relation) {
            case FOREIGNER:
            case GUILD_MEMBER:
            case LEADER:
            case VASSAL_MEMBER:
            default:
                return false;
            case MEMBER:
            case VASSAL_LEADER:
            case GUILD_LEADER:
                return true;
            
        }
    }

    private Object getJoiningAs(Player p, Movement movement) {
        Member relation = movement.getFaction().getRelationToFaction(p.getName());
        if (relation == Member.GUILD_LEADER || relation == Member.GUILD_MEMBER) {
            return FactionManager.getGuildByMember(p.getName());
        } else if (relation == Member.VASSAL_LEADER) {
            return FactionManager.getByMember(p.getName());
        } else if (relation == Member.MEMBER) {
            return p.getName();
        }
        return null;
    }

    private boolean canPlayerCreateCause(Player p, Movement movement) {
        // Check if player is a supporter
        Object supporter = getSupporterObject(p, movement);
        if (supporter == null) return false;
        
        // Check if movement has room for more causes
        if (movement.getCauses().size() >= 3) return false;
        
        return true;
    }

    private Object getSupporterObject(Player p, Movement movement) {
        String playerName = p.getName();
        
        // Check if player is in supporters as a citizen
        if (movement.getSupporters().getCitizens().contains(playerName)) {
            return playerName;
        }
        
        // Check if player's guild is a supporter
        Member relation = movement.getFaction().getRelationToFaction(playerName);
        if (relation == Member.GUILD_LEADER || relation == Member.GUILD_MEMBER) {
            Guild guild = FactionManager.getGuildByMember(playerName);
            if (guild != null && movement.getSupporters().getGuilds().contains(guild)) {
                return guild;
            }
        }
        
        // Check if player's faction is a supporter
        if (relation == Member.VASSAL_LEADER || relation == Member.VASSAL_MEMBER) {
            Faction faction = FactionManager.getByMember(playerName);
            if (faction != null && movement.getSupporters().getFactions().contains(faction)) {
                return faction;
            }
        }
        
        return null;
    }

    private void createNewCause(Player p, Movement movement, Faction f) {
        Object supporter = getSupporterObject(p, movement);
        if (supporter == null) return;
        
        // Get the proposal from the first cause (all causes in a movement share the same proposal type)
        if (movement.getCauses().isEmpty()) return;
        
        Proposal proposal = movement.getCauses().get(0).getProposal();
        
        // Remove from supporters and create new cause
        movement.leave(supporter, null);
        movement.createCause(p.getName(), proposal);
    }
}
