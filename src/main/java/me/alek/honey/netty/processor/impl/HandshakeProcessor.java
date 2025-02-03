package me.alek.honey.netty.processor.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.processor.TypeProcessor;
import me.alek.honey.netty.packetwrappers.impl.handshake.WrappedHandshakeInSetProtocol;

public class HandshakeProcessor extends TypeProcessor<WrappedHandshakeInSetProtocol> {

    public static void load() {
        HandshakeProcessor processor = new HandshakeProcessor();
        processor.addListener(PacketTypes.Handshake.Client.SET_PROTOCOL, PacketContainer.PipelineState.INITIAL_BYTE, processor::handleHandshake);
    }

    public void handleHandshake(HoneyPlayer player, PacketContainer<WrappedHandshakeInSetProtocol> packet) {
        player.onVersionUpdate(packet.getCurrentWrapped().getProtocolVersion());
    }
}
