package me.alek.honey.netty.structure.converters;

import me.alek.honey.netty.api.packet.IStructureModifier;
import me.alek.honey.netty.modelwrappers.*;
import me.alek.honey.netty.modelwrappers.nbt.NBTBase;
import me.alek.honey.netty.modelwrappers.network.WrappedEnumProtocol;
import me.alek.honey.netty.modelwrappers.network.WrappedPacketDataSerializer;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInUseEntity;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.structure.converters.dedicated.NBTConverter;
import me.alek.honey.netty.structure.converters.dedicated.SimpleListConverter;
import me.alek.honey.netty.structure.converters.dedicated.SimpleMapConverter;
import me.alek.honey.netty.structure.converters.dedicated.SimpleSetConverter;
import me.alek.honey.netty.utility.reflect.ConstructorInvoker;
import me.alek.honey.netty.utility.reflect.ItemReflection;
import me.alek.honey.netty.utility.reflect.NMSUtils;
import me.alek.honey.netty.utility.reflect.Reflection;
import org.bukkit.Chunk;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class Converters {

    private static Map<Class<?>, JavaConverter<?, ?>> CONVERTER_CACHE = new HashMap<>();
    private static Map<Class<?>, ListConverter<?, ?>> LIST_CONVERTER_CACHE = new HashMap<>();
    private static Map<Class<?>, SetConverter<?, ?>> SET_CONVERTER_CACHE = new HashMap<>();
    private static Map<Class<?>, MapConverter<?, ?, ?>> MAP_CONVERTER_CACHE = new HashMap<>();
    private static Map<NBTBase<?, ?>, NBTConverter> NBT_CONVERTER_CACHE = new HashMap<>();

    @SuppressWarnings("unchecked")
    private static <R, V> JavaConverter<R, V> getConverter(Class<?> clazz, Function<? super Class<?>, ? extends JavaConverter<?, ?>> orElse) {
        return (JavaConverter<R, V>) CONVERTER_CACHE.computeIfAbsent(clazz, orElse);
    }

    private static NBTConverter getNBTConverter(NBTBase<?, ?> owner, Function<? super NBTBase<?, ?>, ? extends NBTConverter> orElse) {
        return NBT_CONVERTER_CACHE.computeIfAbsent(owner, orElse);
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
                if (!NMSUtils.isBlockPosition(delegate)) {
                    return null;
                }
                ReflectStructure<Object, ?> blockPositionStructure = ReflectStructureCache.acquireStructure(NMSUtils.getBaseBlockPositionClass());
                IStructureModifier<Integer> intStructure = blockPositionStructure.withType(int.class).withTarget(delegate);

                if (intStructure.fieldSize() < 3) {
                    throw new RuntimeException("Reflection fejl");
                }

                return new WrappedBlockPosition(intStructure.readField(0), intStructure.readField(1), intStructure.readField(2));
            }

            @Override
            public Object convertDeclaring(WrappedBlockPosition blockPosition) {
                if (blockPosition == null) {
                    return null;
                }
                ConstructorInvoker<Object> blockPositionConstructor = Reflection.getConstructor(
                        NMSUtils.getBlockPositionClass(), int.class, int.class, int.class);
                try {
                    return blockPositionConstructor.invoke(
                            blockPosition.getX(), blockPosition.getY(), blockPosition.getZ());
                } catch (Exception ex) {
                    throw new RuntimeException("Reflection fejl");
                }
            }
        });
    }

    public static JavaConverter<Object, ItemStack> getItemstackConverter() {
        return getConverter(ItemStack.class, (clazz) -> new JavaConverter<Object, ItemStack>() {
            @Override
            public ItemStack convertDelegate(Object delegate, Object... params) {
                return ItemReflection.asBukkitItem(delegate);
            }

            @Override
            public Object convertDeclaring(ItemStack itemStack) {
                return ItemReflection.asNMSItem(itemStack);
            }
        });
    }

    public static JavaConverter<Object, Chunk> getChunkConverter() {
        return getConverter(Chunk.class, (clazz) -> new JavaConverter<Object, Chunk>() {
            @Override
            public Chunk convertDelegate(Object delegate, Object... params) {
                IStructureModifier<Chunk> chunkModifier = ReflectStructureCache.acquireStructure(delegate.getClass())
                        .withType(Chunk.class)
                        .withTarget(delegate);
                return chunkModifier.readField("bukkitChunk", 0);
            }

            @Override
            public Object convertDeclaring(Chunk object) {
                IStructureModifier<Object> craftChunkModifier = ReflectStructureCache.acquireStructure(NMSUtils.getCraftChunkClass())
                        .withType(NMSUtils.getChunkClass())
                        .withTarget(object);
                return craftChunkModifier.invokeMethod("getHandle", 0);
            }
        });
    }

    public static JavaConverter<Object, WrappedRenderData> getRenderDataConverter() {
        return getConverter(WrappedRenderData.class, (clazz) -> new SimpleWrapperConverter<>(WrappedRenderData.class));
    }

    public static JavaConverter<Object, WrappedGameProfile> getGameProfileConverter() {
        return getConverter(WrappedGameProfile.class, (clazz) -> new SimpleWrapperConverter<>(WrappedGameProfile.class));
    }

    public static JavaConverter<Object, WrappedPacketDataSerializer> getPacketDataSerializerConverter() {
        return getConverter(WrappedPacketDataSerializer.class, (clazz) -> new SimpleWrapperConverter<>(WrappedPacketDataSerializer.class));
    }

    public static JavaConverter<Object, WrappedEnumProtocol> getEnumProtocolConverter() {
        return getConverter(WrappedEnumProtocol.class, (clazz) -> new SimpleWrapperConverter<>(WrappedEnumProtocol.class));
    }

    public static <V> JavaConverter<Object, WrappedLongObjectHashMap<V>> getLongObjectHashMapConverter() {
        return getConverter(WrappedLongObjectHashMap.class, (clazz) -> new SimpleWrapperConverter<>(WrappedLongObjectHashMap.class));
    }

    public static JavaConverter<Object, WrappedPlayInUseEntity.EnumEntityUseAction> getEntityUseActionConverter() {
        return getConverter(WrappedPlayInUseEntity.EnumEntityUseAction.class, (clazz) -> new JavaConverter<Object, WrappedPlayInUseEntity.EnumEntityUseAction>() {

            private final Class<Object> enumEntityUseActionClass = NMSUtils.getEntityUseActionClass();

            @Override
            public WrappedPlayInUseEntity.EnumEntityUseAction convertDelegate(Object delegate, Object... params) {
                if (delegate == null) return null;
                return WrappedPlayInUseEntity.EnumEntityUseAction.valueOf(delegate.toString());
            }

            @Override
            public Object convertDeclaring(WrappedPlayInUseEntity.EnumEntityUseAction object) {
                if (object == null) return null;
                return enumEntityUseActionClass.getEnumConstants()[object.ordinal()];
            }
        });
    }

    public static JavaConverter<Object, Vector> getVector3DConverter() {
        return getConverter(Vector.class, (clazz) -> new JavaConverter<Object, Vector>() {

            private final Class<Object> vec3DClass = NMSUtils.getVec3DClass();

            @Override
            public Vector convertDelegate(Object delegate, Object... params) {
                if (delegate == null) return null;

                IStructureModifier<Double> modifier = ReflectStructureCache.acquireStructure(delegate.getClass())
                        .asDouble()
                        .withTarget(delegate);

                double x = modifier.readField(0);
                double y = modifier.readField(1);
                double z = modifier.readField(2);

                return new Vector(x, y ,z);
            }

            @Override
            public Object convertDeclaring(Vector vector) {
                if (vector == null) return null;

                ConstructorInvoker<Object> vectorConstructor = new ConstructorInvoker<>(vec3DClass);

                double x = vector.getX();
                double y = vector.getY();
                double z = vector.getZ();

                try {
                    return vectorConstructor.invokeWithException(x, y, z);

                } catch (Exception ex) {
                    return null;
                }
            }
        });

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
                        NMSUtils.getChunkCoordIntPairClass(), int.class, int.class
                );
                Object chunkPair;
                try {
                    chunkPair = constructorInvoker.invoke(object.getX(), object.getZ());
                } catch (Exception ex) {
                    throw new RuntimeException("Reflection fejl");
                }
                return chunkPair;
            }
        });
    }

    // nbt
    public static JavaConverter<Object, NBTBase<?, ?>> getNBTBaseConverter(NBTBase<?, ?> owner) {
        return getNBTConverter(owner, (nbtRoot) -> new NBTConverter(owner));
    }

    public static MapConverter<String, Object, NBTBase<?, ?>> getNBTCompoundConverter(NBTBase<?, ?> owner) {
        return getMapConverterWithoutCache(getNBTBaseConverter(owner), true);
    }

    @SuppressWarnings("unchecked")
    public static <T> ListConverter<Object, T> getNBTListConverter(NBTBase<?, ?> owner) {
        return (ListConverter<Object, T>) getListConverterWithoutCache(getNBTBaseConverter(owner), true);
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
