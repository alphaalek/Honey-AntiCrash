package me.alek.honey.netty.packet;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.util.ConstructorInvoker;
import me.alek.honey.netty.util.NMS;
import me.alek.honey.netty.util.NetProtocol;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.util.CallbackAction;

public class PacketFactory {

    public static Object createRawPacket(PacketTypeEnum type) {
        if (NetProtocol.getProtocol().isNewerThan(NetProtocol.v1_16_5)) {
            throw new UnsupportedOperationException();
        }

        try {
            return type.getNmsClass().newInstance();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public static Object createRawPacket(PacketTypeEnum type, ByteBuf serializer) {
        if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_16_5)) {
            return createRawPacket(type);
        }

        try {
            return new ConstructorInvoker<>(type.getNmsClass(), NMS.getPacketDataSerializerClass())
                    .invoke(new PacketDataSerializer(serializer).getSerializer());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public static synchronized <WP extends WrappedPacket<WP>> WrappedPacket<WP> createNewPacket(PacketTypeEnum type, HoneyPlayer player) {
        return createNewPacket(type, null, player);
    }

    public static synchronized <WP extends WrappedPacket<WP>> WrappedPacket<WP> createNewPacket(PacketTypeEnum type, ByteBuf serializer, HoneyPlayer player) {
        return createNewPacket(type, player, serializer, player.channel, null);
    }

    public static synchronized <WP extends WrappedPacket<WP>> WrappedPacket<WP> createNewPacket(PacketTypeEnum type, HoneyPlayer player, ByteBuf serializer, Channel channel, CallbackAction<PacketContainer<WP>> postAction) {
        try {
            Object rawPacket;
            if (serializer == null) {
                rawPacket = createRawPacket(type);
            } else {
                rawPacket = createRawPacket(type, serializer);
            }

            return createWrappedPacket(type, rawPacket, player, channel, postAction);
        } catch (Exception ex) {
            HoneyPlugin.get().getLoggerService().getLogger().severe("Error occurred when creating packet for type " + type);
            ex.printStackTrace();
        }

        return null;
    }

    public static synchronized <WP extends WrappedPacket<WP>> WrappedPacket<WP> createWrappedPacket(PacketTypeEnum type, Object rawPacket, HoneyPlayer player, Channel channel, CallbackAction<PacketContainer<WP>> postAction) {
        return PacketWrapperFactory.initWrappedPacket(type, rawPacket, new InternalPacketContainer<>(rawPacket, player, channel, postAction, type));
    }

    public static synchronized <WP extends WrappedPacket<WP>> WrappedPacket<WP> createWrappedPacket(PacketTypeEnum type, Object rawPacket, PacketContainer<WP> packetContainer) {
        return PacketWrapperFactory.initWrappedPacket(type, rawPacket, packetContainer);
    }
}
