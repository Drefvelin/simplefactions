package me.Plugins.SimpleFactions.Objects.Request;

import me.Plugins.SimpleFactions.Diplomacy.RelationType;
import me.Plugins.SimpleFactions.Guild.Guild;

public class RelationRequest extends Request{
	private RelationType type;
	
	public RelationRequest(Guild sender, RelationType type) {
		super(sender);
		this.type = type;
	}

	public RelationType getType() {
		return type;
	}
}
