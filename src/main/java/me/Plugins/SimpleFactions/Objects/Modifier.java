package me.Plugins.SimpleFactions.Objects;

public class Modifier {
	String type;
	Double amount;
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public Double getAmount() {
		return amount;
	}
	public void setAmount(Double amount) {
		this.amount = amount;
	}
	public Modifier(String t, Double a) {
		this.type = t;
		this.amount = a;
	}
}
