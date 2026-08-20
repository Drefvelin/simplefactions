package me.Plugins.SimpleFactions.War.battle.engine;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class CapturePoint {
	private String id;
	private Location loc;
	private BattleSide controller;
	private int captureProgress;
	private Collection<Entity> e;
	private HashMap<BattleSide, Integer> playerSide = new HashMap<>();
	private int sequenceIndex;
	private String advanceSideId;
	
	public Collection<Entity> getNearbyEntities(){
		return e;
	}
	public void updateNearbyEntities() {
		e = loc.getWorld().getNearbyEntities(loc, 10, 5, 10);
	}
	public void updateSides(List<BattleSide> sides) {
		playerSide.clear();
		for(Entity en : e) {
			if(en instanceof Player) {
				Player p = (Player) en;
				for(BattleSide s : sides) {
					if(s.hasPlayer(p)) {
						if(!playerSide.containsKey(s)) {
							playerSide.put(s, 1);
						} else {
							playerSide.put(s, playerSide.get(s)+1);
						}
					}
				}
			}
		}
		if(playerSide.size() > 0) {
			int maxValueInMap = (Collections.max(playerSide.values()));

	        for (Entry<BattleSide, Integer> entry :
	        	playerSide.entrySet()) {
	 
	            if (entry.getValue() == maxValueInMap && entry.getValue() >= 3) {
	                tickCapture(entry.getKey());
	            }
	        }
		}	
	}
	public BattleSide getController() {
		return controller;
	}

	public Location getLoc() {
		return loc;
	}
	public String getId() {
		return this.id;
	}

	public int getCaptureProgress() {
		return captureProgress;
	}

	public int getSequenceIndex() {
		return sequenceIndex;
	}

	public void setSequenceIndex(int sequenceIndex) {
		this.sequenceIndex = sequenceIndex;
	}

	public String getAdvanceSideId() {
		return advanceSideId;
	}

	public void setAdvanceSideId(String advanceSideId) {
		this.advanceSideId = advanceSideId;
	}

	public boolean isFullyControlledBy(String sideId) {
		return controller != null
				&& sideId != null
				&& controller.getId().equalsIgnoreCase(sideId)
				&& captureProgress >= 100;
	}

	public static boolean isFrontPoint(CapturePoint point, List<CapturePoint> points) {
		if (point == null || point.getAdvanceSideId() == null) {
			return true;
		}
		String sideId = point.getAdvanceSideId();
		int frontIndex = Integer.MAX_VALUE;
		for (CapturePoint candidate : points) {
			if (candidate.getAdvanceSideId() == null
					|| !candidate.getAdvanceSideId().equalsIgnoreCase(sideId)) {
				continue;
			}
			if (!candidate.isFullyControlledBy(sideId)) {
				frontIndex = Math.min(frontIndex, candidate.getSequenceIndex());
			}
		}
		if (frontIndex == Integer.MAX_VALUE) {
			return true;
		}
		return point.getSequenceIndex() == frontIndex;
	}
	private void tickCapture(BattleSide s) {
		if(controller.getId().equals(s.getId())) {
			if(this.captureProgress < 100) {
				this.captureProgress++;
			}
		} else {
			this.captureProgress--;
			if(this.captureProgress < 0) {
				this.controller = s;
				this.captureProgress++;
			}
		}
	}
	public CapturePoint(String id, Location l, BattleSide c, int prog) {
		this.id = id;
		this.loc = l;
		this.controller = c;
		this.captureProgress = prog;
	}
	public CapturePoint(CapturePoint another) {
		this.id = another.getId();
		this.loc = another.getLoc();
		this.controller = another.getController();
		this.captureProgress = another.getCaptureProgress();
		this.sequenceIndex = another.getSequenceIndex();
		this.advanceSideId = another.getAdvanceSideId();
	}
}