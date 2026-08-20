package me.Plugins.SimpleFactions.Map.export;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import me.Plugins.SimpleFactions.Managers.ProvinceManager;
import me.Plugins.SimpleFactions.Managers.RelationManager;
import me.Plugins.SimpleFactions.Map.Provinces.Province;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.SimpleFactions;

/**
 * Fort zone-of-control province selection for map export.
 *
 * <p>TODO step-43-war: exclude provinces owned by factions at war with fortOwner
 */
public final class ZocRealm {
    private ZocRealm() {
    }

    public static String topRealmId(Faction faction) {
        String topLiege = RelationManager.getTopLiege(faction);
        return topLiege != null ? topLiege : faction.getId();
    }

    public static boolean sameTopRealm(Faction a, Faction b) {
        return topRealmId(a).equalsIgnoreCase(topRealmId(b));
    }

    public static List<Integer> computeZocProvinces(Faction fortOwner, int fortProvince) {
        ProvinceManager provinceManager = SimpleFactions.getInstance().getProvinceManager();
        Province fortProv = provinceManager.get(fortProvince);
        if (fortProv == null || !fortProv.isValid()) {
            return List.of();
        }

        TreeSet<Integer> provinces = new TreeSet<>();
        provinces.add(fortProvince);

        for (int neighborId : fortProv.getNeighbours()) {
            Province neighbor = provinceManager.get(neighborId);
            if (neighbor == null || !neighbor.isValid() || neighbor.isSea()) {
                continue;
            }

            Faction owner = neighbor.getOwner();
            if (owner == null) {
                continue;
            }

            if (sameTopRealm(fortOwner, owner)) {
                provinces.add(neighborId);
            }
        }

        return new ArrayList<>(provinces);
    }
}
