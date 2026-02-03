package me.Plugins.SimpleFactions.Objects.Request;

import me.Plugins.SimpleFactions.Guild.Guild;

public class RelocateRequest extends Request{
    int newCapital;
    public RelocateRequest(Guild sender, int newCapital) {
        super(sender);
        this.newCapital = newCapital;
    }

    public int getNewCapital() {
        return newCapital;
    }
}
