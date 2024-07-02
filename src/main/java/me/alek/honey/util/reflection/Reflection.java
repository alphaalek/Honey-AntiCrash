package me.alek.honey.util.reflection;

import me.alek.honey.util.protocol.NetProtocol;
import org.bukkit.Bukkit;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Comparator;
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

        String protocolPackage;
        boolean newProtocol = NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_17);
        if (newProtocol) {
            NMS = "net.minecraft";
            protocolPackage = NMS + ".network.protocol";
        } else {
            NMS = protocolPackage = OBC.replace("org.bukkit.craftbukkit", "net.minecraft.server");
        }
        COMMON        = protocolPackage + (newProtocol ? "." + "common"        : "");
        COOKIE        = protocolPackage + (newProtocol ? "." + "cookie"        : "");
        HANDSHAKE     = protocolPackage + (newProtocol ? "." + "handshake"     : "");
        PING          = protocolPackage + (newProtocol ? "." + "ping"          : "");
        PLAY          = protocolPackage + (newProtocol ? "." + "game"          : "");
        STATUS        = protocolPackage + (newProtocol ? "." + "status"        : "");
        LOGIN         = protocolPackage + (newProtocol ? "." + "login"         : "");
        CONFIGURATION = protocolPackage + (newProtocol ? "." + "configuration" : "");

        OBC_PREFIX = OBC;
        NMS_PREFIX = NMS;
    }

    private static final String OBC_PREFIX;
    private static final String COMMON;
    private static final String COOKIE;
    private static final String HANDSHAKE;
    private static final String PING;
    private static final String PLAY;
    private static final String STATUS;
    private static final String LOGIN;
    private static final String CONFIGURATION;
    private static final String NMS_PREFIX;
    private static final String VERSION = OBC_PREFIX.replace(
            "org.bukkit.craftbukkit", "").replace(".", "");
    private static final Pattern MATCH_VARIABLE = Pattern.compile("\\{([^\\}]+)\\}");

    @SuppressWarnings("unchecked")
    public static Class<Object> asObject(Class<?> clazz) {
        return (Class<Object>) clazz;
    }

    public static Class<?> getSubClass(Class<?> superClass, String name) {
        for (Class<?> subClass : superClass.getDeclaredClasses()) {
            if (subClass.getSimpleName().equals(name)) {
                return subClass;
            }
        }

        throw new RuntimeException("Reflection error (subclass): " + superClass.getName() + ", " + name);
    }

    public static Class<?> getSubClass(Class<?> superClass, int i) {
        int index = 0;
        for (Class<?> subClass : superClass.getDeclaredClasses()) {
            if (index++ == i) {
                return subClass;
            }
        }

        throw new RuntimeException("Reflection error (subclass): " + superClass.getName() + ", " + i);
    }

    public static Class<?> getClassWithException(String name) throws Exception {
        return getCanonicalClassWithException(setPlaceholders(name));
    }

    public static Class<?> getClass(String name) {
        return CLASS_CACHE.computeIfAbsent(name, (d) -> getCanonicalClass(setPlaceholders(name)));
    }

    public static Class<?> getArrayClass(String name) {
        return getClass("[L" + name + ";");
    }

    public static Class<?> getArrayClass(Class<?> clazz) {
        return getArrayClass(clazz.getName());
    }

    public static Class<?> getUnsureClass(String... names) {
        for (String name : names) {
            try {
                return getClassWithException(name);
            } catch (Exception ignored) {
            }
        }
        throw new RuntimeException("Reflection error (class): " + Arrays.stream(names).map(Reflection::setPlaceholders).collect(Collectors.toList()));
    }

    public static Class<Object> getUnsureObjectClass(String... names) {
        return (Class<Object>) getUnsureClass(names);
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
            String variable = matcher.group(1).toLowerCase();
            String replacement;
            switch (variable) {
                case "nms":
                    replacement = NMS_PREFIX;
                    break;
                case "obc":
                    replacement = OBC_PREFIX;
                    break;
                case "common":
                    replacement = COMMON;
                    break;
                case "cookie":
                    replacement = COOKIE;
                    break;
                case "handshake":
                    replacement = HANDSHAKE;
                    break;
                case "ping":
                    replacement = PING;
                    break;
                case "play":
                    replacement = PLAY;
                    break;
                case "status":
                    replacement = STATUS;
                    break;
                case "login":
                    replacement = LOGIN;
                    break;
                case "configuration":
                    replacement = CONFIGURATION;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid placeholder: " + variable);
            }

            if (!replacement.isEmpty() && matcher.end() < name.length()
                    && name.charAt(matcher.end()) != '.') {
                replacement += ".";
            }

            matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
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

    public static MethodInvoker<Object> getMethod(String target, String name, Class<?>... parameters) {
        return getMethod(getClass(target), name, parameters);
    }

    public static Method[] getDeclaredMethods(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredMethods()).sorted(new Comparator<Method>() {
            @Override
            public int compare(Method o1, Method o2) {
                return o1.getName().compareTo(o2.getName());
            }
        }).toArray(Method[]::new);
    }

    public static Field[] getDeclaredFields(Class<?> clazz) {
        return clazz.getDeclaredFields();
    }

    public static MethodInvoker<Object> getMethod(Class<?> target, Predicate<Method> methodPredicate) {
        for (Method method : getDeclaredMethods(target)) {
            if (methodPredicate.test(method)) {
                return new MethodInvoker<>(method);
            }
        }
        if (target.getSuperclass() != null) {
            return getMethod(target, methodPredicate);
        }
        throw new RuntimeException("Reflection error (method predicate): " + target.getName());
    }

    public static MethodInvoker<Object> getMethod(Class<?> target, int index, Class<?>... parameters) {
        int currentIndex = 0;

        for (Method method : getDeclaredMethods(target)) {
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
        for (Method method : getDeclaredMethods(target)) {
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
        for (Method method : getDeclaredMethods(target)) {
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
        for (Method method : getDeclaredMethods(target)) {
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
        for (Method method : getDeclaredMethods(target)) {
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
            if (name != null && !field.getName().equals(name)) {
                continue;
            } else if (type != null && !field.getType().equals(type)) {
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
            if (!field.getName().equals(name)
                    || !type.isAssignableFrom(field.getType())) {
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
            if (type.isAssignableFrom(field.getType()) && currentIndex++ == index) {
                return new FieldAccessor<>(field);
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
        if (types.length == 0) {
            return getNullableField(target, name, null);
        }

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
        setFinalField(instance, accessor.getHandle(), value);
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
