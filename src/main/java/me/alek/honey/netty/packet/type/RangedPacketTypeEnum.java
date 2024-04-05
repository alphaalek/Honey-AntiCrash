package me.alek.honey.netty.packet.type;

import me.alek.honey.netty.util.NetProtocolRange;

public interface RangedPacketTypeEnum extends PacketTypeEnum {

    NetProtocolRange getRange();
}
