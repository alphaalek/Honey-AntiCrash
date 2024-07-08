package me.alek.honey.netty.structure.converters;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.codec.WrappedEnumProtocol;
import me.alek.honey.netty.modelwrappers.WrappedBlockPosition;
import me.alek.honey.netty.modelwrappers.WrappedChunkCoordIntPair;
import me.alek.honey.netty.modelwrappers.WrappedGameProfile;
import me.alek.honey.netty.modelwrappers.WrappedRenderData;
import me.alek.honey.netty.modelwrappers.nbt.NBTBase;
import me.alek.honey.netty.modelwrappers.nbt.NBTRoot;
import me.alek.honey.netty.modelwrappers.suggestion.WrappedSuggestion;
import me.alek.honey.netty.modelwrappers.suggestion.WrappedSuggestions;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.CachedReflectStructureProvider;
import me.alek.honey.netty.structure.converters.dedicated.NBTConverter;
import me.alek.honey.netty.structure.converters.dedicated.SimpleListConverter;
import me.alek.honey.netty.structure.converters.dedicated.SimpleMapConverter;
import me.alek.honey.netty.structure.converters.dedicated.SimpleSetConverter;
import me.alek.honey.util.reflection.ConstructorInvoker;
import me.alek.honey.util.reflection.ItemReflection;
import me.alek.honey.util.reflection.NMS;
import me.alek.honey.util.reflection.Reflection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class Converters {

    private static final Map<Class<?>, JavaConverter<?, ?>> CONVERTER_CACHE = new HashMap<>();
    private static final Map<Class<?>, ListConverter<?, ?>> LIST_CONVERTER_CACHE = new HashMap<>();
    private static final Map<Class<?>, SetConverter<?, ?>> SET_CONVERTER_CACHE = new HashMap<>();
    private static final Map<Class<?>, MapConverter<?, ?, ?>> MAP_CONVERTER_CACHE = new HashMap<>();

    @SuppressWarnings("unchecked")
    private static <R, V> JavaConverter<R, V> getConverter(Class<?> clazz, Function<? super Class<?>, ? extends JavaConverter<?, ?>> orElse) {
        return (JavaConverter<R, V>) CONVERTER_CACHE.computeIfAbsent(clazz, orElse);
    }

    @SuppressWarnings("unchecked")
    private static <R, V, D> MapConverter<R, V, D> getMapConverter(Class<?> clazz, Function<? super Class<?>, ? extends MapConverter<R, V, D>> orElse) {
        return (MapConverter<R, V, D>) MAP_CONVERTER_CACHE.computeIfAbsent(clazz, orElse);
    }

    @SuppressWarnings("unchecked")
    private static <R, V> SetConverter<R, V> getSetConverter(Class<?> clazz, Function<? super Class<?>, ? extends SetConverter<R, V>> orElse) {
        return (SetConverter<R, V>) SET_CONVERTER_CACHE.computeIfAbsent(clazz, orElse);
    }

    @SuppressWarnings("unchecked")
    private static <R, V> ListConverter<R, V> getListConverter(Class<?> clazz, Function<? super Class<?>, ? extends ListConverter<R, V>> orElse) {
        return (ListConverter<R, V>) LIST_CONVERTER_CACHE.computeIfAbsent(clazz, orElse);
    }

    public static JavaConverter<Object, WrappedBlockPosition> getBlockPositionConverter() {

        return getConverter(WrappedBlockPosition.class, (clazz) -> new JavaConverter<Object, WrappedBlockPosition>() {
            @Override
            public WrappedBlockPosition convertDelegate(Object delegate, Object... params) {
                if (delegate == null) {
                    return null;
                }

                if (!NMS.isBlockPosition(delegate)) {
                    return null;
                }

                ReflectStructure<Object, ?> blockPositionStructure = CachedReflectStructureProvider.acquireStructure(NMS.getBaseBlockPositionClass());
                IStructureModifier<Integer> intStructure = blockPositionStructure.withInts().withTarget(delegate);
                if (intStructure.fieldSize() < 3) {
                    throw new RuntimeException("Reflection error");
                }

                return new WrappedBlockPosition(intStructure.readField(0), intStructure.readField(1), intStructure.readField(2));
            }

            @Override
            public Object convertDeclaring(WrappedBlockPosition blockPosition) {
                if (blockPosition == null) {
                    return null;
                }

                ConstructorInvoker<Object> blockPositionConstructor = Reflection.getConstructor(
                        NMS.getBlockPositionClass(), int.class, int.class, int.class);
                try {
                    return blockPositionConstructor.invoke(
                            blockPosition.getX(), blockPosition.getY(), blockPosition.getZ());
                } catch (Exception ex) {
                    throw new RuntimeException("Reflection error");
                }
            }
        });
    }

    public static JavaConverter<Object, Vector> getVec3DConverter() {
        return getConverter(Vector.class, (clazz) -> new JavaConverter<Object, Vector>() {
            @Override
            public Vector convertDelegate(Object delegate, Object... params) {
                if (delegate == null) {
                    return null;
                }

                ReflectStructure<Object, ?> vec3DModifier = CachedReflectStructureProvider.acquireStructure(NMS.getVec3DClass());
                IStructureModifier<Double> doubleModifier = vec3DModifier.withDoubles().withTarget(delegate);

                if (doubleModifier.fieldSize() < 3) {
                    throw new RuntimeException("Reflection error");
                }

                return new Vector(doubleModifier.readField(0), doubleModifier.readField(1), doubleModifier.readField(2));
            }

            @Override
            public Object convertDeclaring(Vector object) {
                if (object == null) {
                    return null;
                }
                ConstructorInvoker<Object> blockPositionConstructor = Reflection.getConstructor(
                        NMS.getVec3DClass(), double.class, double.class, double.class);
                try {
                    return blockPositionConstructor.invoke(
                            object.getX(), object.getY(), object.getZ());
                } catch (Exception ex) {
                    throw new RuntimeException("Reflection error");
                }
            }
        });
    }

    public static JavaConverter<Object, ItemStack> getItemStackConverter() {
        return getConverter(ItemStack.class, (clazz) -> new JavaConverter<Object, ItemStack>() {
            @Override
            public ItemStack convertDelegate(Object delegate, Object... params) {
                return ItemReflection.asBukkitCopy(delegate);
            }

            @Override
            public Object convertDeclaring(ItemStack itemStack) {
                return ItemReflection.asNMSCopy(itemStack);
            }
        });
    }

    public static JavaConverter<Object, WrappedRenderData> getRenderDataConverter() {
        return getConverter(WrappedRenderData.class, (clazz) -> new SimpleWrapperConverter<>(WrappedRenderData.class));
    }

    public static JavaConverter<Object, WrappedGameProfile> getGameProfileConverter() {
        return getConverter(WrappedGameProfile.class, (clazz) -> new SimpleWrapperConverter<>(WrappedGameProfile.class));
    }

    public static JavaConverter<Object, PacketDataSerializer> getPacketDataSerializerConverter() {
        return new JavaConverter<Object, PacketDataSerializer>() {
            @Override
            public PacketDataSerializer convertDelegate(Object delegate, Object... params) {
                ByteBuf buf = CachedReflectStructureProvider.acquireStructure(NMS.getPacketDataSerializerClass())
                        .withType(ByteBuf.class).withTarget(delegate)
                        .readField(0);

                // copy
                if (params.length > 0 && params[0].equals(true)) {
                    buf = Unpooled.copiedBuffer(buf);
                }

                return new PacketDataSerializer(buf);
            }

            @Override
            public Object convertDeclaring(PacketDataSerializer object) {
                return object.getSerializer();
            }
        };
    }

    public static JavaConverter<Enum<?>, WrappedEnumProtocol> getEnumProtocolConverter() {
        return getConverter(WrappedEnumProtocol.class, (clazz) -> new SimpleWrapperConverter<>(WrappedEnumProtocol.class, PacketBound.class));
    }

    public static JavaConverter<Object, WrappedSuggestions> getSuggestionsConverter() {
        return getConverter(WrappedSuggestions.class, (clazz) -> new SimpleWrapperConverter<>(WrappedSuggestions.class));
    }

    public static JavaConverter<Object, WrappedSuggestion> getSuggestionConverter() {
        return getConverter(WrappedSuggestion.class, (clazz) -> new SimpleWrapperConverter<>(WrappedSuggestion.class));
    }

    public static JavaConverter<Object, WrappedChunkCoordIntPair> getChunkCoordIntConverter() {
        return getConverter(WrappedChunkCoordIntPair.class, (clazz) -> new JavaConverter<Object, WrappedChunkCoordIntPair>() {
            @Override
            public WrappedChunkCoordIntPair convertDelegate(Object delegate, Object... params) {
                return new WrappedChunkCoordIntPair(delegate);
            }

            @Override
            public Object convertDeclaring(WrappedChunkCoordIntPair object) {
                if (object == null) {
                    return null;
                }
                ConstructorInvoker<Object> constructorInvoker = Reflection.getConstructor(
                        NMS.getChunkCoordIntPairClass(), int.class, int.class
                );
                Object chunkPair;
                try {
                    chunkPair = constructorInvoker.invoke(object.getX(), object.getY());
                } catch (Exception ex) {
                    throw new RuntimeException("Reflection error");
                }
                return chunkPair;
            }
        });
    }

    // nbt
    public static JavaConverter<Object, NBTBase<?, ?>> getNBTBaseConverter(NBTRoot root, NBTBase<?, ?> owner, boolean forceCreation) {
        return new NBTConverter(root, owner, forceCreation);
    }

    public static MapConverter<String, Object, NBTBase<?, ?>> getNBTCompoundConverter(NBTRoot root, NBTBase<?, ?> owner, boolean forceCreation) {
        return getMapConverterWithoutCache(getNBTBaseConverter(root, owner, forceCreation), true);
    }

    @SuppressWarnings("unchecked")
    public static <T> ListConverter<Object, T> getNBTListConverter(NBTRoot root, NBTBase<?, ?> owner, boolean forceCreation) {
        return (ListConverter<Object, T>) getListConverterWithoutCache(getNBTBaseConverter(root, owner, forceCreation), true);
    }

    // lists
    public static <K, T> ListConverter<K, T> getListConverterOf(Class<?> clazz, JavaConverter<K, T> converter, boolean controlDeclaring) {
        return getListConverter(clazz, (clazz1) -> new SimpleListConverter<>(converter, controlDeclaring));
    }

    public static <K, T> ListConverter<K, T> getListConverterWithoutCache(JavaConverter<K, T> converter, boolean controlDeclaring) {
        return new SimpleListConverter<>(converter, controlDeclaring);
    }

    // sets
    public static <K, T> SetConverter<K, T> getSetConverterOf(Class<?> clazz, JavaConverter<K, T> converter, boolean controlDeclaring) {
        return getSetConverter(clazz, (clazz1) -> new SimpleSetConverter<>(converter, controlDeclaring));
    }

    public static <K, T> SetConverter<K, T> getSetConverterWithoutCache(JavaConverter<K, T> converter, boolean controlDeclaring) {
        return new SimpleSetConverter<>(converter, controlDeclaring);
    }

    // maps
    public static <K, T, D> MapConverter<K, T, D> getMapConverterOf(Class<?> clazz, JavaConverter<T, D> converter, boolean controlDeclaring) {
        return getMapConverter(clazz, (clazz1) -> new SimpleMapConverter<>(converter, controlDeclaring));
    }

    public static <K, T, D> MapConverter<K, T, D> getMapConverterWithoutCache(JavaConverter<T, D> converter, boolean controlDeclaring) {
        return new SimpleMapConverter<>(converter, controlDeclaring);
    }
}
