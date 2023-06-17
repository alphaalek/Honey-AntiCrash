package me.alek.honey.checks.impl;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.types.ContainerCheck;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.login.client.WrappedLoginInEncryptionResponse;
import me.alek.honey.netty.utility.reflect.NMSUtils;
import org.bukkit.entity.Player;

import java.net.InetSocketAddress;

public class EncryptionA implements ContainerCheck<WrappedLoginInEncryptionResponse> {

    @Override
    public ResponseContainer accept(Player player, PacketContainer<WrappedLoginInEncryptionResponse> packet, CheckContextContainer container) {
        ChannelPipeline pipeline = packet.getChannel().pipeline();
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
