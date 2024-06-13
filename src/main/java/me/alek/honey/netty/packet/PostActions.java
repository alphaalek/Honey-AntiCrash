package me.alek.honey.netty.packet;

import io.netty.buffer.ByteBuf;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.api.PacketProcessor;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.handler.HoneyPacketHandler;
import me.alek.honey.netty.injector.MissingHandlerException;
import me.alek.honey.netty.modelwrappers.WrappedGameProfile;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.util.NMS;
import me.alek.honey.netty.util.NetProtocol;
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

        packetProcessor.addPostAction(PacketTypes.Login.Client.LOGIN_START, packet -> {
            String name = getNameFromLoginStart((PacketContainer<WrappedLoginInLoginStart>) packet);
            NMS.submitChannelForName(name, packet.getChannel());

            try {
                HoneyPacketHandler packetHandler = HoneyPlugin.get().getPacketLibrary().getPipelineInjector().getPacketHandler(packet.getChannel());
                packetHandler.setPlayerName(name);
            } catch (MissingHandlerException ignored) {
            }
        });

        packetProcessor.addPostAction(PacketTypes.Login.Server.LOGIN_SUCCESS, packet -> {
            try {
                 HoneyPlugin.get().getPacketLibrary().getPipelineInjector().relocatePreparationHandlers(packet.getPlayer(), packet.getChannel());
            } catch (MissingHandlerException ex) {
                ex.printStackTrace();
            }
        });

        packetProcessor.addPostAction(PacketTypes.Login.Server.SET_COMPRESSION, packet -> {
            HoneyPlugin.get().getPacketLibrary().getPipelineInjector().relocateFilter(packet.getChannel());
        });

        HoneyPlugin.get().getPacketLibrary().getPacketProcessor().<WrappedPlayInCustomPayload>addPostAction(PacketTypes.Play.Client.CUSTOM_PAYLOAD, packet -> {
            ByteBuf serializer = packet.getPacket().getBuf();

            if (serializer.refCnt() > 0) {
                serializer.retain();
            } else {
                serializer.release();
            }
        });
    }

    public static String getNameFromLoginStart(PacketContainer<WrappedLoginInLoginStart> packet) {
        ReflectStructure<?, ?> loginStartPacketReflectStructure = ReflectStructureCache.acquireStructure(packet.getHandle().getClass());
        if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_18_2)) {
            IStructureModifier<WrappedGameProfile> gameProfileStructureModifier = loginStartPacketReflectStructure
                    .withType(NMS.getGameProfileClass(), Converters.getGameProfileConverter())
                    .withTarget(packet.getHandle());
            WrappedGameProfile gameProfile = gameProfileStructureModifier.readField(0);
            return gameProfile.getName();
        } else {
            IStructureModifier<String> stringStructureModifier = loginStartPacketReflectStructure
                    .withStrings().withTarget(packet.getHandle());
            return stringStructureModifier.readField(0);
        }
    }
}
