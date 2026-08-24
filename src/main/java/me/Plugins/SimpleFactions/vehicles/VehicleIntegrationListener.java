package me.Plugins.SimpleFactions.vehicles;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import me.Plugins.SimpleFactions.SimpleFactions;
import me.Plugins.SimpleFactions.Loaders.VehiclesConfigLoader;
import net.tfminecraft.VFBuilders.core.Blueprint;
import net.tfminecraft.VFBuilders.events.BeginVehicleConstructionEvent;
import net.tfminecraft.VFBuilders.events.VehicleConstructEvent;
import net.tfminecraft.VehicleFramework.Cache.Cache;
import net.tfminecraft.VehicleFramework.Data.VehicleRemovePayload;
import net.tfminecraft.VehicleFramework.Events.VehicleRemoveEvent;
import net.tfminecraft.VehicleFramework.Vehicles.ActiveVehicle;
import net.tfminecraft.VehicleFramework.Vehicles.Vehicle;

public final class VehicleIntegrationListener implements Listener {
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBeginVehicleConstruction(BeginVehicleConstructionEvent event) {
        Player constructor = event.getConstructor();
        if (constructor == null) {
            return;
        }

        PlayerVehicleRegistry registry = SimpleFactions.getVehicleRegistry();
        if (!VehicleSlotGuard.isPersonalSlotAvailable(constructor.getUniqueId(), registry)) {
            event.setCancelled(true);
            int limit = VehiclesConfigLoader.getPersonalSlotLimit();
            constructor.sendMessage("§cYou already have a personal vehicle (limit: " + limit + ").");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehicleConstruct(VehicleConstructEvent event) {
        UUID constructorUuid = event.getConstructorUuid();
        ActiveVehicle vehicle = event.getVehicle();
        Blueprint blueprint = event.getBlueprint();

        if (constructorUuid == null || vehicle == null || blueprint == null) {
            SimpleFactions.getInstance().getLogger().warning(
                "[SimpleFactions] VehicleConstructEvent missing constructor, vehicle, or blueprint");
            return;
        }

        String ownerEntry = resolveOwnerEntry(constructorUuid, event.getConstructor());
        vehicle.getOwnerData().setOwner(ownerEntry);
        if (Cache.allowWhitelist) {
            vehicle.getOwnerData().setWhiteListed(true);
        }

        String vehicleTypeId = resolveVehicleTypeId(blueprint);
        SimpleFactions.getVehicleRegistry().register(
            new PlayerVehicleRecord(
                constructorUuid,
                vehicle.getUUID(),
                vehicleTypeId,
                OwnershipMode.PERSONAL,
                null));
        SimpleFactions.getInstance().saveVehicleRegistry();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehicleRemove(VehicleRemoveEvent event) {
        ActiveVehicle vehicle = event.getVehicle();
        if (vehicle == null || vehicle.getUUID() == null) {
            return;
        }
        VehicleRemovePayload payload = event.getPayload();
        if (SimpleFactions.getVehicleRegistry().unregister(vehicle.getUUID())) {
            SimpleFactions.getInstance().saveVehicleRegistry();
            if (payload != null && payload.isDeath()) {
                payload.getDeathCause().ifPresent(cause ->
                    SimpleFactions.getInstance().getLogger().info(
                        "Vehicle removed by death: "
                        + vehicle.getUUID()
                        + " cause="
                        + cause
                    )
                );
            }
        }
    }

    private static String resolveOwnerEntry(UUID constructorUuid, Player onlineConstructor) {
        if (onlineConstructor != null) {
            return "player_" + onlineConstructor.getName();
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(constructorUuid);
        String name = offline.getName();
        if (name != null && !name.isEmpty()) {
            return "player_" + name;
        }
        return "player_" + constructorUuid;
    }

    private static String resolveVehicleTypeId(Blueprint blueprint) {
        Vehicle vehicle = blueprint.getVehicle();
        if (vehicle != null && vehicle.getId() != null && !vehicle.getId().isEmpty()) {
            return vehicle.getId();
        }
        return blueprint.getId();
    }
}
