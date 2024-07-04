package me.alek.honey.netty.processor.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.processor.Processor;
import me.alek.honey.packetwrappers.handshake.client.WrappedHandshakeInSetProtocol;
import me.alek.honey.util.ClientInfo;

public class HandshakeProcessor extends Processor<WrappedHandshakeInSetProtocol> {

    public HandshakeProcessor() {
        addReader(PacketTypes.Handshake.Client.SET_PROTOCOL, new WrappedHandshakeInSetProtocol.Reader(), this::handleClientIntent, (p, t) -> p.channel.close());
    }

    public void handleClientIntent(HoneyPlayer player, Object[] data) {
        player.clientInfo = new ClientInfo((int) data[0], (String) data[1]);
    }
}
