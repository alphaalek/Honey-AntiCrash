package me.alek.honey.netty.processor.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInCustomPayload;
import me.alek.honey.netty.processor.Processor;

public class BrandProcessor extends Processor<WrappedPlayInCustomPayload> {

    public BrandProcessor() {
        addListener(PacketTypes.Play.Client.CUSTOM_PAYLOAD, this::handleCustomPayload, (p, t) -> p.channel.close());
    }

    public void handleCustomPayload(HoneyPlayer player, PacketContainer<WrappedPlayInCustomPayload> packet) {
        if (packet.getAnyWrapped().getChannel().equals("MC|Brand")) {
            String brand = packet.getAnyWrapped().getDataSerializer(true).readString(Short.MAX_VALUE);
            player.clientInfo.setBrand(brand);
        }
    }
}
