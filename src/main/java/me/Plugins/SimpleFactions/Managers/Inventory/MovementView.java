package me.Plugins.SimpleFactions.Managers.Inventory;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.InventoryManager;
import me.Plugins.SimpleFactions.Managers.Holder.SFInventoryHolder;
import me.Plugins.SimpleFactions.enums.SFGUI;
import me.Plugins.SimpleFactions.government.movement.Movement;
import me.Plugins.SimpleFactions.government.movement.cause.Cause;
import me.Plugins.SimpleFactions.keys.Keys;

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
        i.setItem(10, creator.createMovementLeaderItem(movement));
        
        // Organization icon
        i.setItem(11, creator.createOrganizationItem(movement));
        
        // Causes button (red banner)
        i.setItem(13, creator.createCausesButton(movement));
        
        // Join as supporter button
        i.setItem(15, creator.createJoinAsSupporterButton(player, movement));
        
        // Join as foreign backer button
        i.setItem(16, creator.createJoinAsForeignBackerButton(player, movement));
        
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
        
        int x = 0;
        List<Cause> causes = movement.getCauses();
        for (int slot : SLOTS) {
            if (x >= causes.size()) break;
            i.setItem(slot, creator.createCauseItem(causes.get(x)));
            x++;
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
        i.setItem(10, creator.createCauseLeaderItem(cause));
        
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
        
        switch (gui) {
            case MOVEMENT_LIST:
                handleMovementListClick(e, p, f, inventory, meta);
                break;
            case MOVEMENT_VIEW:
                handleMovementViewClick(e, p, f, inventory, meta);
                break;
            case CAUSES_VIEW:
                handleCausesViewClick(e, p, f, inventory, meta);
                break;
            case CAUSE_VIEW:
                handleCauseViewClick(e, p, f, inventory, meta);
                break;
            default:
                break;
        }
    }
    
    private void handleMovementListClick(InventoryClickEvent e, Player p, Faction f, Inventory inventory, ItemMeta meta) {
        // Check if a movement was clicked
        if (meta.getPersistentDataContainer().has(Keys.STRING_KEY, PersistentDataType.STRING)) {
            String leaderName = meta.getPersistentDataContainer().get(Keys.STRING_KEY, PersistentDataType.STRING);
            Movement movement = f.getGovernment().getMovementByLeader(leaderName);
            if (movement != null) {
                movementView(p, f, movement, null);
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1, 1);
            }
        }
    }
    
    private void handleMovementViewClick(InventoryClickEvent e, Player p, Faction f, Inventory inventory, ItemMeta meta) {
        Movement movement = f.getGovernment().getMovementByMember(p.getName());
        if (movement == null) {
            movement = f.getGovernment().getMovementByLeader(p.getName());
        }
        if (movement == null) return;
        
        int slot = e.getSlot();
        
        // Causes button
        if (slot == 13) {
            causesView(p, f, movement, null);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1, 1);
        }
        // Join as supporter button
        else if (slot == 15) {
            // TODO: Implement join logic
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1, 1);
        }
        // Join as foreign backer button
        else if (slot == 16) {
            // TODO: Implement join logic
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1, 1);
        }
    }
    
    private void handleCausesViewClick(InventoryClickEvent e, Player p, Faction f, Inventory inventory, ItemMeta meta) {
        Movement movement = f.getGovernment().getMovementByMember(p.getName());
        if (movement == null) {
            movement = f.getGovernment().getMovementByLeader(p.getName());
        }
        if (movement == null) return;
        
        // Check if a cause was clicked
        if (meta.getPersistentDataContainer().has(Keys.STRING_KEY, PersistentDataType.STRING)) {
            String leaderName = meta.getPersistentDataContainer().get(Keys.STRING_KEY, PersistentDataType.STRING);
            Cause cause = movement.getCauseByLeader(leaderName);
            if (cause != null) {
                causeView(p, f, movement, cause, null);
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1, 1);
            }
        }
    }
    
    private void handleCauseViewClick(InventoryClickEvent e, Player p, Faction f, Inventory inventory, ItemMeta meta) {
        int slot = e.getSlot();
        
        // Join cause button
        if (slot == 14) {
            // TODO: Implement join logic
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1, 1);
        }
    }
}
