package me.Plugins.SimpleFactions.Database;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;

import me.Plugins.SimpleFactions.Army.Military;
import me.Plugins.SimpleFactions.Army.MilitaryExpansion;
import me.Plugins.SimpleFactions.Army.Regiment;
import me.Plugins.SimpleFactions.Guild.Branch.Branch;
import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Loaders.BranchLoader;
import me.Plugins.SimpleFactions.Loaders.TitleLoader;
import me.Plugins.SimpleFactions.Loaders.WarGoalLoader;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.RelationManager;
import me.Plugins.SimpleFactions.Objects.Bank;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Objects.Modifier;
import me.Plugins.SimpleFactions.Tiers.Title;
import me.Plugins.SimpleFactions.War.Participant;
import me.Plugins.SimpleFactions.War.Side;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.War.WarGoal;

public class Database {

	public int getTimer() {
		try {
			File file = new File("plugins/SimpleFactions/Cache", "data.json");
			if (!file.exists()) return 0;

			TimerData data = JsonUtil.readJson(file, TimerData.class);
			return data != null ? data.time : 0;

		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
	}

	public void saveTimer(int time) {
		try {
			File folder = new File("plugins/SimpleFactions/Cache");
			if (!folder.exists()) folder.mkdirs();

			File file = new File(folder, "data.json");

			TimerData data = new TimerData();
			data.time = time;

			JsonUtil.writeJson(file, data);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

    /* =====================================================
     * FACTIONS
     * ===================================================== */

    public void loadFactions() {
        Bukkit.getLogger().info("[SimpleFactions] Loading factions (Gson)");

        File folder = new File("plugins/SimpleFactions/Data");
        if (!folder.exists()) folder.mkdirs();

        for (File file : Objects.requireNonNull(folder.listFiles())) {
            if (!file.getName().endsWith(".json")) continue;

            try {
                FactionData data = JsonUtil.readJson(file, FactionData.class);
                if (data == null || data.id == null) continue;

                // --- Provinces ---
                List<Integer> provinces = new ArrayList<>();
                for (Number n : data.provinces) provinces.add(n.intValue());

                // --- Titles ---
                List<Title> titles = new ArrayList<>();
                for (String tid : data.titles) {
                    Title t = TitleLoader.getById(tid);
                    if (t != null) titles.add(t);
                }

                int capital = data.capital != null ? data.capital : -1;
                int extraCap = data.extraNodeCapacity != null ? data.extraNodeCapacity.intValue() : 0;

                double tax = data.taxRate != null ? data.taxRate : 5.0;
                double vassalTax = data.vassalTaxRate != null ? data.vassalTaxRate : 100.0;

                Faction f = new Faction(
                        data.id,
                        data.rgb,
                        provinces,
                        titles,
                        data.leader,
                        data.name,
                        data.rulerTitle,
                        data.banner,
                        data.government,
                        data.culture,
                        data.religion,
                        extraCap,
                        loadModifiers(data.prestigeModifiers),
                        loadModifiers(data.wealthModifiers),
                        tax,
                        vassalTax,
                        capital
                );

                // --- Bank ---
                if ("true".equalsIgnoreCase(data.bank)) {
                    Chunk c = Bukkit.getWorld(data.world)
                            .getChunkAt(data.xPos.intValue(), data.zPos.intValue());
                    f.setBank(new Bank(f, data.balance != null ? data.balance : 0.0, c));
                    f.updateWealth();
                }

                // --- Relations ---
                for (String r : data.relations) {
                    FactionManager.addDBRelation(f, r);
                }

                // --- Tier ---
                if (data.tierIndex != null) {
                    f.getTier().setIndex(data.tierIndex.intValue());
                }

                // --- Military ---
                Military m = f.getMilitary();
                for (String s : data.military) {
                    String[] split = s.split("\\.");
                    m.getRegiment(split[0]).setCurrentSlots(Integer.parseInt(split[1]));
                }

                for (String s : data.militaryQueue) {
                    String[] split = s.split("\\.");
                    m.addQueueItem(m.getRegiment(split[0]), Integer.parseInt(split[1]));
                }

                // --- Guild ---
                if (data.guilds != null) {
                    for (GuildData gd : data.guilds) {

                        Map<Integer, Branch> branches = new HashMap<>();
                        for (GuildBranchData bd : gd.branches) {
                            Branch base = BranchLoader.getByString(bd.id);
                            if (base != null) {
                                branches.put(
                                    bd.group.intValue(),
                                    new Branch(base, bd.level.intValue())
                                );
                            }
                        }

                        Guild g = new Guild(
                            gd.id,
                            gd.name,
                            gd.leader,
                            gd.rgb,
                            gd.capital != null ? gd.capital : -1,
                            gd.type,
                            gd.members,
                            branches,
                            f
                        );

                        f.getGuildHandler().addGuild(g);
                    }
                }

                FactionManager.factions.add(f);
                f.updateWealth();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* =====================================================
     * SAVE
     * ===================================================== */

    public void saveFaction(Faction f) {
        try {
            File file = new File("plugins/SimpleFactions/Data", f.getId() + ".json");

            FactionData data = new FactionData();
            data.id = f.getId();
            data.name = f.getName();
            data.rgb = f.getRGB();
            data.leader = f.getLeader();
            data.rulerTitle = f.getRulerTitle();
            data.government = f.getGovernment();
            data.culture = f.getCulture();
            data.religion = f.getReligion();

            data.taxRate = f.getTaxRate();
            data.vassalTaxRate = f.getVassalTaxRate();
            data.capital = f.getCapital();
            data.extraNodeCapacity = (double) f.getExtraNodeCapacity();

            data.banner = new ArrayList<>(f.getBannerPatterns());

            for (int p : f.getProvinces()) data.provinces.add(p);
            for (Title t : f.getTitles()) data.titles.add(t.getId());

            // --- Bank ---
            if (f.getBank() != null) {
                Bank b = f.getBank();
                data.bank = "true";
                data.world = b.getChunk().getWorld().getName();
                data.xPos = (double) b.getChunk().getX();
                data.zPos = (double) b.getChunk().getZ();
                data.balance = b.getWealth();
            } else {
                data.bank = "false";
            }

            // --- Military ---
            for (Regiment r : f.getMilitary().getRegiments()) {
                if (!r.isLevy()) {
                    data.military.add(r.getId() + "." + r.getCurrentSlots());
                }
            }

            for (MilitaryExpansion e : f.getMilitary().getQueue()) {
                data.militaryQueue.add(e.getRegiment().getId() + "." + e.getTimeLeft());
            }

            // --- Relations ---
            f.getRelations().forEach((id, rel) ->
                    data.relations.add(id + "(" + rel.getType().getId() + "."
                            + rel.getAttitude().getId() + "." + rel.getOpinion() + ")"));

            // --- Modifiers ---
            f.getPrestigeModifiers().forEach(m ->
                    data.prestigeModifiers.add(m.getType() + "(" + m.getAmount() + ")"));

            f.getWealthModifiers().forEach(m ->
                    data.wealthModifiers.add(m.getType() + "(" + m.getAmount() + ")"));

            // --- Guild ---
            for (Guild g : f.getGuildHandler().getGuilds()) {

                GuildData gd = new GuildData();
                gd.id = g.getId();
                gd.name = g.getName();
                gd.leader = g.getLeader();
                gd.rgb = g.getRGB();
                gd.type = g.getType().getId();
                gd.capital = g.getCapital();
                gd.members = new ArrayList<>(g.getMembers());

                for (Map.Entry<Integer, Branch> e : g.getBranches().entrySet()) {
                    Branch b = e.getValue();
                    GuildBranchData bd = new GuildBranchData();
                    bd.group = e.getKey();
                    bd.id = b.getId();
                    bd.level = b.getLevel();
                    gd.branches.add(bd);
                }

                data.guilds.add(gd);
            }


            data.overlord = RelationManager.getOverlord(f);
            data.tierIndex = (double) f.getTier().getIndex();

            JsonUtil.writeJson(file, data);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* =====================================================
     * HELPERS
     * ===================================================== */

    private List<Modifier> loadModifiers(List<String> raw) {
        List<Modifier> list = new ArrayList<>();
        for (String s : raw) {
            String type = s.substring(0, s.indexOf("("));
            double amt = Double.parseDouble(s.substring(s.indexOf("(") + 1, s.indexOf(")")));
            list.add(new Modifier(type, amt));
        }
        return list;
    }

    public void deleteFaction(Faction f) {
        File file = new File("plugins/SimpleFactions/Data", f.getId() + ".json");
        if (file.exists()) file.delete();
    }

	public void saveWar(War war) {
		try {
			File folder = new File("plugins/SimpleFactions/Wars");
			if (!folder.exists()) folder.mkdirs();

			File file = new File(folder, "war_" + war.getId() + ".json");

			WarData data = new WarData();
			data.id = war.getId();
			data.attackers = serializeSide(war.getAttackers());
			data.defenders = serializeSide(war.getDefenders());

			JsonUtil.writeJson(file, data);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private SideData serializeSide(Side side) {
		SideData data = new SideData();
		data.leader = side.getLeader().getId();

		for (Participant p : side.getMainParticipants()) {
			data.participants.add(serializeParticipant(p));
		}

		return data;
	}

	private ParticipantData serializeParticipant(Participant p) {
		ParticipantData data = new ParticipantData();
		data.leader = p.getLeader().getId();
		data.civilWar = p.isCivilWar();

		for (Faction s : p.getSubjects()) {
			data.subjects.add(s.getId());
		}

		for (Map.Entry<Faction, Boolean> entry : p.getAllies().entrySet()) {
			data.allies.put(entry.getKey().getId(), entry.getValue());
		}

		for (Map.Entry<Faction, WarGoal> entry : p.getWarGoals().entrySet()) {
			data.warGoals.put(entry.getKey().getId(), entry.getValue().getId());
		}

		return data;
	}

	public List<War> loadWars() {
		List<War> wars = new ArrayList<>();
		File folder = new File("plugins/SimpleFactions/Wars");

		if (!folder.exists() || !folder.isDirectory()) return wars;

		for (File file : folder.listFiles()) {
			if (!file.getName().endsWith(".json")) continue;

			try {
				WarData data = JsonUtil.readJson(file, WarData.class);
				if (data == null) continue;

				Faction atkLeader = FactionManager.getByString(data.attackers.leader);
				Faction defLeader = FactionManager.getByString(data.defenders.leader);

				if (atkLeader == null || defLeader == null) continue;

				War war = new War(data.id, atkLeader, defLeader);

				war.getAttackers().getMainParticipants().clear();
				war.getDefenders().getMainParticipants().clear();

				loadParticipants(data.attackers, war.getAttackers());
				loadParticipants(data.defenders, war.getDefenders());

				wars.add(war);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return wars;
	}

	private void loadParticipants(SideData data, Side side) {
		for (ParticipantData pData : data.participants) {

			Faction leader = FactionManager.getByString(pData.leader);
			if (leader == null) continue;

			List<Faction> subjects = new ArrayList<>();
			for (String id : pData.subjects) {
				Faction f = FactionManager.getByString(id);
				if (f != null) subjects.add(f);
			}

			Map<Faction, Boolean> allies = new HashMap<>();
			for (Map.Entry<String, Boolean> entry : pData.allies.entrySet()) {
				Faction f = FactionManager.getByString(entry.getKey());
				if (f != null) allies.put(f, entry.getValue());
			}

			Map<Faction, WarGoal> warGoals = new HashMap<>();
			for (Map.Entry<String, String> entry : pData.warGoals.entrySet()) {
				Faction target = FactionManager.getByString(entry.getKey());
				WarGoal goal = WarGoalLoader.getByString(entry.getValue());
				if (target != null && goal != null) {
					warGoals.put(target, goal);
				}
			}

			Participant p = new Participant(
					leader,
					subjects,
					allies,
					warGoals,
					pData.civilWar
			);

			side.getMainParticipants().add(p);
		}
	}

	public void deleteWar(War war) {
		File file = new File("plugins/SimpleFactions/Wars", "war_" + war.getId() + ".json");
		if (file.exists()) file.delete();
	}
}