package me.alek.honey.netty.packet.type;

import me.alek.honey.netty.utility.protocol.ProtocolRange;

public interface RangedPacketTypeEnum extends PacketTypeEnum {

    ProtocolRange getRange();
}
