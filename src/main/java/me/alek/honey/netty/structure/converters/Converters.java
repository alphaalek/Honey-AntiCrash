package me.alek.honey.netty.structure.converters;

import me.alek.honey.helper.ItemHelper;
import me.alek.honey.netty.modelwrappers.WrappedBlockPosition;
import me.alek.honey.netty.api.packet.IStructureModifier;
import me.alek.honey.netty.modelwrappers.nbt.NBTBase;
import me.alek.honey.netty.modelwrappers.nbt.NBTFactory;
import me.alek.honey.netty.modelwrappers.nbt.NBTRoot;
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
    private static Map<NBTBase<?, ?>, NBTConverter> NBT_CONVERTER_CACHE = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static <R, V> JavaConverter<R, V> getConverter(Class<?> clazz, Function<? super Class<?>, ? extends JavaConverter<?, ?>> orElse) {
        return (JavaConverter<R, V>) CONVERTER_CACHE.computeIfAbsent(clazz, orElse);
    }

    public static NBTConverter getNBTConverter(NBTBase<?, ?> owner, Function<? super NBTBase<?, ?>, ? extends NBTConverter> orElse) {
        return NBT_CONVERTER_CACHE.computeIfAbsent(owner, orElse);
    }

    @SuppressWarnings("unchecked")
    public static <R, V, D> SimpleMapConverter<R, V, D> getMapConverter(SimpleMapConverter<?, ?, ?> converter) {
        return (SimpleMapConverter<R, V, D>) converter;
    }

    @SuppressWarnings("unchecked")
    public static <R, V> SimpleListConverter<R, V> getListConverter(SimpleListConverter<?, ?> converter) {
        return (SimpleListConverter<R, V>) converter;
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
                return ItemHelper.asBukkitItem(delegate);
            }

            @Override
            public Object convertDeclaring(ItemStack itemStack) {
                return ItemHelper.asNMSItem(itemStack);
            }
        });
    }

    public static JavaConverter<Object, NBTBase<?, ?>> getNBTBaseConverter(NBTBase<?, ?> owner) {
        return getNBTConverter(owner, (nbtRoot) -> new NBTConverter(owner));
    }

    public static SimpleMapConverter<String, Object, NBTBase<?, ?>> getNBTCompoundConverter(NBTBase<?, ?> owner) {
        return getMapConverter(new SimpleMapConverter<>(getNBTBaseConverter(owner)));
    }

    public static <T> SimpleListConverter<Object, T> getNBTListConverter(NBTBase<?, ?> owner) {
        return getListConverter(new SimpleListConverter<>(getNBTBaseConverter(owner)));
    }
}
