package me.alek.honey.checks;

import org.bukkit.entity.Player;

public interface Check {

    String getName();

    default void load() {}

    default void setup(Player player) {}

    default void cleanup(Player player) {}

    default void onResetVls(Player player) {}

}
