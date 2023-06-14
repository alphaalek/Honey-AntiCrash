package me.alek.honey.checks.impl;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.Check;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.listener.AsyncPacketAdapter;
import me.alek.honey.netty.listener.FuzzyPacketAdapter;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.login.client.WrappedLoginInEncryptionResponse;
import me.alek.honey.netty.packetwrappers.login.client.WrappedLoginInLoginStart;
import me.alek.honey.netty.utility.reflect.NMSUtils;
import me.alek.honey.netty.utility.reflect.Reflection;
import me.alek.honey.services.ViolationService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class NullPing {

    private static final ViolationService VIOLATION_SERVICE = HoneyPlugin.get().getViolationService();

    private static boolean isNullPing(Channel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        ChannelHandler handler = pipeline.get("packet_handler");
        if (NMSUtils.getNetworkManagerClass().isInstance(handler)) {
            InetSocketAddress address = NMSUtils.getSocketAddress(handler);
            return address.getAddress() == null;
        }
        return false;
    }

    // offline mode
    public static class NullPingLogin extends AsyncPacketAdapter<WrappedLoginInLoginStart> implements Check {

        public NullPingLogin() {}

        @Override
        public void onPacketReceive(Player player, PacketContainer<WrappedLoginInLoginStart> packetContainer) {
            if (isNullPing(packetContainer.getChannel())) {
                VIOLATION_SERVICE.increment(player, packetContainer.getPacket(), this);
                packetContainer.cancel();
            }
        }

        @Override
        public Supplier<List<PacketTypeEnum>> getPacketTypes() {
            return () -> Collections.singletonList(PacketType.Login.Client.LOGIN_START);
        }

        @Override
        public String getName() {
            return "Null Ping";
        }
    }

    // online mode
    public static class NullPingEncryption extends AsyncPacketAdapter<WrappedLoginInEncryptionResponse> implements Check {

        public NullPingEncryption() {}

        @Override
        public void onPacketReceive(Player player, PacketContainer<WrappedLoginInEncryptionResponse> packetContainer) {
            if (isNullPing(packetContainer.getChannel())) {
                VIOLATION_SERVICE.increment(player, packetContainer.getPacket(), this);
                packetContainer.cancel();
            }
        }

        @Override
        public Supplier<List<PacketTypeEnum>> getPacketTypes() {
            return () -> Collections.singletonList(PacketType.Login.Client.ENCRYPTION_RESPONSE);
        }

        @Override
        public String getName() {
            return "Null Ping";
        }
    }
}
