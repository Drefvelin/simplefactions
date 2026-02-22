package me.Plugins.SimpleFactions.Managers.Holder;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import me.Plugins.SimpleFactions.enums.SFGUI;

public class SFInventoryHolder implements InventoryHolder {
    private final String id;
    private final SFGUI type;
    private int page;
    private boolean flag;

    public SFInventoryHolder(String id, SFGUI type) {
        this.id = id;
        this.type = type;
        this.page = 0;
        this.flag = false;
    }
    
    public SFInventoryHolder(String id, SFGUI type, int page) {
        this.id = id;
        this.type = type;
        this.page = page;
        this.flag = false;
    }
    
    public SFInventoryHolder(String id, SFGUI type, int page, boolean flag) {
        this.id = id;
        this.type = type;
        this.page = page;
        this.flag = flag;
    }

    public int getPage() {
        return page;
    }
    
    public boolean getFlag() {
        return flag;
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
