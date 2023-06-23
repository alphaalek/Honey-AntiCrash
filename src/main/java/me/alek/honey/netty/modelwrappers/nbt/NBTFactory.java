package me.alek.honey.netty.modelwrappers.nbt;

import me.alek.honey.helper.ItemHelper;
import me.alek.honey.netty.api.packet.IStructureModifier;
import me.alek.honey.netty.packet.structure.ReflectStructure;
import me.alek.honey.netty.packet.structure.ReflectStructureCache;
import me.alek.honey.netty.packet.structure.converters.Converters;
import me.alek.honey.netty.utility.reflect.ConstructorInvoker;
import me.alek.honey.netty.utility.reflect.NMSUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class NBTFactory {

    public static NBTCompound fromItem(ItemStack item) {
        Object nmsItem = ItemHelper.asNMSItem(item);
        if (nmsItem == null) {
            return null;
        }
        Bukkit.getLogger().info("CHECKING ITEM " + nmsItem);
        ReflectStructure<Object, ?> reflectStructure = ReflectStructureCache.acquireStructure(nmsItem.getClass());
        Bukkit.getLogger().info("STRUCTURE! " + reflectStructure);
        IStructureModifier<NBTBase<?, ?>> structureModifier = reflectStructure
                .withType(NMSUtils.getNBTTagCompoundClass(), Converters.getNBTBaseConverter())
                .withTarget(nmsItem);

        NBTBase<?, ?> base = structureModifier.readField(0);
        if (base == null) {
            return null;
        }
        Bukkit.getLogger().info("NBTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTT got nbt " + base + " from " + item.getType());
        return (NBTCompound) base;
    }

    public static <K, V> NBTBase<K, V> fromDelegate(Object handle) {
        return copyFromType(getType(handle), handle);
    }

    public static <K, V> NBTBase<K, V> copyBase(NBTBase<K, V> base) {
        return copyFromType(base.getType(), base.getHandle());
    }

    @SuppressWarnings("unchecked")
    public static <K, V> NBTBase<K, V> copyFromType(NBTType type, Object handle) {
        if (type == null || type == NBTType.UNKNOWN) {
            return null;
        }
        ConstructorInvoker<? extends NBTBase<?, ?>> constructorInvoker = (ConstructorInvoker<? extends NBTBase<?,?>>) type.getConstructorInvoker();
        if (constructorInvoker == null) {
            return null;
        }

        if (type == NBTType.LIST) {
            return (NBTBase<K, V>) constructorInvoker.invoke(handle, getListType(handle));
        }
        return (NBTBase<K, V>) constructorInvoker.invoke(handle);
    }

    public static ReflectStructure<Object, ? > getNBTStructure(Object handle) {
        if (handle == null) {
            return null;
        }
        return ReflectStructureCache.acquireStructure(handle.getClass());
    }

    public static NBTType getListType(Object handle) {
        if (handle == null) {
            return NBTType.UNKNOWN;
        }
        ReflectStructure<Object, ?> reflectStructure = getNBTStructure(handle);
        IStructureModifier<Byte> byteModifier = reflectStructure
                .withType(byte.class)
                .withTarget(handle);
        byte type = byteModifier.readField("type", 0);
        if (type != 0) {
            return NBTType.getFromTypeId(type);
        }

        IStructureModifier<List<NBTBase<?, ?>>> listModifier = reflectStructure
                .<List<Object>, List<NBTBase<?, ?>>>withWeakType(List.class, Converters.getNBTListConverter())
                .withTarget(handle);

        List<? extends NBTBase<?, ?>> list = listModifier.readField(0);
        NBTBase<?, ?> value = list.get(0);

        if (value == null)
            return NBTType.UNKNOWN;

        return value.getType();
    }

    public static NBTType getType(Object handle) {
        if (handle == null) {
            return NBTType.UNKNOWN;
        }
        ReflectStructure<Object, ?> reflectStructure = getNBTStructure(handle);
        IStructureModifier<Byte> structureModifier = reflectStructure
                .withType(byte.class)
                .withTarget(handle);
        Validate.notNull(structureModifier);

        Bukkit.broadcastMessage(structureModifier.methodSize() + "");
        byte typeId = structureModifier.invokeMethod("getTypeId", 0);
        return NBTType.getFromTypeId(typeId);
    }
}
