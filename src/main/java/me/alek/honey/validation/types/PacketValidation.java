package me.alek.honey.validation.types;

import me.alek.honey.netty.packetwrappers.WrappedPacket;

public interface PacketValidation<WP> extends HoneyValidation<WP> {

    interface Unknown extends PacketValidation<WrappedPacket<?>> {

    }
}
