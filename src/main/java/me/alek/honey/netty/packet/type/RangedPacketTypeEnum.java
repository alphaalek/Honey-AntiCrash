package me.alek.honey.netty.packet.type;

import me.alek.honey.netty.utility.NetProtocolRange;

public interface RangedPacketTypeEnum extends PacketTypeEnum {

    NetProtocolRange getRange();
}
