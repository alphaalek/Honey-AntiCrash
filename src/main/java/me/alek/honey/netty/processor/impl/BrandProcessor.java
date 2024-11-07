package me.alek.honey.netty.processor.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInCustomPayload;
import me.alek.honey.netty.processor.Processor;

public class BrandProcessor<CP extends WrappedPlayInCustomPayload<CP>> extends Processor<CP> {

    public BrandProcessor() {
        addListener(PacketTypes.Play.Client.CUSTOM_PAYLOAD, PacketContainer.PipelineState.INITIAL_BYTE, this::handleCustomPayload);
    }

    public void handleCustomPayload(HoneyPlayer player, PacketContainer<CP> packet) {
        WrappedPlayInCustomPayload.PayloadData data = packet.getAnyWrapped().getPayloadData();
        String channel = data.getChannel();
        if (channel.equals("MC|Brand") || channel.equals("minecraft:brand")) {
            String brand = data.makeSerializerCopy().readString(Short.MAX_VALUE);
            player.clientInfo.setBrand(brand);
        }
    }
}
