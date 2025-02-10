package me.alek.honey.util.statics;

import me.alek.honey.netty.modelwrappers.WrappedLocation;
import org.bukkit.Location;

public class LocationUtils {

    private static boolean compare(double x1, double x2, double y1, double y2, double z1, double z2) {
        return x1 == x2 && y1 == y2 && z1 == z2;
    }

    public static boolean compare3DLocation(WrappedLocation loc1, WrappedLocation loc2) {
        if (loc1 == null || loc2 == null) return loc1 == loc2;

        return compare(loc1.getX(), loc2.getX(), loc1.getY(), loc2.getY(), loc1.getZ(), loc2.getZ());
    }

    public static boolean compareLocation(WrappedLocation loc1, WrappedLocation loc2) {
        if (loc1 == null || loc2 == null) return loc1 == loc2;

        if (!compare3DLocation(loc1, loc2)) {
            return false;
        }
        return loc1.getPitch() == loc2.getPitch() && loc1.getYaw() == loc2.getYaw();
    }

    public static boolean checkInvalidLocation(WrappedLocation... locations) {
        for (WrappedLocation location : locations) {
            if (location == null) continue;

            if (NumberUtils.isInvalid(location.getX(), location.getZ(), location.getY())
                    || NumberUtils.isInvalid(location.getPitch(), location.getPitch())) {
                return true;
            }
        }
        return false;
    }

    public static double calculateYDistance(WrappedLocation loc1, WrappedLocation loc2) {
        if (loc1 == null || loc2 == null) return 0d;

        if (checkInvalidLocation(loc1, loc2)) return -1;

        return Math.abs(loc1.getY() - loc2.getY());
    }

    public static float calculateRotationalDistance(float yaw1, float yaw2) {
        if (NumberUtils.isInvalid(yaw1, yaw2)) return -1;

        return Math.abs(yaw1 - yaw2);
    }

    public static double calculate2DDistance(WrappedLocation loc1, WrappedLocation loc2) {
        if (loc1 == null || loc2 == null) return 0d;

        if (checkInvalidLocation(loc1, loc2)) return -1;

        final double deltaX = Math.abs(loc1.getX() - loc2.getX());
        final double deltaZ = Math.abs(loc1.getZ() - loc2.getZ());

        return Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaZ, 2));
    }

    public static double calculate3DDistance(WrappedLocation loc1, WrappedLocation loc2) {
        if (loc1 == null || loc2 == null) return 0d;

        final double distance2D = calculate2DDistance(loc1, loc2);
        if (distance2D == -1) return -1;

        final double deltaY = Math.abs(loc1.getY() - loc2.getY());

        return Math.sqrt(Math.pow(distance2D, 2) + Math.pow(deltaY, 2));
    }
}
