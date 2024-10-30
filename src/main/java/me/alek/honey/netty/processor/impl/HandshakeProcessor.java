package me.alek.honey.netty.processor.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.processor.Processor;
import me.alek.honey.netty.packetwrappers.handshake.client.WrappedHandshakeInSetProtocol;
import me.alek.honey.util.ClientInfo;

public class HandshakeProcessor extends Processor<WrappedHandshakeInSetProtocol> {

    public HandshakeProcessor() {
        addListener(PacketTypes.Handshake.Client.SET_PROTOCOL, PacketContainer.PipelineState.FINAL_BYTE, this::handleHandshake, (p, t) -> p.channel.close());
    }

    public void handleHandshake(HoneyPlayer player, PacketContainer<WrappedHandshakeInSetProtocol> packet) {
        player.clientInfo = new ClientInfo(packet.getAnyWrapped().getProtocolVersion());
    }
}
