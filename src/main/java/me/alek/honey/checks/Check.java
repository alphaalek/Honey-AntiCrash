package me.alek.honey.checks;

import me.alek.honey.netty.packet.type.PacketTypeEnum;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public interface Check {

    String getName();

    default void load() {}

    default void setup(Player player) {}

    default void cleanup(Player player) {}

    default void resetVls(Player player) {}

    default Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return ArrayList::new;
    }

    default CheckType getType() {
        return CheckType.NETTY;
    }
}
