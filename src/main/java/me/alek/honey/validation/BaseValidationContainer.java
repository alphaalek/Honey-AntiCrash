package me.alek.honey.validation;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.api.ListenerPriority;

public interface BaseValidationContainer {

    default void load() {}

    default void setup(HoneyPlayer player) {}

    default void cleanup(HoneyPlayer player) {}

    void register(PacketLibrary library);

    default ListenerPriority getPriority() {
        return ListenerPriority.NORMAL;
    }
}
