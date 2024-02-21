package me.alek.honey.checks.impl;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import me.alek.honey.checks.*;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.netty.api.packet.ListenerPriority;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packet.bundle.PacketBundleFilter;
import me.alek.honey.netty.packet.bundle.PacketBundleFilterFactory;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.login.client.WrappedLoginInEncryptionResponse;
import me.alek.honey.netty.utility.NMSUtils;
import org.bukkit.entity.Player;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class EncryptionBeginListener extends AbstractPacketListener<WrappedLoginInEncryptionResponse> implements IPacketListener {

    @Override
    public boolean onPrePacketReceive(Player player, PacketContainer<WrappedLoginInEncryptionResponse> packetContainer, CheckContextContainer container) {
        return true;
    }

    @Override
    public void onPostPacketReceive(Player player, PacketContainer<WrappedLoginInEncryptionResponse> packetContainer) {

    }

    @Override
    public ListenerPriority getPriority() {
        return ListenerPriority.NORMAL;
    }

    @Override
    public CheckVisitor<WrappedLoginInEncryptionResponse> getVisitor() {
        CheckVisitor<WrappedLoginInEncryptionResponse> visitor = new CheckVisitor<>();
        visitor.visit(new EncryptionA());
        return visitor;
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Collections.singletonList(PacketType.Login.Client.ENCRYPTION_RESPONSE);
    }

    public static class EncryptionA implements PacketCheck<WrappedLoginInEncryptionResponse> {

        @Override
        public ResponseContainer check(Player player, WrappedLoginInEncryptionResponse packet, CheckContextContainer container) {
            ChannelPipeline pipeline = packet.getContainer().getChannel().pipeline();
            ChannelHandler handler = pipeline.get("packet_handler");

            if (NMSUtils.getNetworkManagerClass().isInstance(handler)) {
                InetSocketAddress address = NMSUtils.getSocketAddress(handler);

                if (address == null || address.getAddress() == null) {
                    return ResponseBuilder.inst().cancel().build();
                }
            }
            return ResponseBuilder.SUCCESS;
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
