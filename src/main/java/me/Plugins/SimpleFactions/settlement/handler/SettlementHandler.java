package me.Plugins.SimpleFactions.settlement.handler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.Database.SettlementData;
import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Map.Provinces.Province;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.Utils.Formatter;
import me.Plugins.SimpleFactions.settlement.Settlement;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public class SettlementHandler {
    private final Faction faction;
    private final Map<String, Settlement> byId = new HashMap<>();
    private final Map<Integer, Settlement> provinceIndex = new HashMap<>();

    public SettlementHandler(Faction faction) {
        this.faction = faction;
    }

    public void load(List<SettlementData> data) {
        byId.clear();
        provinceIndex.clear();
        if (data == null) {
            return;
        }
        for (SettlementData entry : data) {
            try {
                register(new Settlement(entry));
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
        rebuildIndex();
    }

    public Settlement getByProvince(int provinceId) {
        return provinceIndex.get(provinceId);
    }

    public Settlement getById(String id) {
        if (id == null) {
            return null;
        }
        return byId.get(id);
    }

    public Collection<Settlement> getAll() {
        return byId.values();
    }

    public List<SettlementData> serialize() {
        List<SettlementData> out = new ArrayList<>();
        for (Settlement s : byId.values()) {
            out.add(s.toData());
        }
        return out;
    }

    public CapitalResult resolveGuildCapital(Player player, Guild guild, int province, String nameOpt) {
        if (guild.isBase()) {
            return CapitalResult.fail("§cUse §e/faction setcapital §cto set the faction capital");
        }
        if (!faction.hasProvince(province)) {
            return CapitalResult.fail("§cYour faction doesn't own this province!");
        }
        return resolveCapital(player, province, nameOpt, false, guild.getCapital(), false);
    }

    public CapitalResult resolveFactionCapital(Player player, int province, String nameOpt) {
        if (!faction.hasProvince(province)) {
            return CapitalResult.fail("§cYour faction doesn't own this province!");
        }
        if (byId.isEmpty() && (nameOpt == null || nameOpt.isBlank())) {
            return CapitalResult.fail(
                    "§cName required to found your capital city: §e/faction setcapital <name>");
        }
        return resolveCapital(player, province, nameOpt, true, faction.getCapital(), false);
    }

    public boolean requiresFoundingName(int province) {
        if (getByProvince(province) != null) {
            return false;
        }
        if (findJoinTarget(province) != null) {
            return false;
        }
        return minHopsToAnyCentre(province) >= Cache.settlementFoundDistance;
    }

    public void onGuildDepartedCapital(int province) {
        if (province == -1) {
            return;
        }
        Settlement settlement = getByProvince(province);
        if (settlement == null) {
            return;
        }
        if (getPopulation(settlement).isEmpty()) {
            dissolve(settlement);
        }
    }

    public CapitalResult onGuildRelocateTo(Player player, Guild guild, int newCapital, String nameOpt) {
        if (!faction.hasProvince(newCapital)) {
            return CapitalResult.fail("§cYour faction doesn't own this province!");
        }
        return resolveCapital(player, newCapital, nameOpt, false, -1, true);
    }

    private CapitalResult resolveCapital(
            Player player,
            int province,
            String nameOpt,
            boolean factionCapital,
            int currentCapital,
            boolean fromRelocate) {
        Province prov = SimpleFactions.getInstance().getProvinceManager().get(province);
        if (province == 0 || prov == null || !prov.isValid()) {
            return CapitalResult.fail("§cThis location has no province!");
        }
        if (prov.isSea()) {
            return CapitalResult.fail("§cYou cannot set a capital on water!");
        }

        if (!fromRelocate && currentCapital != -1 && currentCapital != province) {
            Settlement currentSettlement = getByProvince(currentCapital);
            Settlement targetSettlement = getByProvince(province);
            if (currentSettlement == null
                    || targetSettlement == null
                    || currentSettlement != targetSettlement) {
                return CapitalResult.fail(
                        "§cCapital already set — use relocate to move between cities");
            }
        }

        if (currentCapital == province) {
            Settlement existing = getByProvince(province);
            if (existing != null) {
                return CapitalResult.ok(
                        "§aCapital set in §f" + existing.getName(), existing);
            }
        }

        Settlement inSettlement = getByProvince(province);
        if (inSettlement != null) {
            if (factionCapital && !inSettlement.isCenter(province)) {
                return CapitalResult.fail("§cFaction capital must be the city centre");
            }
            return CapitalResult.ok(
                    "§aCapital set in §f" + inSettlement.getName(), inSettlement);
        }

        Settlement joinTarget = findJoinTarget(province);
        if (joinTarget != null) {
            if (factionCapital && !joinTarget.isCenter(province)) {
                return CapitalResult.fail("§cFaction capital must be the city centre");
            }
            CapitalResult joined = join(joinTarget, province);
            if (!joined.isSuccess()) {
                return joined;
            }
            return CapitalResult.ok(
                    "§aJoined settlement §f" + joinTarget.getName(), joinTarget);
        }

        int minHops = minHopsToAnyCentre(province);
        if (minHops >= Cache.settlementFoundDistance) {
            if (nameOpt == null || nameOpt.isBlank()) {
                return CapitalResult.fail("§cName required to found a settlement here");
            }
            return found(
                    nameOpt,
                    province,
                    player.getLocation().getBlockX(),
                    player.getLocation().getBlockZ());
        }

        Settlement nearest = nearestSettlement(province);
        if (nearest != null) {
            return CapitalResult.fail(
                    "§cToo close to §f" + nearest.getName() + " §cto found a new city");
        }
        return CapitalResult.fail("§cName required to found a settlement here");
    }

    public CapitalResult found(String displayName, int province, int x, int z) {
        String id = Formatter.formatId(displayName);
        if (id.isBlank()) {
            return CapitalResult.fail("§cInvalid settlement name");
        }
        if (byId.containsKey(id)) {
            return CapitalResult.fail("§cA settlement with that id already exists");
        }

        String name = StringFormatter.formatHex(Formatter.formatName(displayName));
        Settlement settlement = new Settlement(id, name, province, x, z);

        for (int p : initialTerritory(province)) {
            if (p != province) {
                settlement.addProvince(p);
            }
        }

        register(settlement);
        enqueueMapUpdate();

        return CapitalResult.ok(
                "§aFounded settlement §f" + name + " §7(" + id + ")", settlement);
    }

    public CapitalResult join(Settlement settlement, int province) {
        if (!settlement.contains(province)) {
            if (getByProvince(province) != null) {
                return CapitalResult.fail("§cThis province belongs to another settlement");
            }
            if (!faction.hasProvince(province)) {
                return CapitalResult.fail("§cYour faction doesn't own this province!");
            }
            settlement.addProvince(province);
            rebuildIndex();
            enqueueMapUpdate();
        }
        return CapitalResult.ok("§aJoined settlement §f" + settlement.getName(), settlement);
    }

    public void onProvinceClaimed(int province) {
        if (getByProvince(province) != null) {
            return;
        }
        if (!faction.hasProvince(province)) {
            return;
        }

        Province prov = SimpleFactions.getInstance().getProvinceManager().get(province);
        if (prov == null || prov.isSea()) {
            return;
        }

        List<Settlement> candidates = new ArrayList<>();
        for (Settlement s : byId.values()) {
            if (isLandAdjacentToSettlement(province, s)) {
                candidates.add(s);
            }
        }

        if (candidates.isEmpty()) {
            return;
        }

        Settlement chosen = candidates.size() == 1
                ? candidates.get(0)
                : candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));

        chosen.addProvince(province);
        rebuildIndex();
        enqueueMapUpdate();
    }

    public void onProvinceLost(int province) {
        Settlement settlement = getByProvince(province);
        if (settlement == null) {
            return;
        }

        if (settlement.isCenter(province)) {
            dissolve(settlement);
            return;
        }

        settlement.removeProvince(province);
        rebuildIndex();
        enqueueMapUpdate();
    }

    public List<Guild> getPopulation(Settlement settlement) {
        List<Guild> population = new ArrayList<>();
        for (Guild g : faction.getGuildHandler().getGuilds()) {
            if (settlement.contains(g.getCapital())) {
                population.add(g);
            }
        }
        return population;
    }

    public int minLandHops(int from, int to) {
        if (from == to) {
            return 0;
        }

        var pm = SimpleFactions.getInstance().getProvinceManager();
        Province start = pm.get(from);
        Province goal = pm.get(to);
        if (start == null || goal == null || !start.isValid() || !goal.isValid()) {
            return Integer.MAX_VALUE;
        }

        ArrayDeque<Integer> queue = new ArrayDeque<>();
        Map<Integer, Integer> depth = new HashMap<>();
        queue.add(from);
        depth.put(from, 0);

        while (!queue.isEmpty()) {
            int current = queue.poll();
            int d = depth.get(current);
            Province p = pm.get(current);
            if (p == null) {
                continue;
            }

            for (int neighbour : p.getNeighbours()) {
                if (depth.containsKey(neighbour)) {
                    continue;
                }
                Province np = pm.get(neighbour);
                if (np == null || np.isSea()) {
                    continue;
                }
                int nextDepth = d + 1;
                if (neighbour == to) {
                    return nextDepth;
                }
                depth.put(neighbour, nextDepth);
                queue.add(neighbour);
            }
        }

        return Integer.MAX_VALUE;
    }

    public Set<Integer> initialTerritory(int center) {
        Set<Integer> territory = new HashSet<>();
        territory.add(center);

        Province centerProv = SimpleFactions.getInstance().getProvinceManager().get(center);
        if (centerProv == null) {
            return territory;
        }

        for (int neighbour : centerProv.getNeighbours()) {
            if (!faction.hasProvince(neighbour)) {
                continue;
            }
            if (getByProvince(neighbour) != null) {
                continue;
            }
            Province np = SimpleFactions.getInstance().getProvinceManager().get(neighbour);
            if (np == null || np.isSea()) {
                continue;
            }
            territory.add(neighbour);
        }

        return territory;
    }

    public Settlement findJoinTarget(int province) {
        Settlement best = null;
        for (Settlement s : byId.values()) {
            if (minLandHops(province, s.getCenterProvince()) != 1) {
                continue;
            }
            if (best == null || s.getId().compareTo(best.getId()) < 0) {
                best = s;
            }
        }
        return best;
    }

    public void validate() {
        List<Settlement> snapshot = new ArrayList<>(byId.values());
        for (Settlement s : snapshot) {
            s.getProvinces().removeIf(p -> !faction.hasProvince(p));

            if (s.getProvinces().isEmpty()
                    || !s.contains(s.getCenterProvince())
                    || !faction.hasProvince(s.getCenterProvince())) {
                dissolve(s);
            }
        }
        rebuildIndex();
    }

    void register(Settlement s) {
        byId.put(s.getId(), s);
        rebuildIndex();
    }

    void dissolve(Settlement s) {
        String settlementName = s.getName();

        for (Guild g : faction.getGuildHandler().getGuilds()) {
            if (s.contains(g.getCapital())) {
                g.setCapital(-1, false);
            }
        }
        if (s.contains(faction.getCapital())) {
            faction.setCapital(-1);
        }

        byId.remove(s.getId());
        for (int p : s.getProvinces()) {
            provinceIndex.remove(p, s);
        }

        enqueueMapUpdate();

        Player leader = Bukkit.getPlayerExact(faction.getLeader());
        if (leader != null) {
            leader.sendMessage("§cSettlement §f" + settlementName + " §chas been destroyed");
        }
    }

    private int minHopsToAnyCentre(int province) {
        if (byId.isEmpty()) {
            return Integer.MAX_VALUE;
        }
        int min = Integer.MAX_VALUE;
        for (Settlement s : byId.values()) {
            min = Math.min(min, minLandHops(province, s.getCenterProvince()));
        }
        return min;
    }

    private Settlement nearestSettlement(int province) {
        Settlement nearest = null;
        int minHops = Integer.MAX_VALUE;
        for (Settlement s : byId.values()) {
            int hops = minLandHops(province, s.getCenterProvince());
            if (hops < minHops) {
                minHops = hops;
                nearest = s;
            }
        }
        return nearest;
    }

    private boolean isLandAdjacentToSettlement(int province, Settlement settlement) {
        Province p = SimpleFactions.getInstance().getProvinceManager().get(province);
        if (p == null || p.isSea()) {
            return false;
        }
        for (int neighbour : p.getNeighbours()) {
            if (!settlement.contains(neighbour)) {
                continue;
            }
            Province np = SimpleFactions.getInstance().getProvinceManager().get(neighbour);
            if (np != null && !np.isSea()) {
                return true;
            }
        }
        return false;
    }

    private void enqueueMapUpdate() {
        FactionManager.getMap().enqueue("nation", faction.getRGB());
    }

    private void rebuildIndex() {
        provinceIndex.clear();
        for (Settlement s : byId.values()) {
            for (int p : s.getProvinces()) {
                provinceIndex.put(p, s);
            }
        }
    }
}
