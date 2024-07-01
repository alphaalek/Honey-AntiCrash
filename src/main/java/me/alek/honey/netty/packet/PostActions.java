package me.alek.honey.netty.packet;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.api.PacketProcessor;
import me.alek.honey.netty.handler.HoneyPacketHandler;
import me.alek.honey.netty.injector.MissingHandlerException;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.packetwrappers.handshake.client.WrappedHandshakeInSetProtocol;
import me.alek.honey.util.Callback;
import me.alek.honey.util.reflection.NMS;
import me.alek.honey.packetwrappers.login.client.WrappedLoginInLoginStart;
import me.alek.honey.packetwrappers.play.client.WrappedPlayInCustomPayload;

public class PostActions {

    private static boolean isSetup;

    public static synchronized void setup() {
        if (isSetup) {
            return;
        }
        isSetup = true;

        PacketLibrary packetLibrary = HoneyPlugin.get().getPacketLibrary();
        PacketProcessor packetProcessor = packetLibrary.getPacketProcessor();

        packetProcessor.addPostAction(PacketTypes.Handshake.Client.SET_PROTOCOL, Callback.Action.<PacketContainer<WrappedHandshakeInSetProtocol>>from(packet -> {
            try {
                // to wrap around ProtocolLib channel proxy
                HoneyPlugin.get().getPacketLibrary().getPipelineInjector().injectChannelProxy(packet.getChannel());
            } catch (MissingHandlerException ex) {
                packet.getChannel().close();
            }
        }));

        packetProcessor.addPostAction(PacketTypes.Login.Client.LOGIN_START, Callback.Action.<PacketContainer<WrappedLoginInLoginStart>>from(packet -> {
            String name = packet.getPacket().getName();
            NMS.submitChannelForName(name, packet.getChannel());

            try {
                HoneyPacketHandler packetHandler = HoneyPlugin.get().getPacketLibrary().getPipelineInjector().getPacketHandler(packet.getChannel());
                packetHandler.setPlayerName(name);
            } catch (MissingHandlerException ignored) {
            }
        }));

        packetProcessor.addPostAction(PacketTypes.Login.Server.LOGIN_SUCCESS, Callback.Action.<PacketContainer<? extends WrappedPacket<?>>>from(packet -> {
            try {
                 HoneyPlugin.get().getPacketLibrary().getPipelineInjector().relocatePreparationHandlers(packet.getChannel());
            } catch (MissingHandlerException ex) {
                ex.printStackTrace();
            }
        }));

        packetProcessor.addPostAction(PacketTypes.Play.Client.CUSTOM_PAYLOAD, Callback.Action.<PacketContainer<WrappedPlayInCustomPayload>>from(packet -> {
            packet.getPacket().getBuf().retain();
        }));
    }
}
