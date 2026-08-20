package me.Plugins.SimpleFactions.War.battle.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import me.Plugins.SimpleFactions.War.battle.enums.BattleType;

public class BattleEndedEvent extends Event {
	private static final HandlerList HANDLERS = new HandlerList();

	private final String battleId;
	private final BattleType battleType;
	private final Integer warId;
	private final String winningSideId;

	public BattleEndedEvent(String battleId, BattleType battleType, Integer warId, String winningSideId) {
		this.battleId = battleId;
		this.battleType = battleType;
		this.warId = warId;
		this.winningSideId = winningSideId;
	}

	public String getBattleId() {
		return battleId;
	}

	public BattleType getBattleType() {
		return battleType;
	}

	public Integer getWarId() {
		return warId;
	}

	public String getWinningSideId() {
		return winningSideId;
	}

	public boolean hasWinner() {
		return winningSideId != null && !winningSideId.isBlank();
	}

	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}
}
