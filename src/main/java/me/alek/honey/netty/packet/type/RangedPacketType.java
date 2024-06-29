package me.alek.honey.netty.packet.type;

import me.alek.honey.util.protocol.NetProtocolRange;

public interface RangedPacketType extends PacketType {

    NetProtocolRange getRange();
}
