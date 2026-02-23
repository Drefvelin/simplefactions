package me.Plugins.SimpleFactions.Objects.Request;

import me.Plugins.SimpleFactions.Diplomacy.RelationType;
import me.Plugins.SimpleFactions.Guild.Guild;

public class RelationRequest extends Request{
	private RelationType type;
	private boolean trade;
	
	public RelationRequest(Guild sender, RelationType type, boolean trade) {
		super(sender);
		this.type = type;
		this.trade = trade;
	}

	public RelationType getType() {
		return type;
	}

	public boolean isTrade() {
		return trade;
	}
}
