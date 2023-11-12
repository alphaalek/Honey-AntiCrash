package me.alek.honey.checks;

import me.alek.honey.netty.packet.type.PacketTypeEnum;

import java.util.List;
import java.util.function.Supplier;

public interface IPacketListener extends IListener {

    Supplier<List<PacketTypeEnum>> getPacketTypes();

    default HoneyCheckType getCheckType() {
        return HoneyCheckType.PACKET;
    }
}
