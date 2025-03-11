package me.alek.honey.netty.packet;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
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
            packet.getPacketHandler().setCompressionEnabled();
        }));

        packetProcessor.addPostAction(PacketTypes.Login.Server.LOGIN_SUCCESS, Callback.Action.<PacketContainer<?>>from(packet -> {
            HoneyPlugin.get().getPacketLibrary().getPipelineInjector().ensureCorrectPipelineOrder(packet.getChannel());
        }));
    }
}
