package me.alek.honey.netty.packet.type;

import me.alek.honey.util.Array;

public interface CommonPacketStateTypeEnum extends PacketType {

    default PacketState getState() {
        return PacketState.UNKNOWN;
    }

    Array<PacketState> getCommonStates();
}
