package me.alek.honey.checks;

import me.alek.honey.netty.api.packet.ListenerPriority;
import org.bukkit.entity.Player;

public interface IListener {

    default void load() {}

    default void setup(Player player) {}

    default void cleanup(Player player) {}

    boolean hasInitializedCheckInstances();

    HoneyCheckType getCheckType();

    default ListenerPriority getPriority() {
        return ListenerPriority.NORMAL;
    }
}
