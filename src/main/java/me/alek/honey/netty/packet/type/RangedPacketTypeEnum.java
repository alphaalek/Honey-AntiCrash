package me.alek.honey.netty.packet.type;

import me.alek.honey.util.protocol.NetProtocolRange;

public interface RangedPacketTypeEnum extends PacketType {

    NetProtocolRange getRange();
}
