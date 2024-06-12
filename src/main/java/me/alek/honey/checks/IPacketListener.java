package me.alek.honey.checks;

import me.alek.honey.netty.packet.type.PacketType;

import java.util.List;
import java.util.function.Supplier;

public interface IPacketListener extends IListener {

    Supplier<List<PacketType>> getPacketTypes();

    default HoneyCheckType getCheckType() {
        return HoneyCheckType.PACKET;
    }
}
