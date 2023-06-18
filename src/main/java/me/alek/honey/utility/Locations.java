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

    public static int calculate2DDistance(Location loc1, Location loc2) {
        final double x1 = loc1.getX();
        final double z1 = loc1.getZ();
        final double x2 = loc2.getX();
        final double z2 = loc2.getZ();

        if (NumberConversions.isInvalid(new double[]{x1, z1, x2, z2})) {
            return 1000000;
        }
        final double deltaX = Math.abs(x1 - x2);
        final double deltaZ = Math.abs(z1 - z2);

        return (int) Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaZ, 2));
    }
}
