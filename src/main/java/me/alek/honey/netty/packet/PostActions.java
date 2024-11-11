package me.alek.honey.netty.packet;

import io.netty.channel.ChannelHandler;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.api.PacketProcessor;
import me.alek.honey.netty.handler.HoneyPacketHandler;
import me.alek.honey.netty.handler.PreResolvedPacketHandler;
import me.alek.honey.netty.injector.MissingHandlerException;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.packetwrappers.impl.handshake.WrappedHandshakeInSetProtocol;
import me.alek.honey.util.Callback;
import me.alek.honey.util.reflection.NMS;
import me.alek.honey.netty.packetwrappers.impl.login.WrappedLoginInLoginStart;

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

        packetProcessor.addPostAction(PacketTypes.Login.Server.SET_COMPRESSION, Callback.Action.<PacketContainer<?>>from(packet -> {
            // await LOGIN_SUCCESS packet and ensure correct pipeline order for serverbound packet stream afterward
            HoneyPacketHandler.DELAYED_TASKS[PacketBound.CLIENT.ordinal()].get().add(() -> {
                HoneyPlugin.get().getPacketLibrary().getPipelineInjector().ensureCorrectPipelineOrder(packet.getChannel());
            });
        }));

        packetProcessor.addPostAction(PacketTypes.Login.Server.LOGIN_SUCCESS, Callback.Action.<PacketContainer<?>>from(packet -> {
            // check if the pre resolved packet handler is set for this pipeline, because then we can't be sure the pipeline has the correct
            // order, although we called the method in the SET_COMPRESSION post action. This is because the pre resolved handler comes right
            // after the minecraft packet handler in the outbound packet stream, and therefore handlers can have modified the pipeline without
            // us knowing, while the rest of the pipeline is processing the SET_COMPRESSION packet
            ChannelHandler handler = packet.getChannel().pipeline().get("pre-resolved-packet-handler");
            if (handler != null) {
                PreResolvedPacketHandler preResolvedPacketHandler = (PreResolvedPacketHandler) handler;
                if (preResolvedPacketHandler.write) {
                    HoneyPlugin.get().getPacketLibrary().getPipelineInjector().ensureCorrectPipelineOrder(packet.getChannel());
                }
            }
        }));

        packetProcessor.addPostAction(PacketTypes.Login.Client.LOGIN_START, Callback.Action.<PacketContainer<WrappedLoginInLoginStart>>from(packet -> {
            String name = packet.getAnyWrapped().getName();
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
                packet.getChannel().close();
            }
        }));
    }
}
