package me.Plugins.SimpleFactions.Managers.Holder;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import me.Plugins.SimpleFactions.enums.SFGUI;

public class SFInventoryHolder implements InventoryHolder {
    private final String id;
    private final SFGUI type;

    public SFInventoryHolder(String id, SFGUI type) {
        this.id = id;
        this.type = type;
    }

    public String getId() {
        return id;
    }
    
    public SFGUI getType() {
    	return type;
    }

    @Override
    public Inventory getInventory() {
        return null; // Not used in this case
    }
}
