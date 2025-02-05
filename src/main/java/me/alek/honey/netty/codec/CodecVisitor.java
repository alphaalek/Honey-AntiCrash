package me.alek.honey.netty.codec;

import me.alek.honey.util.protocol.NetProtocol;

public interface CodecVisitor {

    NetProtocol protocol();
}
