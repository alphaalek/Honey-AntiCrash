package me.alek.honey.checks.impl;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.checks.*;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.netty.api.ListenerPriority;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.bundle.PacketBundleFilter;
import me.alek.honey.netty.packet.bundle.PacketBundleFilterFactory;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.packetwrappers.login.client.WrappedLoginInEncryptionResponse;
import me.alek.honey.netty.util.NMS;
import me.alek.honey.util.CallbackAction;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class EncryptionBeginListener extends AbstractPacketListener<WrappedLoginInEncryptionResponse> implements IPacketListener {

    @Override
    public boolean onPrePacketReceive(HoneyPlayer player, PacketContainer<WrappedLoginInEncryptionResponse> packetContainer, CheckContextContainer container, CallbackAction<ResponseContainer> callback) {
        return true;
    }

    @Override
    public void onPostPacketReceive(HoneyPlayer player, PacketContainer<WrappedLoginInEncryptionResponse> packetContainer, CheckContextContainer container) {

    }

    @Override
    public boolean requiresPlayer() {
        return false;
    }

    @Override
    public ListenerPriority getPriority() {
        return ListenerPriority.NORMAL;
    }

    @Override
    public CheckVisitor<WrappedLoginInEncryptionResponse> getVisitor(HoneyPlayer player) {
        CheckVisitor<WrappedLoginInEncryptionResponse> visitor = new CheckVisitor<>(player);
        visitor.visit(new EncryptionA());
        return visitor;
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Collections.singletonList(PacketType.Login.Client.ENCRYPTION_RESPONSE);
    }

    public static class EncryptionA implements PacketCheck<WrappedLoginInEncryptionResponse> {

        @Override
        public ResponseContainer check(HoneyPlayer player, WrappedLoginInEncryptionResponse packet, CheckContextContainer container) {
            ChannelPipeline pipeline = packet.getContainer().getChannel().pipeline();
            ChannelHandler handler = pipeline.get("packet_handler");

            if (NMS.getNetworkManagerClass().isInstance(handler)) {
                InetSocketAddress address = NMS.getSocketAddress(handler);

                if (address == null || address.getAddress() == null) {
                    return ResponseBuilder.inst().cancel().build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public int getID() {
            return 17;
        }

        @Override
        public String getName() {
            return "Encryption.A";
        }

        @Override
        public String getDescription() {
            return "Encryption response with invalid ping";
        }

        @Override
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilterFactory.createEmptyFilter();
        }
    }
}
