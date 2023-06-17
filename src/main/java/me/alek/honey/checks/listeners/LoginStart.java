package me.alek.honey.checks.listeners;

import com.mojang.authlib.GameProfile;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import me.alek.honey.checks.*;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.ContainerCheck;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.login.client.WrappedLoginInLoginStart;
import me.alek.honey.netty.utility.reflect.NMSUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class LoginStart extends AbstractPacketListener<WrappedLoginInLoginStart> implements IPacketListener {

    @Override
    public boolean onPrePacketReceive(Player player, PacketContainer<WrappedLoginInLoginStart> packetContainer, CheckContextContainer container) {
        return true;
    }

    @Override
    public void onPostPacketReceive(Player player, PacketContainer<WrappedLoginInLoginStart> packetContainer) {

    }

    @Override
    public CheckVisitor<WrappedLoginInLoginStart> getVisitor() {
        CheckVisitor<WrappedLoginInLoginStart> visitor = new CheckVisitor<>();
        visitor.visit(new LoginA());
        visitor.visit(new LoginB());
        return visitor;
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Collections.singletonList(PacketType.Login.Client.LOGIN_START);
    }

    public static class LoginA implements PacketCheck<WrappedLoginInLoginStart> {

        @Override
        public ResponseContainer accept(Player player, WrappedLoginInLoginStart packet, CheckContextContainer container) {
            GameProfile profile = packet.getProfile();

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.getName().equals(profile.getName())) {
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

    public static class LoginB implements ContainerCheck<WrappedLoginInLoginStart> {

        @Override
        public ResponseContainer accept(Player player, PacketContainer<WrappedLoginInLoginStart> packet, CheckContextContainer container) {
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

}
