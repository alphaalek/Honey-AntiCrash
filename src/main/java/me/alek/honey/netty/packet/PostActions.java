package me.alek.honey.netty.packet;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.api.PacketProcessor;
import me.alek.honey.netty.handler.HoneyPacketHandler;
import me.alek.honey.netty.injector.MissingHandlerException;
import me.alek.honey.netty.packet.type.PacketTypes;
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

        packetProcessor.<WrappedLoginInLoginStart>addPostAction(PacketTypes.Login.Client.LOGIN_START, packet -> {
            String name = packet.getPacket().getName();
            NMS.submitChannelForName(name, packet.getChannel());

            try {
                HoneyPacketHandler packetHandler = HoneyPlugin.get().getPacketLibrary().getPipelineInjector().getPacketHandler(packet.getChannel());
                packetHandler.setPlayerName(name);
            } catch (MissingHandlerException ignored) {
            }
        });

        packetProcessor.addPostAction(PacketTypes.Login.Server.LOGIN_SUCCESS, packet -> {
            try {
                 HoneyPlugin.get().getPacketLibrary().getPipelineInjector().relocatePreparationHandlers(packet.getChannel());
            } catch (MissingHandlerException ex) {
                ex.printStackTrace();
            }
        });

        packetProcessor.<WrappedPlayInCustomPayload>addPostAction(PacketTypes.Play.Client.CUSTOM_PAYLOAD, packet -> {
            packet.getPacket().getBuf().retain();
        });
    }
}
