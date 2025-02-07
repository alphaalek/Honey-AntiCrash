package me.alek.honey.netty.processor.impl;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.AutoReleasable;
import me.alek.honey.netty.packetwrappers.impl.play.client.WrappedPlayInCustomPayload;
import me.alek.honey.netty.processor.TypeProcessor;

public class BrandProcessor<CP extends WrappedPlayInCustomPayload<CP>> extends TypeProcessor<CP> {

    public static <CP extends WrappedPlayInCustomPayload<CP>> void load() {
        BrandProcessor<CP> processor = new BrandProcessor<>();
        processor.addListener(PacketTypes.Play.Client.CUSTOM_PAYLOAD, PacketContainer.PipelineState.INITIAL_BYTE, processor::handleCustomPayload);
    }

    public void handleCustomPayload(HoneyPlayer player, PacketContainer<CP> packet) {
        WrappedPlayInCustomPayload.PayloadData<CP> data = packet.getCurrentWrapped().getPayloadData();
        String channel = data.getChannel();
        if (channel.equals("MC|Brand") || channel.equals("minecraft:brand")) {
            PacketDataSerializer serializer = data.getSerializer();
            String band = serializer.readString();
            player.clientInfo.setBrand(band);
            AutoReleasable.doRelease(serializer);
        }
    }
}
