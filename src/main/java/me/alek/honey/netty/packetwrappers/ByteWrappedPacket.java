package me.alek.honey.netty.packetwrappers;

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

public interface ByteWrappedPacket<WP extends WrappedPacket<WP>> extends WrappedPacket<WP> {

    @Override
    default boolean isByteBased() {
        return true;
    }

    @Override
    default Map<String, Object> getProperties() {
        Map<String, Object> properties = new HashMap<>();

        Class<?> cls = getClass();
        for (;;) {
            for (Field field : cls.getDeclaredFields()) {
                if (field.isAnnotationPresent(Property.class)) {
                    Property property = field.getAnnotation(Property.class);

                    if (NetProtocolRange.matchProtocol(getProtocol(), property.to(), property.from())) {
                        Object val = Reflection.getField(field).get(this);
                        if (val == null && field.isAnnotationPresent(Property.IgnoreIfNull.class)) {
                            continue;
                        }

                        properties.put(field.getName(), val);
                    }
                }
            }
            cls = cls.getSuperclass();
            if (!ByteWrappedPacket.class.isAssignableFrom(cls)) {
                break;
            }
        }

        return properties;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Property {

        NetProtocol.To to() default @NetProtocol.To(to = NetProtocol.LATEST);

        NetProtocol.From from() default @NetProtocol.From(from = NetProtocol.OLDEST);

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.FIELD)
        @interface IgnoreIfNull {

        }
    }
}
