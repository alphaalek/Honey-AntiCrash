package me.alek.honey.checks.listeners;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import me.alek.honey.checks.*;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.login.client.WrappedLoginInEncryptionResponse;
import me.alek.honey.netty.utility.reflect.NMSUtils;
import org.bukkit.entity.Player;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class EncryptionBegin extends AbstractPacketListener<WrappedLoginInEncryptionResponse> implements IPacketListener {

    @Override
    public boolean onPrePacketReceive(Player player, PacketContainer<WrappedLoginInEncryptionResponse> packetContainer, CheckContextContainer container) {
        return true;
    }

    @Override
    public void onPostPacketReceive(Player player, PacketContainer<WrappedLoginInEncryptionResponse> packetContainer) {

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
        public ResponseContainer accept(Player player, WrappedLoginInEncryptionResponse packet, CheckContextContainer container) {
            ChannelPipeline pipeline = packet.getContainer().getChannel().pipeline();
            ChannelHandler handler = pipeline.get("packet_handler");

            if (NMSUtils.getNetworkManagerClass().isInstance(handler)) {
                InetSocketAddress address = NMSUtils.getSocketAddress(handler);

                if (address.getAddress() == null) {
                    return ResponseBuilder
                            .inst()
                            .cancel()
                            .notify("")
                            .build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }
    }
}
