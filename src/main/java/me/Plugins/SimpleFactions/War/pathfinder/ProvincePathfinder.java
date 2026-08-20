package me.Plugins.SimpleFactions.War.pathfinder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import me.Plugins.SimpleFactions.Cache;
import me.Plugins.SimpleFactions.Map.Provinces.Province;
import me.Plugins.SimpleFactions.Managers.ProvinceManager;
import me.Plugins.SimpleFactions.War.War;
import me.Plugins.SimpleFactions.enums.Terrain;

public class ProvincePathfinder {
	private final ProvinceManager pm;
	private final ProvinceOwnerLookup owners;

	public ProvincePathfinder(ProvinceManager pm, ProvinceOwnerLookup owners) {
		this.pm = pm;
		this.owners = owners;
	}

	public PathfinderResult findRoute(
			int from,
			int to,
			PathfinderPass pass,
			BelligerentTerritory territory) {
		PathfinderResult result = dijkstra(from, to, pass, territory);
		return result.withStartProvinceId(from);
	}

	public PathfinderResult findRouteWithFallback(
			int from,
			int to,
			BelligerentTerritory territory) {
		List<PathfinderPass> passes = new ArrayList<>();
		passes.add(PathfinderPass.LAND_NO_NEUTRAL);
		if (Cache.warPathfinderSeaPassEnabled) {
			passes.add(PathfinderPass.SEA_NO_NEUTRAL);
		}
		passes.add(PathfinderPass.LAND_NEUTRAL_PENALTY);

		for (PathfinderPass pass : passes) {
			PathfinderResult result = findRoute(from, to, pass, territory);
			if (result.isFound()) {
				return result;
			}
		}
		return PathfinderResult.notFound();
	}

	public PathfinderResult computeCampaignLine(War war, int objectiveProvinceId) {
		BelligerentTerritory territory = BelligerentTerritory.fromWar(war, owners);
		List<Integer> candidates = territory.findInvasionEntryProvinces(pm);
		boolean seaContactFallback = candidates.isEmpty();
		if (seaContactFallback) {
			candidates = territory.findSeaInvasionEntryProvinces(pm);
			if (candidates.isEmpty()) {
				candidates = territory.findDefenderProvinces(pm);
			}
		}

		PathfinderResult best = PathfinderResult.notFound();
		for (int candidate : candidates) {
			PathfinderResult result = findRouteWithFallback(candidate, objectiveProvinceId, territory);
			if (!result.isFound()) {
				continue;
			}
			if (isBetterCandidate(result, candidate, territory, seaContactFallback, best)) {
				best = result;
			}
		}
		return best;
	}

	private boolean isBetterCandidate(
			PathfinderResult candidateResult,
			int candidateStart,
			BelligerentTerritory territory,
			boolean seaContactFallback,
			PathfinderResult currentBest) {
		if (!currentBest.isFound()) {
			return true;
		}
		if (candidateResult.getTotalCost() < currentBest.getTotalCost()) {
			return true;
		}
		if (candidateResult.getTotalCost() > currentBest.getTotalCost()) {
			return false;
		}
		if (seaContactFallback) {
			boolean candidateSea = territory.isAdjacentToSea(pm, candidateStart);
			boolean bestSea = territory.isAdjacentToSea(pm, currentBest.getStartProvinceId());
			if (candidateSea && !bestSea) {
				return true;
			}
			if (!candidateSea && bestSea) {
				return false;
			}
		}
		return candidateStart < currentBest.getStartProvinceId();
	}

	private PathfinderResult dijkstra(
			int from,
			int to,
			PathfinderPass pass,
			BelligerentTerritory territory) {
		Province start = pm.get(from);
		Province end = pm.get(to);
		if (start.getId() == 0 || end.getId() == 0) {
			return PathfinderResult.notFound();
		}
		if (!canTraverse(start, pass, territory) || !canTraverse(end, pass, territory)) {
			return PathfinderResult.notFound();
		}
		if (from == to) {
			return PathfinderResult.found(List.of(from), 0.0, pass, from);
		}

		Map<Integer, Double> dist = new HashMap<>();
		Map<Integer, Integer> previous = new HashMap<>();
		PriorityQueue<Integer> queue = new PriorityQueue<>(
				Comparator.comparingDouble(id -> dist.getOrDefault(id, Double.POSITIVE_INFINITY)));

		dist.put(from, 0.0);
		queue.add(from);

		while (!queue.isEmpty()) {
			int currentId = queue.poll();
			double currentCost = dist.getOrDefault(currentId, Double.POSITIVE_INFINITY);
			if (currentId == to) {
				break;
			}

			Province current = pm.get(currentId);
			if (current.getId() == 0) {
				continue;
			}

			for (int neighbourId : current.getNeighbours()) {
				Province neighbour = pm.get(neighbourId);
				if (neighbour.getId() == 0) {
					continue;
				}
				if (!canTraverse(neighbour, pass, territory)) {
					continue;
				}

				double stepCost = enterCost(neighbour, pass, territory);
				if (Double.isInfinite(stepCost)) {
					continue;
				}

				double nextCost = currentCost + stepCost;
				if (nextCost >= dist.getOrDefault(neighbourId, Double.POSITIVE_INFINITY)) {
					continue;
				}

				dist.put(neighbourId, nextCost);
				previous.put(neighbourId, currentId);
				queue.add(neighbourId);
			}
		}

		if (!dist.containsKey(to)) {
			return PathfinderResult.notFound();
		}

		List<Integer> path = reconstructPath(previous, from, to);
		double totalCost = dist.getOrDefault(to, Double.POSITIVE_INFINITY);
		return PathfinderResult.found(path, totalCost, pass, from);
	}

	private List<Integer> reconstructPath(Map<Integer, Integer> previous, int from, int to) {
		List<Integer> path = new ArrayList<>();
		Integer current = to;
		while (current != null) {
			path.add(0, current);
			if (current == from) {
				break;
			}
			current = previous.get(current);
		}
		return path;
	}

	private boolean canTraverse(Province province, PathfinderPass pass, BelligerentTerritory territory) {
		Terrain terrain = province.getTerrain();
		switch (pass) {
			case LAND_NO_NEUTRAL:
				return terrain != Terrain.SEA && !territory.isNeutral(province.getId());
			case SEA_NO_NEUTRAL:
				if (terrain == Terrain.SEA) {
					return true;
				}
				return !territory.isNeutral(province.getId());
			case LAND_NEUTRAL_PENALTY:
				return terrain != Terrain.SEA;
			default:
				return false;
		}
	}

	private double enterCost(Province province, PathfinderPass pass, BelligerentTerritory territory) {
		double cost = terrainEnterCost(province.getTerrain());
		if (pass == PathfinderPass.LAND_NEUTRAL_PENALTY && territory.isNeutral(province.getId())) {
			cost *= Cache.warPathfinderNeutralPenalty;
		}
		return cost;
	}

	static double terrainEnterCost(Terrain terrain) {
		if (Cache.warPathfinderWaterCost > 0 && terrain == Terrain.WATER) {
			return Cache.warPathfinderWaterCost;
		}
		double carry = Cache.getTradeCarry(terrain);
		return carry > 0 ? 1.0 / carry : Double.POSITIVE_INFINITY;
	}
}
