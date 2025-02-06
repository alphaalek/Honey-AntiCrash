package me.alek.honey.validation;

import me.alek.honey.netty.packetwrappers.ByteInWrappedPacket;
import me.alek.honey.netty.packetwrappers.SourcedWrappedPacket;

public interface PacketValidation<WP extends SourcedWrappedPacket<?>> extends HoneyValidation<WP> {

    interface Byte extends PacketValidation<ByteInWrappedPacket<?>> {

    }
}
