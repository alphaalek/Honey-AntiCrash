package me.alek.honey.netty.packet;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.handler.HoneyPacketHandler;
import me.alek.honey.netty.injector.ChannelCloseInspection;
import me.alek.honey.netty.injector.MissingHandlerException;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.impl.handshake.WrappedHandshakeInSetProtocol;
import me.alek.honey.netty.packetwrappers.impl.login.WrappedLoginInLoginStart;
import me.alek.honey.netty.processor.PacketProcessor;
import me.alek.honey.util.Callback;
import me.alek.honey.util.reflection.NMS;

public class PostActions {

    private static boolean hasCalledSetup;

    public static synchronized void setup() {
        if (hasCalledSetup) {
            return;
        }
        hasCalledSetup = true;

        PacketProcessor packetProcessor = HoneyPlugin.get().getPacketLibrary().getPacketProcessor();

        packetProcessor.addPostAction(PacketTypes.Handshake.Client.SET_PROTOCOL, Callback.Action.<PacketContainer<WrappedHandshakeInSetProtocol>>from(packet -> {
            try {
                // to wrap around ProtocolLib channel proxy
                HoneyPlugin.get().getPacketLibrary().getPipelineInjector().injectChannelProxy(packet.getChannel());
            } catch (MissingHandlerException ex) {
                ChannelCloseInspection.close(packet.getChannel());
            }

            packet.getPacketHandler().handleViaClientboundInterceptor();
        }));

        packetProcessor.addPostAction(PacketTypes.Login.Server.SET_COMPRESSION, Callback.Action.<PacketContainer<?>>from(packet -> {
            packet.addServerCallbackAction(Callback.Action.from(__ -> HoneyPlugin.get().getPacketLibrary().getPipelineInjector().ensureCorrectPipelineOrder(packet.getChannel())));
            packet.getPacketHandler().hasCompression();
        }));

        packetProcessor.addPostAction(PacketTypes.Login.Client.HELLO, Callback.Action.<PacketContainer<WrappedLoginInLoginStart>>from(packet -> {
            String name = packet.getAnyWrapped().getUsername();
            NMS.submitChannelForName(name, packet.getChannel());

            packet.getPacketHandler().setPlayerName(name);
        }));

        packetProcessor.addPostAction(PacketTypes.Login.Server.LOGIN_SUCCESS, Callback.Action.<PacketContainer<?>>from(packet -> {
            try {
                HoneyPlugin.get().getPacketLibrary().getPipelineInjector().relocatePreparationHandlers(packet.getChannel());
            } catch (MissingHandlerException ex) {
                ChannelCloseInspection.close(packet.getChannel());
            }
        }));
    }
}
