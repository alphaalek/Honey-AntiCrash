package me.alek.honey.listeners.check;

import me.alek.honey.netty.packet.type.PacketTypeEnum;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public interface Check {

    String getName();

    void cleanup(Player player);

    void resetVls(Player player);

    default Supplier<List<PacketTypeEnum>> getTypes() {
        return ArrayList::new;
    }

    default CheckType getType() {
        return CheckType.NETTY;
    }
}
