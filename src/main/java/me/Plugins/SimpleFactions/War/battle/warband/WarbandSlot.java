package me.Plugins.SimpleFactions.War.battle.warband;

public class WarbandSlot {
	private int max;
	private int current;
	
	public WarbandSlot(int a) {
		max = a;
		current = 0;
	}
	
	public int getMax() {
		return max;
	}
	
	public int getCurrent() {
		return current;
	}
	
	public boolean isFull() {
		return max == current;
	}
	
	public void change(int a) {
		current += a;
		if(current < 0) current = 0;
	}
}