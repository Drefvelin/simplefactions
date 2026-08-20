package me.Plugins.SimpleFactions.Managers.Holder;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import me.Plugins.SimpleFactions.enums.SFGUI;

public class CampaignInventoryHolder implements InventoryHolder {
	private final int warId;
	private final int routePage;
	private final SFGUI type;

	public CampaignInventoryHolder(int warId, int routePage, SFGUI type) {
		this.warId = warId;
		this.routePage = routePage;
		this.type = type;
	}

	public int getWarId() {
		return warId;
	}

	public int getRoutePage() {
		return routePage;
	}

	public SFGUI getType() {
		return type;
	}

	@Override
	public Inventory getInventory() {
		return null;
	}
}
