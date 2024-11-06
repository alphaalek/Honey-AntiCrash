package me.alek.honey.netty.packetwrappers;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.protocol.NetProtocolRange;
import me.alek.honey.util.reflection.Reflection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ByteWrappedPacket<WP extends WrappedPacket<WP>> implements WrappedPacket<WP> {

    private final PacketContainer<WP> packet;
    private final PacketDataSerializer buf;
    private final int byteSize;
    private final int accumulatedByteSize;
    private final int doubleByteSequences;

    public ByteWrappedPacket(PacketDataSerializer buf, PacketContainer<WP> packet) {
        this.buf = buf;
        this.packet = packet;
        this.byteSize = buf.readableBytes();

        if (packet.getInitialByteWrapped() == null) {
            int start = buf.readerIndex(), accumulated = 0, prevByte = 0, doubleSequences = 0;
            while (buf.isReadable()) {
                byte b = buf.readByte();
                accumulated += Math.abs(b) + 8;
                if (b == prevByte) {
                    doubleSequences++;
                }
                prevByte = b;
            }
            buf.readerIndex(start);
            this.accumulatedByteSize = accumulated;
            this.doubleByteSequences = doubleSequences;
        } else {
            this.accumulatedByteSize = packet.getInitialByteWrapped().getAccumulatedByteSize();
            this.doubleByteSequences = packet.getInitialByteWrapped().getDoubleByteSequences();
        }
    }

    @Override
    public boolean isByteBased() {
        return true;
    }

    public PacketDataSerializer getBuf() {
        return this.buf;
    }

    public int getByteSize() {
        return this.byteSize;
    }

    public int getAccumulatedByteSize() {
        return this.accumulatedByteSize;
    }

    public int getDoubleByteSequences() {
        return this.doubleByteSequences;
    }

    @Override
    public HoneyPlayer getPlayer() {
        return this.packet.getPlayer();
    }

    @Override
    public PacketContainer<WP> getContainer() {
        return this.packet;
    }

    @Override
    public PacketType getType() {
        return this.packet.getType();
    }

    @Override
    public Map<String, Object> getProperties() {
        if (getClass() != ByteWrappedPacket.class) {
            Map<String, Object> properties = new HashMap<>();

            for (Field field : getClass().getDeclaredFields()) {

                if (field.isAnnotationPresent(Property.class)) {
                    Property property = field.getAnnotation(Property.class);
                    if (NetProtocolRange.matchProtocol(property.to(), property.from())) {

                        Object val;
                        if (field.isAnnotationPresent(Property.Unlock.class)) {
                            String methodSupplierName = field.getAnnotation(Property.Unlock.class).methodSupplier();
                            val = Reflection.getMethod(getClass(), methodSupplierName).invoke(this);
                        } else {
                            val = Reflection.getField(field).get(this);
                        }

                        properties.put(field.getName(), val);
                    }
                }
            }

            return properties;
        }

        return Collections.emptyMap();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Property {

        NetProtocol.To to() default @NetProtocol.To(to = NetProtocol.LATEST);

        NetProtocol.From from() default @NetProtocol.From(from = NetProtocol.OLDEST);

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.FIELD)
        @interface Unlock {

            String methodSupplier();
        }
    }
}
