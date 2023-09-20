package me.alek.honey.checks.impl;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import me.alek.honey.checks.AbstractPacketListener;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.CheckVisitor;
import me.alek.honey.checks.IPacketListener;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.netty.api.packet.ListenerPriority;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.modelwrappers.WrappedGameProfile;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.login.client.WrappedLoginInLoginStart;
import me.alek.honey.netty.utility.reflect.NMSUtils;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.block.CraftSkull;
import org.bukkit.entity.Player;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class LoginStartListener extends AbstractPacketListener<WrappedLoginInLoginStart> implements IPacketListener {

    @Override
    public boolean onPrePacketReceive(Player player, PacketContainer<WrappedLoginInLoginStart> packetContainer, CheckContextContainer container) {
        return true;
    }

    @Override
    public void onPostPacketReceive(Player player, PacketContainer<WrappedLoginInLoginStart> packetContainer) {

    }

    @Override
    public ListenerPriority getPriority() {
        return ListenerPriority.NORMAL;
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
        public ResponseContainer check(Player player, WrappedLoginInLoginStart packet, CheckContextContainer container) {
            WrappedGameProfile profile = packet.getProfile();

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.getName().equals(profile.getName())) {

                    NMSUtils.getChannel(player).close();
                    return ResponseBuilder.inst().cancel().build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getCheckName() {
            return "Login.A";
        }

        @Override
        public String getDescription() {
            return "Player name already online";
        }
    }

    public static class LoginB implements PacketCheck<WrappedLoginInLoginStart> {

        @Override
        public ResponseContainer check(Player player, WrappedLoginInLoginStart packet, CheckContextContainer container) {
            ChannelPipeline pipeline = packet.getContainer().getChannel().pipeline();
            ChannelHandler handler = pipeline.get("packet_handler");

            if (NMSUtils.getNetworkManagerClass().isInstance(handler)) {
                InetSocketAddress address = NMSUtils.getSocketAddress(handler);

                if (address.getAddress() == null) {
                    return ResponseBuilder.inst().cancel().build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getCheckName() {
            return "Login.B";
        }

        @Override
        public String getDescription() {
            return "Login start with invalid ping";
        }
    }

}
