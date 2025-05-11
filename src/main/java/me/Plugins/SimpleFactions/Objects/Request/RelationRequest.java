package me.Plugins.SimpleFactions.Objects.Request;

import me.Plugins.SimpleFactions.Diplomacy.RelationType;
import me.Plugins.SimpleFactions.Objects.Faction;

public class RelationRequest extends Request{
	private RelationType type;
	
	public RelationRequest(Faction sender, RelationType type) {
		super(sender);
		this.type = type;
	}

	public RelationType getType() {
		return type;
	}
}
