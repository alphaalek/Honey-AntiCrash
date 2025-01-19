package me.alek.honey.netty.processor.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.processor.TypeProcessor;
import me.alek.honey.netty.packetwrappers.impl.handshake.WrappedHandshakeInSetProtocol;

public class HandshakeProcessor extends TypeProcessor<WrappedHandshakeInSetProtocol> {

    public HandshakeProcessor() {
        addListener(PacketTypes.Handshake.Client.SET_PROTOCOL, PacketContainer.PipelineState.INITIAL_BYTE, this::handleHandshake, (p, t) -> p.channel.close());
    }

    public void handleHandshake(HoneyPlayer player, PacketContainer<WrappedHandshakeInSetProtocol> packet) {
        player.onVersionUpdate(packet.getAnyWrapped().getProtocolVersion());
    }
}
