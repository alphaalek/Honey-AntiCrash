package me.alek.honey.checks;

import org.bukkit.entity.Player;

public interface IListener {

    default void load() {}

    default void setup(Player player) {}

    default void cleanup(Player player) {}

}
