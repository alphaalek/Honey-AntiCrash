package me.alek.honey.netty.utility.reflect;

import me.alek.honey.netty.utility.protocol.Protocol;
import org.bukkit.Bukkit;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Reflection {

    static {
        String OBC = Bukkit.getServer().getClass().getPackage().getName();
        String NMS;

        boolean newProtocol = Protocol.getProtocol().isNewerThanOrEqual(Protocol.v1_17);
        if (newProtocol) {
            PROTOCOL_STATES = new HashMap<String, String>(){{
                put("PacketHandshaking", "handshake");
                put("PacketStatus", "status");
                put("PacketPlay", "game");
                put("Clientbound", "game");
                put("PacketLogin", "login");
            }};
            NMS = "net.minecraft";
        }
        else {
            NMS = OBC.replace("org.bukkit.craftbukkit", "net.minecraft.server");
            PROTOCOL_STATES = null;
        }
        USE_NEW_NMS_PROTOCOL = newProtocol;

        OBC_PREFIX = OBC;
        NMS_PREFIX = NMS;
    }

    private static final boolean USE_NEW_NMS_PROTOCOL;
    private static final String OBC_PREFIX;
    private static final String NMS_PREFIX;
    private static final String VERSION = OBC_PREFIX.replace(
            "org.bukkit.craftbukkit", "").replace(".", "");
    private static final Pattern MATCH_VARIABLE = Pattern.compile("\\{([^\\}]+)\\}");
    private static final HashMap<String, String> PROTOCOL_STATES;

    public static Class<?> getSubClass(Class<?> superClass, String name) {
        if (superClass == null) {
            throw new RuntimeException("Reflection fejl " + name);
        }
        for (Class<?> subClass : superClass.getDeclaredClasses()) {
            if (subClass.getSimpleName().equals(name)) {
                return subClass;
            }
        }
        throw new RuntimeException("Reflection fejl (subclass): " + superClass.getName() + ", " + name);
    }

    public static Class<Object> getSubClass(String superClass, String name) {
        return (Class<Object>) getSubClass(getClass(superClass), name);
    }

    public static Class<?> getClassWithException(String name) throws Exception {
        return getCanonicalClassWithException(setPlaceholders(name));
    }

    public static Class<Object> getObjectClassWithException(String name) throws Exception {
        return (Class<Object>) getClassWithException(name);
    }

    public static Class<?> getClass(String name) {
        return getCanonicalClass(setPlaceholders(name));
    }

    public static Class<Object> getObjectClass(String name) {
        return (Class<Object>) getClass(name);
    }

    public static Class<?> getArrayClass(String name) {
        return getClass("[L" + name + ";");
    }

    public static Class<Object[]> getArrayObjectClass(String name) {
        return (Class<Object[]>) getClass("[" + name + ";");
    }

    public static Class<?> getFuzzyClass(String... names) {
        for (String name : names) {
            try {
                return getClassWithException(name);
            } catch (Exception ex) {
            }
        }
        throw new RuntimeException("Reflection fejl (class): " + Arrays.toString(names));
    }

    public static Class<Object> getFuzzyObjectClass(String... names) {
        return (Class<Object>) getFuzzyClass(names);
    }

    public static Class<?> getCanonicalClassWithException(String name) throws Exception {
        return Class.forName(name);
    }

    public static Class<?> getCanonicalClass(String name) {
        try {
            return getCanonicalClassWithException(name);
        } catch (Exception ex) {
            throw new RuntimeException("Reflection fejl " + name);
        }
    }

    public static String setPlaceholders(String name) {
        StringBuffer output = new StringBuffer();
        Matcher matcher = MATCH_VARIABLE.matcher(name);
        while (matcher.find()) {
            String variable = matcher.group(1);

            String replacement = "";
            if ("nms".equalsIgnoreCase(variable)) {
                if (USE_NEW_NMS_PROTOCOL) {
                    for (Map.Entry<String, String> stateEntry : PROTOCOL_STATES.entrySet()) {

                        if (name.contains(stateEntry.getKey())) {
                            replacement = NMS_PREFIX + ".network.protocol." + stateEntry.getValue();
                            break;
                        }
                    }
                }
                if (replacement.equals("")) {
                    replacement = NMS_PREFIX;
                }
            }
            else if ("obc".equalsIgnoreCase(variable)) {
                replacement = OBC_PREFIX;
            }
            else if ("version".equalsIgnoreCase(variable)) {
                replacement = VERSION;
            }
            else {
                throw new IllegalArgumentException("Ugyldig placeholder: "
                        + variable);
            }
            if (replacement.length() > 0 && matcher.end() < name.length()
                    && name.charAt(matcher.end()) != '.')
                replacement += ".";
            matcher.appendReplacement(output,
                    Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(output);
        return output.toString();
    }

    public static ConstructorInvoker<Object> getConstructor(Class<?> target, Class<?>... parameters) {
        try {
            return (ConstructorInvoker<Object>) new ConstructorInvoker<>(target.getConstructor(parameters));
        } catch (Exception ex) {
            throw new RuntimeException("Reflection fejl (constructor): " + target.getName() + ", " + Arrays.toString(parameters));
        }
    }

    public static MethodInvoker<Object> getMethod(Method method) {
        return new MethodInvoker<>(method);
    }

    public static MethodInvoker<Object> getMethod(String target, String name, Class<?>... parameters) {
        return getMethod(getClass(target), name, parameters);
    }

    public static MethodInvoker<Object> getMethodFromParameters(Class<?> target, int index, Class<?>... parameters) {
        int currentIndex = -1;

        for (Method method : target.getDeclaredMethods()) {
            if (Arrays.equals(method.getParameterTypes(), parameters)) {

                if (currentIndex++ == index) {
                    return new MethodInvoker<>(method);
                }
            }
        }
        if (target.getSuperclass() != null) {
            return getMethodFromParameters(target.getSuperclass(), index, parameters);
        }
        throw new RuntimeException("Reflection fejl (method): " + target.getName() + ", " + index + ", " + Arrays.toString(parameters));
    }

    public static MethodInvoker<Object> getMethodFromReturnType(Class<?> target, int index, Class<?> returnType) {
        int currentIndex = 0;
        for (Method method : target.getDeclaredMethods()) {

            if (method.getReturnType() == returnType) {
                if (currentIndex == index) {
                    return new MethodInvoker<>(method);
                }
                currentIndex++;
            }
        }
        if (target.getSuperclass() != null) {
            return getMethodFromReturnType(target.getSuperclass(), index, returnType);
        }
        throw new RuntimeException("Reflection fejl (method):" + index + " " + target.getName() + ", " + returnType + ", " + returnType);
    }

    public static MethodInvoker<Object> getMethod(Class<?> target, String name, Class<?>... parameters) {
        for (Method method : target.getDeclaredMethods()) {
            if (!method.getName().equals(name) || method.getParameterTypes() == parameters) {
                continue;
            }
            return new MethodInvoker<>(method);
        }
        if (target.getSuperclass() != null) {
            return getMethod(target.getSuperclass(), name, parameters);
        }
        throw new RuntimeException("Reflection fejl (method): " + target.getName() + ", " + name + ", " + Arrays.toString(parameters));
    }

    public static <T> FieldAccessor<T> getNullableField(Class<?> target, String name, Class<?> type) {
        for (Field field : target.getDeclaredFields()) {
            if (!field.getName().equals(name) || field.getType() != type) {
                continue;
            }
            return new FieldAccessor<>(field);
        }
        return null;
    }

    public static <T> FieldAccessor<T> getNullableField(Class<?> target, int index, Class<?> type) {
        int currentIndex = 0;
        for (Field field : target.getDeclaredFields()) {

            if (field.getType() == type) {
                if (currentIndex++ == index) {
                    return new FieldAccessor<>(field);
                }
            }
        }
        return null;
    }

    public static <T> FieldAccessor<T> getNullableFieldAssignableFrom(Class<?> target, String name, Class<?> type) {
        for (Field field : target.getDeclaredFields()) {
            if (!field.getName().equals(name) || type.isAssignableFrom(field.getType())) {
                continue;
            }
            return new FieldAccessor<>(field);
        }
        return null;
    }

    public static <T> FieldAccessor<T> getNullableFieldAssignableFrom(Class<?> target, int index, Class<?> type) {
        int currentIndex = 0;
        for (Field field : target.getDeclaredFields()) {

            if (type.isAssignableFrom(field.getType())) {
                if (currentIndex++ == index) {
                    return new FieldAccessor<>(field);
                }
            }
        }
        return null;
    }

    public static <T> FieldAccessor<T> getField(String target, String name, Class<?> type) {
        return getField(getClass(target), name, type);
    }

    public static <T> FieldAccessor<T> getField(Class<?> target, String name, Class<?>... types) {
        for (Class<?> type : types) {
            FieldAccessor<T> accessor = getNullableField(target, name, type);

            if (accessor != null) return accessor;
        }
        throw new RuntimeException("Reflection fejl (field): " + target.getName() + ", " + name + ", " + Arrays.toString(types));
    }

    public static <T> FieldAccessor<T> getField(Class<?> target, int index, Class<?>... types) {
        for (Class<?> type : types) {
            FieldAccessor<T> accessor = getNullableField(target, index, type);

            if (accessor != null) return accessor;
        }
        throw new RuntimeException("Reflection fejl (field): " + target.getName() + ", " + index + ", " + Arrays.toString(types));
    }

    public static <T> FieldAccessor<T> getFieldAssignableFrom(Class<?> target, String name, Class<?>... types) {
        for (Class<?> type : types) {
            FieldAccessor<T> accessor = getNullableFieldAssignableFrom(target, name, type);

            if (accessor != null) return accessor;
        }
        throw new RuntimeException("Reflection fejl (assignable type field): " + target.getName() + ", " + name + ", " + Arrays.toString(types));
    }

    public static <T> FieldAccessor<T> getFieldAssignableFrom(Class<?> target, int index, Class<?>... types) {
        for (Class<?> type : types) {
            FieldAccessor<T> accessor = getNullableFieldAssignableFrom(target, index, type);

            if (accessor != null) return accessor;
        }
        throw new RuntimeException("Reflection fejl (assignable type field): " + target.getName() + ", " + index + ", " + Arrays.toString(types));
    }

    public static <T> FieldAccessor<T> getParameterizedField(Class<?> target, Class<?> bound, Class<?>... generic) {
        for (Field field : target.getDeclaredFields()) {
            Type fieldType = field.getGenericType();

            if (fieldType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) fieldType;

                if (Arrays.equals(generic, parameterizedType.getActualTypeArguments())) {
                    return new FieldAccessor<>(field);
                }
            }
        }
        throw new RuntimeException("Reflection fejl (parameterized field):" + target.getName() + ", " + bound + ", " + Arrays.toString(generic));
    }

    public static Enum<?> getEnumAtIndex(Class<Enum<?>> enumClass, int ordinal) {
        for (Enum<?> enumValue : enumClass.getEnumConstants()) {
            if (enumValue.ordinal() ==ordinal) {
                return enumValue;
            }
        }
        throw new RuntimeException("Reflection fejl (enum): " + enumClass.getName() + ", " + ordinal);
    }

    public static Enum<?> getEnumForName(Class<Enum<?>> enumClass, String name) {
        for (Enum<?> enumValue : enumClass.getEnumConstants()) {
            if (enumValue.name().equals(name)) {
                return enumValue;
            }
        }
        throw new RuntimeException("Reflection fejl (enum): " + enumClass.getName() + ", " + name);
    }

    public static String getVersion() {
        return VERSION;
    }

    public static String getNMSPrefix() {
        return NMS_PREFIX;
    }

    public static String getOBCPrefix() {
        return OBC_PREFIX;
    }
}
