package me.Plugins.SimpleFactions.Managers.Holder;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import me.Plugins.SimpleFactions.enums.SFGUI;

public class SFInventoryHolder implements InventoryHolder {
    private final String id;
    private final SFGUI type;
    private int page;

    public SFInventoryHolder(String id, SFGUI type) {
        this.id = id;
        this.type = type;
        page = 0;
    }
    public SFInventoryHolder(String id, SFGUI type, int page) {
        this.id = id;
        this.type = type;
        this.page = page;
    }

    public int getPage() {
        return page;
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
