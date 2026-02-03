package me.Plugins.SimpleFactions.Objects.Handler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.Guild.Guild;
import me.Plugins.SimpleFactions.Loaders.TitleLoader;
import me.Plugins.SimpleFactions.Managers.ProvinceManager;
import me.Plugins.SimpleFactions.Managers.RelationManager;
import me.Plugins.SimpleFactions.Managers.TitleManager;
import me.Plugins.SimpleFactions.Map.Provinces.Province;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Tiers.Title;

public class ProvinceHandler {
    private Faction f;
    private List<Integer> provinces = new ArrayList<>();

	private int capital = -1;

    public ProvinceHandler(Faction f, int capital, List<Integer> provinces) {
		this.f = f;
        this.capital = capital;
		for(int i : provinces) {
			if(TitleManager.getByProvince(i) != null) continue;
			this.provinces.add(i);
		}
    }

    public ProvinceHandler(Faction f) {
        this.f = f;
        this.capital = -1;
        this.provinces = new ArrayList<>();
    }

    public boolean hasCapital() {
		return capital != -1;
	}

	public int getCapital() {
		return capital;
	}

	public void setCapital(int i) {
		if(!provinces.contains(i)) return;
		capital = i;
		SimpleFactions.getInstance().getProvinceManager().recalculateForSingleGuild(f.getOrCreateMainGuild(), true);
	}

    public boolean hasProvince(int i) {
		return provinces.contains(i);
	}
	
	public void addProvince(int i) {
		if(provinces.contains(i)) return;
		provinces.add(i);
		f.updateTier();
	}

    public void removeProvince(int i, boolean destroyTitles) {
		for(int x = 0; x<provinces.size(); x++) {
			int p = provinces.get(x);
			if(p == i) {
				provinces.remove(x);
                if(destroyTitles) {
                    Title t = TitleLoader.getByProvince(p);
                    if(t != null) {
                        List<Integer> provinces = TitleManager.getProvinces(f);
                        List<Title> titles = TitleManager.getTitles(f);
                        t.destroy(f, provinces, titles);
                    }
                }
				return;
			}
		}
		f.updateTier();
	}
	public List<Integer> getProvinces(){
		return provinces;
	}

    public List<Integer> getUntitledProvinces() {
		List<Integer> p = new ArrayList<>();
		for(int i : provinces) {
			if(TitleLoader.getByProvince(i) == null) p.add(i);
		}
		for(Faction subject : RelationManager.getSubjects(f)) {
			for(int i : subject.getProvinces()) {
				if(TitleLoader.getByProvince(i) == null) p.add(i);
			}
		}
		return p;
	}

    public void provinceCap() {
		if(TitleManager.overProvinceCap(f) && provinces.size() > 0) {
			int toRemove = provinces.get(provinces.size() - 1);
			if(toRemove == capital && provinces.size() > 1) toRemove = provinces.get(provinces.size() - 2);
			if(toRemove == capital) return;
			removeProvince(toRemove, true);
		}
	}

    //claims

	public boolean canClaim(int provinceId, boolean sea) {
        if(provinces.size() == 0) return true;
		if(provinces.contains(provinceId)) return false;

		ProvinceManager pm = SimpleFactions.getInstance().getProvinceManager();
		Province target = pm.get(provinceId);
		if (target == null || !target.isValid()) return false;

		// 1) Normal land adjacency
		for (int ownedId : provinces) {
			Province owned = pm.get(ownedId);
			if (owned != null && owned.getNeighbours().contains(provinceId)) {
				return true;
			}
		}

		// 2) Sea-adjacency mode
		if (sea) {
			return isSeaAdjacent(pm, target);
		}

		return false;
	}

	private boolean isLandConnected(ProvinceManager pm, int startId, int targetId) {
		Set<Integer> visited = new HashSet<>();
		ArrayDeque<Integer> queue = new ArrayDeque<>();

		queue.add(startId);
		visited.add(startId);

		while (!queue.isEmpty()) {
			int currentId = queue.poll();
			if (currentId == targetId) return true;

			Province current = pm.get(currentId);
			if (current == null) continue;

			for (int nId : current.getNeighbours()) {
				if (visited.contains(nId)) continue;
				if (!provinces.contains(nId)) continue;

				Province n = pm.get(nId);
				if (n == null || n.isSea()) continue;

				visited.add(nId);
				queue.add(nId);
			}
		}

		return false;
	}

	private boolean isSeaAdjacent(ProvinceManager pm, Province target) {
		Set<Integer> visited = new HashSet<>();
		ArrayDeque<Integer> queue = new ArrayDeque<>();

		// Seed the queue with sea/water provinces adjacent to owned territory
		for (int ownedId : provinces) {
			Province owned = pm.get(ownedId);
			if (owned == null) continue;

			for (int nId : owned.getNeighbours()) {
				Province n = pm.get(nId);
				if (n == null) continue;

				if (n.isSea()) {
					queue.add(nId);
					visited.add(nId);
				}
			}
		}

		// Flood-fill sea region
		while (!queue.isEmpty()) {
			int currentId = queue.poll();
			Province current = pm.get(currentId);
			if (current == null) continue;

			// If target touches this sea province
			if (current.getNeighbours().contains(target.getId())) {
				return true;
			}

			for (int nId : current.getNeighbours()) {
				if (visited.contains(nId)) continue;

				Province n = pm.get(nId);
				if (n == null || !n.isSea()) continue;

				visited.add(nId);
				queue.add(nId);
			}
		}

		return false;
	}

	public void revalidateClaims() {
		ProvinceManager pm = SimpleFactions.getInstance().getProvinceManager();

		Set<Integer> legal = new HashSet<>();

		// 1) Validate guild capitals
		for (Guild g : f.getGuildHandler().getGuilds()) {
			if (!g.hasCapital()) continue;

			int cap = g.getCapital();
			if (!isCapitalStillLegal(pm, cap)) {
				g.setCapital(-1);
			}
		}

		// 2) Flood from faction capital (land only)
		if (hasCapital()) {
            legal.add(capital);
			floodLand(pm, capital, legal);
		}

		// 3) Flood from valid guild capitals
		for (Guild g : f.getGuildHandler().getGuilds()) {
			if (!g.hasCapital()) continue;
			floodLand(pm, g.getCapital(), legal);
		}

		// 4) Unclaim illegal provinces
		for (int p : new ArrayList<>(provinces)) {
			if (!legal.contains(p)) {
				removeProvince(p, true);
			}
		}
	}

	private boolean isCapitalStillLegal(ProvinceManager pm, int capitalId) {
		// Land-connected to faction capital?
		if (hasCapital() && isLandConnected(pm, capital, capitalId)) {
			return true;
		}

		// Sea-connected?
		return isSeaAdjacent(pm, pm.get(capitalId));
	}

	private void floodLand(ProvinceManager pm, int start, Set<Integer> out) {
		ArrayDeque<Integer> queue = new ArrayDeque<>();
		Set<Integer> visited = new HashSet<>();

		queue.add(start);
		visited.add(start);

		while (!queue.isEmpty()) {
			int current = queue.poll();
			out.add(current);

			Province p = pm.get(current);
			if (p == null) continue;

			for (int n : p.getNeighbours()) {
				if (visited.contains(n)) continue;
				if (!provinces.contains(n)) continue;

				Province np = pm.get(n);
				if (np == null || np.isSea()) continue; // 🔴 important

				visited.add(n);
				queue.add(n);
			}
		}
	}

    public String getClaimDeniedReason(int provinceId, boolean sea) {
        ProvinceManager pm = SimpleFactions.getInstance().getProvinceManager();
        Province target = pm.get(provinceId);

        if (target == null || !target.isValid()) {
            return "§cThis location has no province.";
        }

        if (provinces.contains(provinceId)) {
            return "§cThis province is already part of your realm.";
        }

        // Land-only adjacency check
        boolean landAdjacent = false;
        for (int ownedId : provinces) {
            Province owned = pm.get(ownedId);
            if (owned != null && owned.getNeighbours().contains(provinceId)) {
                landAdjacent = true;
                break;
            }
        }

        if (!sea) {
            if (!landAdjacent) {
                return "§cThis province does not border your current realm.";
            }
            return "Success";
        }

        // Sea-enabled mode
        if (landAdjacent) return "Success";

        if (!isSeaAdjacent(pm, target)) {
            return "§cThis province must be connected to your realm by land or by sea.";
        }

        return "Success";
    }
}
