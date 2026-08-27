package me.Plugins.SimpleFactions.vehicles;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import net.tfminecraft.VehicleFramework.Enums.VehicleRemoveReason;
import net.tfminecraft.VehicleFramework.Events.VehiclePreInteractEvent;
import net.tfminecraft.VehicleFramework.Events.VehicleSpawnEvent;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;

public final class BattleVehicleEligibilityListener implements Listener {
	private final PlayerVehicleRegistry registry;

	public BattleVehicleEligibilityListener(PlayerVehicleRegistry registry) {
		this.registry = registry;
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onVehiclePreInteract(VehiclePreInteractEvent event) {
		Player player = event.getPlayer();
		ActiveVehicle vehicle = event.getVehicle();
		if (player == null || vehicle == null) {
			return;
		}

		BattleVehicleEligibilityResult result = BattleVehicleEligibilityService.check(player, vehicle, registry);
		if (!result.isDenied()) {
			return;
		}

		String message = BattleVehicleEligibilityMessages.forResult(result);
		if (message != null) {
			player.sendMessage(message);
		}
		event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onVehicleSpawn(VehicleSpawnEvent event) {
		ActiveVehicle vehicle = event.getVehicle();
		if (vehicle == null || vehicle.getUUID() == null) {
			return;
		}

		Player player = BattleVehicleEligibilityService.resolveNotifyPlayer(vehicle, registry);
		if (player == null) {
			return;
		}

		BattleVehicleEligibilityResult result = BattleVehicleEligibilityService.check(player, vehicle, registry);
		if (!result.isDenied()) {
			return;
		}

		vehicle.remove(VehicleRemoveReason.ADMIN_KILL);
		String message = BattleVehicleEligibilityMessages.forResult(result);
		if (message != null) {
			player.sendMessage(message);
		}
	}
}
