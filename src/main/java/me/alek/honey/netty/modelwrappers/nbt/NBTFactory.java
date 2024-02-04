package me.alek.honey.netty.modelwrappers.nbt;

import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.utility.ItemReflection;
import me.alek.honey.netty.api.packet.IStructureModifier;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.structure.converters.ListConverter;
import me.alek.honey.netty.utility.ConstructorInvoker;
import me.alek.honey.netty.utility.NMSUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class NBTFactory {

    public static NBTCompound fromItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }
        final Object nmsItem = ItemReflection.asNMSItem(item);
        if (nmsItem == null) {
            return null;
        }
        final NBTRoot root = new NBTRoot(ConfigurationOptions.ItemAnalysis.NBT.MAX_NBT_BASES + 10);
        final ReflectStructure<Object, ?> reflectStructure = ReflectStructureCache.acquireStructure(nmsItem.getClass());
        final IStructureModifier<NBTBase<?, ?>> structureModifier = reflectStructure
                .withTypeWithoutCache(NMSUtils.getNBTTagCompoundClass(), Converters.getNBTBaseConverter(root))
                .withTarget(nmsItem);

        final NBTBase<?, ?> base = structureModifier.readField(0);
        if (base == null) {
            return null;
        }
        return (NBTCompound) base;
    }

    public static <K, V> NBTBase<K, V> fromDelegate(Object handle, NBTBase<?, ?> owner) {
        return copyFromType(getType(handle, false, owner), handle, owner);
    }

    public static <K, V> NBTBase<K, V> copyBase(NBTBase<K, V> base, NBTBase<?, ?> owner) {
        return copyFromType(base.getType(), base.getHandle(), owner);
    }

    @SuppressWarnings("unchecked")
    public static <K, V> NBTBase<K, V> copyFromType(NBTType type, Object handle, NBTBase<?, ?> owner) {
        if (type == null || type == NBTType.UNKNOWN) {
            return null;
        }
        final ConstructorInvoker<? extends NBTBase<?, ?>> constructorInvoker = type.getConstructorInvoker();
        if (constructorInvoker == null) {
            return null;
        }
        NBTRoot root = null;
        if (owner != null) {
            root = owner.getRoot();
        }
        if (type == NBTType.LIST) {
            return (NBTBase<K, V>) constructorInvoker.invoke(handle, getListType(handle, root), root, owner);
        }
        return (NBTBase<K, V>) constructorInvoker.invoke(handle, root, owner);
    }

    public static ReflectStructure<Object, ? > getNBTStructure(Object handle) {
        if (handle == null) {
            return null;
        }
        return ReflectStructureCache.acquireStructure(handle.getClass());
    }

    @SuppressWarnings("unchecked")
    public static NBTType getListType(Object handle, NBTBase<?, ?> owner) {
        if (handle == null) {
            return NBTType.UNKNOWN;
        }
        final ReflectStructure<Object, ?> reflectStructure = getNBTStructure(handle);
        final IStructureModifier<Byte> byteModifier = reflectStructure
                .withType(byte.class)
                .withTarget(handle);
        final byte type = byteModifier.readField("type", 0);
        if (type != 0) {
            return NBTType.getFromTypeId(type);
        }

        final IStructureModifier<ListConverter<Object, NBTBase<?, ?>>> listModifier = reflectStructure
            .withWeakType(List.class, Converters.<NBTBase<?, ?>>getNBTListConverter(owner))
            .withTarget(handle);

        final List<? extends NBTBase<?, ?>> list = listModifier.readFieldAndApplyMapping(0).getDelegatedList();
        final NBTBase<?, ?> value = list.get(0);

        if (value == null)
            return NBTType.UNKNOWN;

        return value.getType();
    }

    public static NBTType getType(Object handle, boolean ignoreNestedLists, NBTBase<?, ?> owner) {
        if (handle == null) {
            return NBTType.UNKNOWN;
        }
        final ReflectStructure<Object, ?> reflectStructure = getNBTStructure(handle);
        final IStructureModifier<Byte> structureModifier = reflectStructure
                .withType(byte.class)
                .withTarget(handle);
        Validate.notNull(structureModifier);

        final byte typeId = structureModifier.invokeMethod("getTypeId", 0);
        final NBTType type = NBTType.getFromTypeId(typeId);

        if (type == NBTType.LIST && !ignoreNestedLists) {
            switch (getListType(handle, owner)) {
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
