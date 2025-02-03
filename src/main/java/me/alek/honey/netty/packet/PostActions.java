package me.alek.honey.netty.packet;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.ChannelCloseInspection;
import me.alek.honey.netty.injector.MissingHandlerException;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.impl.handshake.WrappedHandshakeInSetProtocol;
import me.alek.honey.netty.processor.PacketProcessor;
import me.alek.honey.util.Callback;

public class PostActions {

    private static boolean hasCalledSetup;

    public static synchronized void setup() {
        if (hasCalledSetup) {
            return;
        }
        hasCalledSetup = true;

        PacketProcessor packetProcessor = HoneyPlugin.get().getPacketLibrary().getPacketProcessor();

        packetProcessor.addPostAction(PacketTypes.Handshake.Client.SET_PROTOCOL, Callback.Action.<PacketContainer<WrappedHandshakeInSetProtocol>>from(packet -> {
            packet.getPlayer().phase = HoneyPlayer.Phase.LOGGING_IN;
            packet.getPacketHandler().handleViaClientboundInterceptor();
        }));

        packetProcessor.addPostAction(PacketTypes.Login.Server.SET_COMPRESSION, Callback.Action.<PacketContainer<?>>from(packet -> {
            packet.addServerCallbackAction(Callback.Action.from(__ -> HoneyPlugin.get().getPacketLibrary().getPipelineInjector().ensureCorrectPipelineOrder(packet.getChannel())));
            packet.getPacketHandler().hasCompression();
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
