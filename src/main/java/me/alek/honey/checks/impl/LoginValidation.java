package me.alek.honey.checks.impl;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.checks.*;
import me.alek.honey.checks.response.Validation;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.api.ListenerPriority;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.modelwrappers.WrappedGameProfile;
import me.alek.honey.netty.packet.bundle.PacketBundleFilter;
import me.alek.honey.netty.packet.bundle.PacketBundleFilterFactory;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.packetwrappers.login.client.WrappedLoginInLoginStart;
import me.alek.honey.netty.util.NMS;
import me.alek.honey.util.CallbackAction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class LoginValidation extends AbstractPacketListener<WrappedLoginInLoginStart> implements IPacketListener {

    @Override
    public boolean onPrePacketReceive(HoneyPlayer player, PacketContainer<WrappedLoginInLoginStart> packetContainer, CheckContextContainer container, CallbackAction<ResponseContainer> callback) {
        return true;
    }

    @Override
    public void onPostPacketReceive(HoneyPlayer player, PacketContainer<WrappedLoginInLoginStart> packetContainer, CheckContextContainer container) {
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
    public CheckVisitor<WrappedLoginInLoginStart> getVisitor(HoneyPlayer player) {
        CheckVisitor<WrappedLoginInLoginStart> visitor = new CheckVisitor<>(player);
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
        public ResponseContainer check(HoneyPlayer player, WrappedLoginInLoginStart packet, CheckContextContainer container) {
            WrappedGameProfile profile = packet.getProfile();
            if (profile == null) {
                return Validation.PASS;
            }

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.getName().equals(profile.getName())) {
                    return Validation.inst().cancel().punish().build();
                }
            }

            return Validation.PASS;
        }

        @Override
        public int getID() {
            return 24;
        }

        @Override
        public String getName() {
            return "Login.A";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.login.a");
        }

        @Override
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilterFactory.createEmptyFilter();
        }
    }

    public static class LoginB implements PacketCheck<WrappedLoginInLoginStart> {

        @Override
        public ResponseContainer check(HoneyPlayer player, WrappedLoginInLoginStart packet, CheckContextContainer container) {
            ChannelPipeline pipeline = packet.getContainer().getChannel().pipeline();
            ChannelHandler handler = pipeline.get("packet_handler");

            if (NMS.getNetworkManagerClass().isInstance(handler)) {
                InetSocketAddress address = NMS.getSocketAddress(handler);

                if (address.getAddress() == null) {
                    return Validation.inst().cancel().build();
                }
            }
            return Validation.PASS;
        }

        @Override
        public int getID() {
            return 25;
        }

        @Override
        public String getName() {
            return "Login.B";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.login.b");
        }

        @Override
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilterFactory.createEmptyFilter();
        }
    }
}
