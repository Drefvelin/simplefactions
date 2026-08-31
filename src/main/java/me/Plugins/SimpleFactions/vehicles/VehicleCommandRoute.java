package me.Plugins.SimpleFactions.vehicles;

public final class VehicleCommandRoute {
    private VehicleCommandRoute() {}

    /**
     * Installation id for a transfer command, empty string if the command matches
     * but the id is missing, or null if args are not a transfer command.
     */
    public static String transferInstallationId(String[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        if (args[0].equalsIgnoreCase("transfervehicle")) {
            return args.length >= 2 ? args[1] : "";
        }
        if (args[0].equalsIgnoreCase("vehicle")
                && args.length >= 2
                && args[1].equalsIgnoreCase("transfer")) {
            return args.length >= 3 ? args[2] : "";
        }
        return null;
    }

    public static boolean isMaintenancePay(String[] args) {
        return args != null
                && args.length >= 3
                && args[0].equalsIgnoreCase("vehicle")
                && args[1].equalsIgnoreCase("maintenance")
                && args[2].equalsIgnoreCase("pay");
    }

    public static boolean isVehicleRoot(String[] args) {
        return args != null && args.length >= 1 && args[0].equalsIgnoreCase("vehicle");
    }
}
