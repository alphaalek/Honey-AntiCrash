package me.alek.honey.netty.packet.type;

import me.alek.honey.util.Array;

public interface CommonPacketStateTypeEnum extends PacketTypeEnum {

    default PacketState getState() {
        return PacketState.UNKNOWN;
    }

    default boolean isCommonState() {
        return true;
    }

    Array<PacketState> getCommonStates();
}
