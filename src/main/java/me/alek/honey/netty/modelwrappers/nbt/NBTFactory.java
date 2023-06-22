package me.alek.honey.netty.modelwrappers.nbt;

import me.alek.honey.helper.ItemHelper;
import me.alek.honey.netty.api.packet.IStructureModifier;
import me.alek.honey.netty.packet.structure.ReflectStructure;
import me.alek.honey.netty.packet.structure.ReflectStructureCache;
import me.alek.honey.netty.packet.structure.converters.Converters;
import me.alek.honey.netty.utility.reflect.ConstructorInvoker;
import me.alek.honey.netty.utility.reflect.NMSUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.inventory.ItemStack;

public class NBTFactory {

    public static NBTCompound fromItem(ItemStack item) {
        Object nmsItem = ItemHelper.asNMSItem(item);
        Validate.notNull(nmsItem);

        ReflectStructure<Object, ?> reflectStructure = ReflectStructureCache.acquireStructure(nmsItem.getClass());
        IStructureModifier<NBTBase<?, ?>> structureModifier = reflectStructure
                .withType(NMSUtils.getNBTTagCompoundClass(), Converters.getNBTBaseConverter())
                .withTarget(nmsItem);

        NBTBase<?, ?> base = structureModifier.readField(0);
        if (base == null) {
            return null;
        }
        return (NBTCompound) base;
    }

    public static <K, V> NBTBase<K, V> fromDelegate(Object handle) {
        return copyFromType(getType(handle), handle);
    }

    public static <K, V> NBTBase<K, V> copyBase(NBTBase<K, V> base) {
        return copyFromType(base.getType(), base.getHandle());
    }

    public static <K, V> NBTBase<K, V> copyFromType(NBTType type, Object handle) {
        ConstructorInvoker<? extends NBTBase<?, ?>> constructorInvoker = type.getConstructorInvoker();

        return (NBTBase<K, V>) constructorInvoker.invoke(handle);
    }

    public static NBTType getType(Object handle) {
        ReflectStructure<Object, ?> reflectStructure = ReflectStructureCache.acquireStructure(handle.getClass());
        IStructureModifier<Byte> structureModifier = reflectStructure
                .withType(byte.class)
                .withTarget(handle);

        byte typeId = structureModifier.invokeMethod(0);
        return NBTType.getFromTypeId(typeId);
    }
}
