package me.alek.honey.netty.modelwrappers.nbt;

import me.alek.honey.helper.ItemHelper;
import me.alek.honey.netty.api.packet.IStructureModifier;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.structure.converters.ListConverter;
import me.alek.honey.netty.structure.converters.SimpleListConverter;
import me.alek.honey.netty.utility.reflect.ConstructorInvoker;
import me.alek.honey.netty.utility.reflect.NMSUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class NBTFactory {

    public static NBTCompound fromItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }
        Object nmsItem = ItemHelper.asNMSItem(item);
        if (nmsItem == null) {
            return null;
        }
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
        return copyFromType(getType(handle, false), handle);
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
        switch (type) {
            case LISTOFLIST:
            case LISTOFCOMOUND:
                return (NBTBase<K, V>) constructorInvoker.invoke(handle, Converters.getNBTListConverter());
            case LIST:
                return (NBTBase<K, V>) constructorInvoker.invoke(handle, getListType(handle), Converters.getNBTListConverter());
            case COMPOUND:
                return (NBTBase<K, V>) constructorInvoker.invoke(handle, Converters.getNBTCompoundConverter());
            // nbt element
            default:
                return (NBTBase<K, V>) constructorInvoker.invoke(handle);
        }
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

        IStructureModifier<ListConverter<Object, NBTBase<?, ?>>> listModifier = reflectStructure
            .<List<Object>, ListConverter<Object, NBTBase<?, ?>>>withWeakType(List.class,
                Converters.<NBTBase<?, ?>>getNBTListConverter().apply(

                    reflectStructure.withType((Class<List>) getType(handle, true).getClazz())
                        .withTarget(handle)
                        .readField(0)
                )
            )
            .withTarget(handle);

        List<? extends NBTBase<?, ?>> list = listModifier.readField(0).getDelegatedList();
        NBTBase<?, ?> value = list.get(0);

        if (value == null)
            return NBTType.UNKNOWN;

        return value.getType();
    }

    public static NBTType getType(Object handle, boolean ignoreNestedLists) {
        if (handle == null) {
            return NBTType.UNKNOWN;
        }
        ReflectStructure<Object, ?> reflectStructure = getNBTStructure(handle);
        IStructureModifier<Byte> structureModifier = reflectStructure
                .withType(byte.class)
                .withTarget(handle);
        Validate.notNull(structureModifier);

        byte typeId = structureModifier.invokeMethod("getTypeId", 0);
        NBTType type = NBTType.getFromTypeId(typeId);

        if (type == NBTType.LIST && !ignoreNestedLists) {

            switch (getListType(handle)) {

                case COMPOUND:
                    return NBTType.LISTOFCOMOUND;
                case LIST:
                    return NBTType.LISTOFLIST;
                default:
                    break;
            }
        }
        return type;
    }
}
