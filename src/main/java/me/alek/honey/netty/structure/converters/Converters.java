package me.alek.honey.netty.structure.converters;

import me.alek.honey.netty.modelwrappers.WrappedPacketDataSerializer;
import me.alek.honey.netty.modelwrappers.WrappedRenderData;
import me.alek.honey.netty.utility.reflect.ItemReflection;
import me.alek.honey.netty.modelwrappers.WrappedBlockPosition;
import me.alek.honey.netty.api.packet.IStructureModifier;
import me.alek.honey.netty.modelwrappers.nbt.NBTBase;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.utility.reflect.ConstructorInvoker;
import me.alek.honey.netty.utility.reflect.NMSUtils;
import me.alek.honey.netty.utility.reflect.Reflection;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class Converters {

    private static Map<Class<?>, JavaConverter<?, ?>> CONVERTER_CACHE = new HashMap<>();
    private static Map<Class<?>, ListConverter<?, ?>> LIST_CONVERTER_CACHE = new HashMap<>();
    private static Map<Class<?>, MapConverter<?, ?, ?>> MAP_CONVERTER_CACHE = new HashMap<>();
    private static Map<NBTBase<?, ?>, NBTConverter> NBT_CONVERTER_CACHE = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static <R, V> JavaConverter<R, V> getConverter(Class<?> clazz, Function<? super Class<?>, ? extends JavaConverter<?, ?>> orElse) {
        return (JavaConverter<R, V>) CONVERTER_CACHE.computeIfAbsent(clazz, orElse);
    }

    public static NBTConverter getNBTConverter(NBTBase<?, ?> owner, Function<? super NBTBase<?, ?>, ? extends NBTConverter> orElse) {
        return NBT_CONVERTER_CACHE.computeIfAbsent(owner, orElse);
    }

    @SuppressWarnings("unchecked")
    public static <R, V, D> MapConverter<R, V, D> getMapConverter(Class<?> clazz, Function<? super Class<?>, ? extends MapConverter<R, V, D>> orElse) {
        return (MapConverter<R, V, D>) MAP_CONVERTER_CACHE.computeIfAbsent(clazz, orElse);
    }

    @SuppressWarnings("unchecked")
    public static <R, V> ListConverter<R, V> getListConverter(Class<?> clazz, Function<? super Class<?>, ? extends ListConverter<R, V>> orElse) {
        return (ListConverter<R, V>) LIST_CONVERTER_CACHE.computeIfAbsent(clazz, orElse);
    }

    public static JavaConverter<Object, WrappedBlockPosition> getBlockPositionConverter() {

        return getConverter(WrappedBlockPosition.class, (clazz) -> new JavaConverter<Object, WrappedBlockPosition>() {
            @Override
            public WrappedBlockPosition convertDelegate(Object delegate) {
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
            public ItemStack convertDelegate(Object delegate) {
                return ItemReflection.asBukkitItem(delegate);
            }

            @Override
            public Object convertDeclaring(ItemStack itemStack) {
                return ItemReflection.asNMSItem(itemStack);
            }
        });
    }

    public static JavaConverter<Object, WrappedRenderData> getRenderDataConverter() {
        return getConverter(WrappedRenderData.class, (clazz) -> new SimpleWrapperConverter<>(WrappedRenderData.class));
    }

    public static JavaConverter<Object, WrappedPacketDataSerializer> getPacketDataSerializerConverter() {
        return getConverter(WrappedPacketDataSerializer.class, (clazz) -> new SimpleWrapperConverter<>(WrappedPacketDataSerializer.class));
    }

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

    public static <K, T> ListConverter<K, T> getListConverterOf(Class<?> clazz, JavaConverter<K, T> converter, boolean controlDeclaring) {
        return getListConverter(clazz, (clazz1) -> new SimpleListConverter<>(converter, controlDeclaring));
    }

    public static <K, T> ListConverter<K, T> getListConverterWithoutCache(JavaConverter<K, T> converter, boolean controlDeclaring) {
        return new SimpleListConverter<>(converter, controlDeclaring);
    }

    public static <K, T, D> MapConverter<K, T, D> getMapConverterOf(Class<?> clazz, JavaConverter<T, D> converter, boolean controlDeclaring) {
        return getMapConverter(clazz, (clazz1) -> new SimpleMapConverter<>(converter, controlDeclaring));
    }

    public static <K, T, D> MapConverter<K, T, D> getMapConverterWithoutCache(JavaConverter<T, D> converter, boolean controlDeclaring) {
        return new SimpleMapConverter<>(converter, controlDeclaring);
    }


}
