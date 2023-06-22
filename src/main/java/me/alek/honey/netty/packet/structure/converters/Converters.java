package me.alek.honey.netty.packet.structure.converters;

import me.alek.honey.helper.ItemHelper;
import me.alek.honey.netty.modelwrappers.WrappedBlockPosition;
import me.alek.honey.netty.api.packet.IStructureModifier;
import me.alek.honey.netty.modelwrappers.nbt.NBTBase;
import me.alek.honey.netty.modelwrappers.nbt.NBTCompound;
import me.alek.honey.netty.modelwrappers.nbt.NBTFactory;
import me.alek.honey.netty.modelwrappers.nbt.NBTList;
import me.alek.honey.netty.packet.structure.ReflectStructure;
import me.alek.honey.netty.packet.structure.ReflectStructureCache;
import me.alek.honey.netty.utility.reflect.ConstructorInvoker;
import me.alek.honey.netty.utility.reflect.NMSUtils;
import me.alek.honey.netty.utility.reflect.Reflection;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class Converters {

    private static Map<Class<?>, TwinConverter<?, ?>> CONVERTER_CACHE = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static <R, V> TwinConverter<R, V> getConverter(Class<?> clazz, Function<? super Class<?>, ? extends TwinConverter<?, ?>> orElse) {
        return (TwinConverter<R, V>) CONVERTER_CACHE.computeIfAbsent(clazz, orElse);
    }

    public static TwinConverter<Object, WrappedBlockPosition> getBlockPositionConverter() {

        return getConverter(WrappedBlockPosition.class, (clazz) -> new TwinConverter<Object, WrappedBlockPosition>() {
            @Override
            public WrappedBlockPosition convertDelegate(Object delegate) {
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
                ConstructorInvoker blockPositionConstructor = Reflection.getConstructor(
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

    public static TwinConverter<Object, ItemStack> getItemstackConverter() {
        return getConverter(ItemStack.class, (clazz) -> new TwinConverter<Object, ItemStack>() {
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

    public static TwinConverter<Object, NBTBase<?, ?>> getNBTBaseConverter() {
        return getConverter(NBTBase.class, (clazz) -> new TwinConverter<Object, NBTBase<?, ?>>() {
            @Override
            public NBTBase<?, ?> convertDelegate(Object delegate) {
                return NBTFactory.fromDelegate(delegate);
            }

            @Override
            public Object convertDeclaring(NBTBase<?, ?> declaring) {
                return declaring.getHandle();
            }
        });
    }

    public static TwinConverter<Map<String, Object>, Map<String, NBTBase<?, ?>>> getNBTCompoundConverter() {
        return getConverter(NBTCompound.class, (clazz) -> new MapConverter<>(getNBTBaseConverter()));
    }

    public static <T> TwinConverter<List<Object>, List<T>> getNBTListConverter() {
        return getConverter(NBTList.class, (clazz) -> new ListConverter<>(getNBTBaseConverter()));
    }
}
