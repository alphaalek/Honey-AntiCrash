package me.alek.honey.checks.impl;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.checks.AbstractPacketListener;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.CheckPipeline;
import me.alek.honey.checks.IPacketListener;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.response.Validation;
import me.alek.honey.checks.types.PacketValidation;
import me.alek.honey.lang.Lang;
import me.alek.honey.netty.modelwrappers.WrappedGameProfile;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.util.NMS;
import me.alek.honey.packetwrappers.login.client.WrappedLoginInLoginStart;
import me.alek.honey.util.IDRegistry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class LoginValidation extends AbstractPacketListener<WrappedLoginInLoginStart> implements IPacketListener {

    @Override
    public boolean requiresPlayer() {
        return false;
    }

    @Override
    public CheckPipeline<PacketValidation<WrappedLoginInLoginStart>> getPipeline(HoneyPlayer player) {
        return CheckPipeline.from(player,
                new LoginA(), new LoginB());
    }

    @Override
    public Supplier<List<PacketType>> getPacketTypes() {
        return () -> Collections.singletonList(PacketTypes.Login.Client.LOGIN_START);
    }

    public static class LoginA implements PacketValidation<WrappedLoginInLoginStart> {

        private static final int id = IDRegistry.CHECKS.acquire();

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
            return id;
        }

        @Override
        public String getName() {
            return "Login.A";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.login.a");
        }
    }

    public static class LoginB implements PacketValidation<WrappedLoginInLoginStart> {

        private static final int id = IDRegistry.CHECKS.acquire();

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
            return id;
        }

        @Override
        public String getName() {
            return "Login.B";
        }

        @Override
        public String getDescription() {
            return Lang.get("validation.login.b");
        }
    }
}
