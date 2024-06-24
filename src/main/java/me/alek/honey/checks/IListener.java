package me.alek.honey.checks;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.api.ListenerPriority;

public interface IListener {

    default void load() {}

    default void setup(HoneyPlayer player) {}

    default void cleanup(HoneyPlayer player) {}

    boolean hasInitializedCheckInstances();

    void register(PacketLibrary library);

    default ListenerPriority getPriority() {
        return ListenerPriority.NORMAL;
    }
}
