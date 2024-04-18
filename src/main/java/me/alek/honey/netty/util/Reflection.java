package me.alek.honey.netty.util;

import org.bukkit.Bukkit;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Reflection {

    private static final Map<String, Class<?>> CLASS_CACHE = new HashMap<>();

    static {
        String OBC = Bukkit.getServer().getClass().getPackage().getName();
        String NMS;

        boolean newProtocol = NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_17);
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
            throw new RuntimeException("Reflection error " + name);
        }
        for (Class<?> subClass : superClass.getDeclaredClasses()) {
            if (subClass.getSimpleName().equals(name)) {
                return subClass;
            }
        }
        throw new RuntimeException("Reflection error (subclass): " + superClass.getName() + ", " + name);
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
        return CLASS_CACHE.computeIfAbsent(name, (d) -> getCanonicalClass(setPlaceholders(name)));
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
        throw new RuntimeException("Reflection error (class): " + Arrays.stream(names).map(Reflection::setPlaceholders).collect(Collectors.toList()));
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
            throw new RuntimeException("Reflection error (canonical class): " + name);
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
            if (!replacement.isEmpty() && matcher.end() < name.length()
                    && name.charAt(matcher.end()) != '.')
                replacement += ".";
            matcher.appendReplacement(output,
                    Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(output);
        return output.toString();
    }

    public static <T> ConstructorInvoker<T> getConstructor(Class<?> target, Class<?>... parameters) {
        return (ConstructorInvoker<T>) new ConstructorInvoker<>(target, parameters);
    }

    public static MethodInvoker<Object> getMethod(Method method) {
        return new MethodInvoker<>(method);
    }

    public static MethodInvoker<Object> getMethod(Class<?> target, Predicate<Method> methodPredicate) {
        for (Method method : target.getDeclaredMethods()) {
            if (methodPredicate.test(method)) {
                return new MethodInvoker<>(method);
            }
        }
        if (target.getSuperclass() != null) {
            return getMethod(target, methodPredicate);
        }
        throw new RuntimeException("Reflection error (method predicate): " + target.getName());
    }

    public static MethodInvoker<Object> getMethod(String target, String name, Class<?>... parameters) {
        return getMethod(getClass(target), name, parameters);
    }

    public static MethodInvoker<Object> getMethod(Class<?> target, int index, Class<?>... parameters) {
        int currentIndex = 0;

        for (Method method : target.getDeclaredMethods()) {
            if (Arrays.equals(method.getParameterTypes(), parameters)) {

                if (currentIndex++ == index) {
                    return new MethodInvoker<>(method);
                }
            }
        }
        if (target.getSuperclass() != null) {
            return getMethod(target.getSuperclass(), index, parameters);
        }
        throw new RuntimeException("Reflection error (method): " + target.getName() + ", " + index + ", " + Arrays.toString(parameters));
    }

    public static MethodInvoker<Object> getMethod(Class<?> target, String name, Class<?>... parameters) {
        for (Method method : target.getDeclaredMethods()) {
            if (!method.getName().equals(name) || !Arrays.equals(method.getParameterTypes(), parameters)) {
                continue;
            }
            return new MethodInvoker<>(method);
        }
        if (target.getSuperclass() != null) {
            return getMethod(target.getSuperclass(), name, parameters);
        }
        throw new RuntimeException("Reflection error (method): " + target.getName() + ", " + name + ", " + Arrays.toString(parameters));
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
        throw new RuntimeException("Reflection error (method): " + index + " " + target.getName() + ", " + returnType + ", " + returnType);
    }

    public static MethodInvoker<Object> getMethodFromReturnType(Class<?> target, String name, Class<?> returnType) {
        for (Method method : target.getDeclaredMethods()) {

            if (!(method.getReturnType().equals(returnType) && name.equals(method.getName()))) {
                continue;
            }
            return new MethodInvoker<>(method);
        }
        if (target.getSuperclass() != null) {
            return getMethodFromReturnType(target.getSuperclass(), name, returnType);
        }
        throw new RuntimeException("Reflection error (method):" + name + " " + target.getName() + ", " + returnType + ", " + returnType);
    }

    public static MethodInvoker<Object> getMethodFromReturnType(Class<?> target, int index, Class<?> returnType, Class<?>... parameters) {
        int currentIndex = 0;
        for (Method method : target.getDeclaredMethods()) {

            if (method.getReturnType() == returnType && Arrays.equals(method.getParameterTypes(), parameters)) {
                if (currentIndex == index) {
                    return new MethodInvoker<>(method);
                }
                currentIndex++;
            }
        }
        if (target.getSuperclass() != null) {
            return getMethodFromReturnType(target.getSuperclass(), index, returnType, parameters);
        }
        throw new RuntimeException("Reflection error (method): " + index + " " + target.getName() + ", " + returnType + ", " + returnType);
    }

    public static <T> FieldAccessor<T> getNullableField(Class<?> target, String name, Class<?> type) {
        for (Field field : target.getDeclaredFields()) {
            if (!((name == null || field.getName().equals(name))
                    && (type == null || field.getType().equals(type)))) {
                continue;
            }
            return new FieldAccessor<>(field);
        }
        if (target.getSuperclass() != null) {
            return getNullableField(target.getSuperclass(), name, type);
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
        if (target.getSuperclass() != null) {
            return getNullableField(target.getSuperclass(), index, type);
        }
        return null;
    }

    public static <T> FieldAccessor<T> getNullableFieldAssignableFrom(Class<?> target, String name, Class<?> type) {
        for (Field field : target.getDeclaredFields()) {
            if (!field.getName().equals(name) || !type.isAssignableFrom(field.getType())) {
                continue;
            }
            return new FieldAccessor<>(field);
        }
        if (target.getSuperclass() != null) {
            return getNullableFieldAssignableFrom(target.getSuperclass(), name, type);
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
        if (target.getSuperclass() != null) {
            return getNullableFieldAssignableFrom(target.getSuperclass(), index, type);
        }
        return null;
    }

    public static <T> FieldAccessor<T> getField(String target, String name, Class<?> type) {
        return getField(getClass(target), name, type);
    }

    public static <T> FieldAccessor<T> getField(Class<?> target, String name, Class<?>... types) {
        for (Class<?> type : types) {
            FieldAccessor<T> accessor = getNullableField(target, name, type);
            if (accessor != null) {
                return accessor;
            }
        }
        if (target.getSuperclass() != null) {
            return getField(target.getSuperclass(), name, types);
        }
        throw new RuntimeException("Reflection error (field): " + target.getName() + ", " + name + ", " + Arrays.toString(types));
    }

    public static <T> FieldAccessor<T> getField(Class<?> target, int index, Class<?>... types) {
        for (Class<?> type : types) {
            FieldAccessor<T> accessor = getNullableField(target, index, type);
            if (accessor != null) {
                return accessor;
            }
        }
        if (target.getSuperclass() != null) {
            return getField(target.getSuperclass(), index, types);
        }
        throw new RuntimeException("Reflection error (field): " + target.getName() + ", " + index + ", " + Arrays.toString(types));
    }

    public static <T> FieldAccessor<T> getFieldAssignableFrom(Class<?> target, String name, Class<?>... types) {
        for (Class<?> type : types) {
            FieldAccessor<T> accessor = getNullableFieldAssignableFrom(target, name, type);
            if (accessor != null) {
                return accessor;
            }
        }
        if (target.getSuperclass() != null) {
            return getFieldAssignableFrom(target.getSuperclass(), name, types);
        }
        throw new RuntimeException("Reflection error (assignable type field): " + target.getName() + ", " + name + ", " + Arrays.toString(types));
    }

    public static <T> FieldAccessor<T> getFieldAssignableFrom(Class<?> target, int index, Class<?>... types) {
        for (Class<?> type : types) {
            FieldAccessor<T> accessor = getNullableFieldAssignableFrom(target, index, type);
            if (accessor != null) {
                return accessor;
            }
        }
        if (target.getSuperclass() != null) {
            return getFieldAssignableFrom(target.getSuperclass(), index, types);
        }
        throw new RuntimeException("Reflection error (assignable type field): " + target.getName() + ", " + index + ", " + Arrays.toString(types));
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
        if (target.getSuperclass() != null) {
            return getParameterizedField(target.getSuperclass(), bound, generic);
        }
        throw new RuntimeException("Reflection error (parameterized field):" + target.getName() + ", " + bound + ", " + Arrays.toString(generic));
    }

    public static Enum<?> getEnumAtIndex(Class<Enum<?>> enumClass, int ordinal) {
        for (Enum<?> enumValue : enumClass.getEnumConstants()) {
            if (enumValue.ordinal() ==ordinal) {
                return enumValue;
            }
        }
        throw new RuntimeException("Reflection error (enum): " + enumClass.getName() + ", " + ordinal);
    }

    public static Enum<?> getEnumForName(Class<Enum<?>> enumClass, String name) {
        for (Enum<?> enumValue : enumClass.getEnumConstants()) {
            if (enumValue.name().equals(name)) {
                return enumValue;
            }
        }
        throw new RuntimeException("Reflection error (enum): " + enumClass.getName() + ", " + name);
    }

    public static void setFinalField(Object instance, FieldAccessor<?> accessor, Object value) throws Exception {
        setFinalField(instance, accessor.getField(), value);
    }

    public static void setFinalField(Object instance, Field field, Object value) throws Exception {
       if (Modifier.isFinal(field.getModifiers())) {
           try {
               setFinalFieldAccessible(field);
           } catch (Exception ex) {
               field.set(instance, value);
               if (field.get(instance) == value) {
                   return;
               }

               throw new Exception(ex);
           }
       }

       field.set(instance, value);
    }

    public static void setFinalFieldAccessible(Field field) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        field.setAccessible(true);

        try {
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            int modifiers = modifiersField.getInt(field);

            modifiers &= ~Modifier.FINAL;
            modifiersField.setInt(field, modifiers);
        } catch (NoSuchFieldException e) {
            Method getDeclaredFields0 = Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class);
            getDeclaredFields0.setAccessible(true);

            Field[] fields = (Field[]) getDeclaredFields0.invoke(Field.class, false);
            for (Field classField : fields) {
                if ("modifiers".equals(classField.getName())) {
                    classField.setAccessible(true);
                    classField.set(field, field.getModifiers() & ~Modifier.FINAL);
                    break;
                }
            }
        }
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
