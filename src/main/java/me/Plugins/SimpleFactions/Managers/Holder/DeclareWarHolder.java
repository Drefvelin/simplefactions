package me.Plugins.SimpleFactions.Managers.Holder;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import me.Plugins.SimpleFactions.enums.SFGUI;

public class DeclareWarHolder implements InventoryHolder {
	private final String attackerId;
	private final String defenderId;
	private final SFGUI step;

	public DeclareWarHolder(String attackerId, String defenderId, SFGUI step) {
		this.attackerId = attackerId;
		this.defenderId = defenderId;
		this.step = step;
	}

	public String getAttackerId() {
		return attackerId;
	}

	public String getDefenderId() {
		return defenderId;
	}

	public SFGUI getStep() {
		return step;
	}

	@Override
	public Inventory getInventory() {
		return null;
	}
}
