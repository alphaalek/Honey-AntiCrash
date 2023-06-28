package me.alek.honey.utility;

import org.bukkit.Location;

public class Locations {

    public static boolean compare3DLocation(Location loc1, Location loc2) {
        return loc1.getX() == loc2.getX() && loc1.getY() == loc2.getY() && loc1.getZ() == loc2.getZ();
    }

    public static boolean compareCompleteLocation(Location loc1, Location loc2) {
        if (!compare3DLocation(loc1, loc2)) {
            return false;
        }
        return loc1.getPitch() == loc2.getPitch() && loc1.getYaw() == loc2.getYaw();
    }

    public static boolean checkInvalidLocation(Location... locations) {
        for (Location location : locations) {

            if (NumberConversions.isInvalid(new double[]{location.getX(), location.getZ(), location.getY()}) || NumberConversions.isInvalid(new float[]{location.getPitch(), location.getPitch()})) {
                return true;
            }
        }
        return false;
    }

    public static double calculateYDistance(Location loc1, Location loc2) {
        if (checkInvalidLocation(loc1, loc2)) {
            return 1_000_000;
        }
        return Math.abs(loc1.getY() - loc2.getY());
    }

    public static float calculateYawDistance(float yaw1, float yaw2) {
        if (NumberConversions.isInvalid(new float[]{yaw1, yaw2})) {
            return 1_000_000f;
        }
        return Math.abs(yaw1 - yaw2);
    }

    public static double calculate2DDistance(Location loc1, Location loc2) {
        if (checkInvalidLocation(loc1, loc2)) {
            return 1_000_000;
        }
        final double deltaX = Math.abs(loc1.getX() - loc2.getX());
        final double deltaZ = Math.abs(loc1.getZ() - loc2.getZ());

        return Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaZ, 2));
    }
}
