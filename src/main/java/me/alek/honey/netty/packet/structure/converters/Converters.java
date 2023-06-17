package me.alek.honey.netty.packet.structure.converters;

import me.alek.honey.helper.ItemHelper;
import me.alek.honey.netty.modelwrappers.WrappedBlockPosition;
import me.alek.honey.netty.api.packet.IStructureModifier;
import me.alek.honey.netty.packet.structure.ReflectStructure;
import me.alek.honey.netty.packet.structure.ReflectStructureCache;
import me.alek.honey.netty.utility.reflect.ConstructorInvoker;
import me.alek.honey.netty.utility.reflect.NMSUtils;
import me.alek.honey.netty.utility.reflect.Reflection;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class Converters {

    private static Map<Class<?>, TwinConverter<?, ?>> CONVERTER_CACHE = new HashMap<>();

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
                ReflectStructure<Object, Object> blockPositionStructure = ReflectStructureCache.acquireStructure(NMSUtils.getBaseBlockPositionClass());
                IStructureModifier<Integer> intStructure = blockPositionStructure.withType(int.class).withTarget(delegate);

                if (intStructure.size() < 3) {
                    throw new RuntimeException("Reflection fejl");
                }

                return new WrappedBlockPosition(intStructure.read(0), intStructure.read(1), intStructure.read(2));
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
}
