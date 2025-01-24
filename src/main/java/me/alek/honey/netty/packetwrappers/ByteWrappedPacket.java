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
        if (getClass() != ByteInWrappedPacket.class) {
            Map<String, Object> properties = new HashMap<>();
            for (Field field : getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(Property.class)) {
                    Property property = field.getAnnotation(Property.class);

                    if (NetProtocolRange.matchProtocol(getProtocol(), property.to(), property.from())) {
                        Object val;
                        if (field.isAnnotationPresent(Property.Unlock.class)) {
                            String methodSupplierName = field.getAnnotation(Property.Unlock.class).methodSupplier();
                            val = Reflection.getMethod(getClass(), methodSupplierName).invoke(this);
                        } else {
                            val = Reflection.getField(field).get(this);
                        }

                        if (val == null && field.isAnnotationPresent(Property.IgnoreIfNull.class)) {
                            continue;
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
    @interface Property {

        NetProtocol.To to() default @NetProtocol.To(to = NetProtocol.LATEST);

        NetProtocol.From from() default @NetProtocol.From(from = NetProtocol.OLDEST);

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.FIELD)
        @interface Unlock {

            String methodSupplier();
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.FIELD)
        @interface IgnoreIfNull {

        }
    }
}
