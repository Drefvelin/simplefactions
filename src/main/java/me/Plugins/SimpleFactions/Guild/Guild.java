package me.Plugins.SimpleFactions.Guild;

import java.util.ArrayList;
import java.util.List;

import me.Plugins.SimpleFactions.Guild.Branch.Branch;

public class Guild {
    private String id;
    private List<String> members = new ArrayList<>();
    private List<Branch> branches = new ArrayList<>();

    private int capital;
}
