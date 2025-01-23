package me.alek.honey.validation.types;

import me.alek.honey.netty.packetwrappers.ByteWrappedPacket;

public interface PacketValidation<WP> extends HoneyValidation<WP> {

    interface Byte extends PacketValidation<ByteWrappedPacket<?>> {

    }
}
