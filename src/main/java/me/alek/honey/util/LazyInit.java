package me.alek.honey.util;

import me.alek.honey.netty.codec.CodecFactory;
import me.alek.honey.netty.codec.WrappedEnumProtocol;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.util.reflection.NMS;
import me.alek.honey.util.protocol.NetProtocol;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.function.Supplier;

public class LazyInit<T> {

    private T val;
    private final Supplier<T> function;

    public LazyInit(Supplier<T> function) {
        this.function = function;
    }

    public T get() {
        return val == null ? (val = function.get()) : val;
    }

    public static LazyInit<Integer> getMinecraftIDInit(PacketType type) {
        return new LazyInit<>(() -> {
            if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_20_5)) {
                if (type.getKeyName1_20_5() == null) {
                    throw new IllegalStateException("Modern protocol for packet mapping without minecraft key");
                }

                Object streamCodec = CodecFactory.getStreamCodec1_20_5(type.getBound(), type.getState());

                List<Object> codecs = ReflectStructureCache.acquireStructure(streamCodec.getClass())
                        .<List<Object>>withWeakType(List.class).withTarget(streamCodec)
                        .readField(0);
                int i = 0;
                for (Object codec : codecs) {
                    Object packetType = ReflectStructureCache.acquireStructure(codec.getClass())
                            .withTarget(codec)
                            .readField(null, (in, f) -> !NMS.getStreamCodecClass().isAssignableFrom(f.getHandle().getType()));
                    Object minecraftKey = ReflectStructureCache.acquireStructure(NMS.getPacketTypeClass())
                            .withType(NMS.getMinecraftKeyClass()).withTarget(packetType)
                            .readField(0);

                    PacketType typeFromCodec = PacketTypes.getTypeFromMinecraftKey(type.getState(), type.getBound(), minecraftKey);
                    if (typeFromCodec.equals(type)) {
                        return i;
                    }
                    i++;
                }
            } else {
                WrappedEnumProtocol enumProtocol = WrappedEnumProtocol.getEnumProtocolDelegate(type.getBound(), type.getState());

                return enumProtocol.getPacketId(type.getNmsClass());
            }
            return -1;
        });
    }
}
