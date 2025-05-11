package me.Plugins.SimpleFactions.Objects.Request;

import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.War.War;

public class WarRequest extends Request{
	private War war;
	
	public WarRequest(Faction sender, War w) {
		super(sender);
		this.war = w;
	}

	public War getWar() {
		return war;
	}
}
