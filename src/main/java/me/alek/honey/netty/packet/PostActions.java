package me.alek.honey.netty.packet;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.modelwrappers.WrappedGameProfile;
import me.alek.honey.netty.modelwrappers.network.WrappedPacketDataSerializer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.utility.NMS;
import me.alek.honey.netty.utility.NetProtocol;

public class PostActions {

    private static boolean isSetup;

    public static synchronized void setup() {
        if (isSetup) {
            return;
        }
        isSetup = true;

        HoneyPlugin.get().getPacketLibrary().getPacketProcessor().addPostAction(PacketType.Login.Server.LOGIN_SUCCESS, (packet) -> {
            HoneyPlugin.get().getPacketLibrary().getPacketManager().getFilter(packet.getChannel()).checkRelocation();
        });

        HoneyPlugin.get().getPacketLibrary().getPacketProcessor().addPostAction(PacketType.Login.Client.LOGIN_START, (packet) -> {
            ReflectStructure<?, ?> loginStartPacketReflectStructure = ReflectStructureCache.acquireStructure(packet.getHandle().getClass());
            String name;
            if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_18_2)) {
                IStructureModifier<WrappedGameProfile> gameProfileStructureModifier = loginStartPacketReflectStructure
                        .withType(NMS.getGameProfileClass(), Converters.getGameProfileConverter())
                        .withTarget(packet.getHandle());
                WrappedGameProfile gameProfile = gameProfileStructureModifier.readField(0);
                name = gameProfile.getName();
            } else {
                IStructureModifier<String> stringStructureModifier = loginStartPacketReflectStructure
                        .asString().withTarget(packet.getHandle());
                name = stringStructureModifier.readField(0);
            }
            NMS.submitChannelForName(name, packet.getChannel());
        });

        HoneyPlugin.get().getPacketLibrary().getPacketProcessor().addPostAction(PacketType.Play.Client.CUSTOM_PAYLOAD, (packet) -> {
            if (packet.getType() == PacketType.Play.Client.CUSTOM_PAYLOAD) {
                WrappedPacketDataSerializer serializer = packet.getDataSerializers().readField(0);

                if (serializer.refCnt() > 0) {
                    serializer.retain();
                }
            }
        });
    }
}
