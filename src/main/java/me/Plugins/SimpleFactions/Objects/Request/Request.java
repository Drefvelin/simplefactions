package me.Plugins.SimpleFactions.Objects.Request;

import me.Plugins.SimpleFactions.Objects.Faction;

public class Request {
	protected Faction sender;
	protected long time = System.currentTimeMillis()+6000;
	
	public Request(Faction sender) {
		this.sender = sender;
	}

	public Faction getSender() {
		return sender;
	}
	
	public boolean timedOut() {
		return System.currentTimeMillis() >= time;
	}
}
