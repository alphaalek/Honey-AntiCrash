package me.alek.honey.compat;

public class Paper {

    public static final boolean IS_PAPER = isPaper();

    private static boolean isPaper() {
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            return true; // Paper detected
        } catch (ClassNotFoundException e) {
            return false; // Not Paper
        }
    }
}
