package me.alek.honey.netty.packet.type;

import me.alek.honey.netty.utility.protocol.NetProtocolRange;

public interface RangedPacketTypeEnum extends PacketTypeEnum {

    NetProtocolRange getRange();
}
