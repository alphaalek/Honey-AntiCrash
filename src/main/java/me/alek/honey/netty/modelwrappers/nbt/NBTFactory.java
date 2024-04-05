package me.alek.honey.netty.modelwrappers.nbt;

import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.util.*;
import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.structure.converters.ListConverter;
import org.apache.commons.lang.Validate;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class NBTFactory {

    public  static NBTCompound fromItem(ItemStack item) {
        return fromItem(item, false);
    }

    public static NBTCompound fromItem(ItemStack item, boolean createIfNone) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }
        final Object nmsItem = ItemReflection.getHandleItem(item);
        if (nmsItem == null) {
            return null;
        }
        final NBTRoot root = new NBTRoot(ConfigurationOptions.ItemAnalysis.NBT.MAX_NBT_BASES);
        final ReflectStructure<Object, ?> reflectStructure = ReflectStructureCache.acquireStructure(nmsItem.getClass());
        final IStructureModifier<NBTBase<?, ?>> structureModifier = reflectStructure
                .withTypeWithoutCache(NMS.getNBTTagCompoundClass(), Converters.getNBTBaseConverter(root))
                .withTarget(nmsItem);

        final NBTBase<?, ?> base = structureModifier.readField(0);
        if (base == null && createIfNone) {
            try {
                Object handle = NMS.getNBTTagCompoundClass().getDeclaredConstructor().newInstance();

                NBTCompound createdNBT = new NBTCompound(handle, root, root);
                structureModifier.writeField(0, createdNBT);

                return createdNBT;

            } catch (Exception ex) {
                ex.printStackTrace();
            }
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
        final ConstructorInvoker<? extends NBTBase<?, ?>> constructorInvoker = type.getWrappedInvoker();
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
        final byte type = byteModifier.readField(0);
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

    public static byte getTypeId(IStructureModifier<Byte> structureModifier) {
        if (structureModifier == null) {
            return (byte) -1;
        }
        if (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_17)) {
            return structureModifier.invokeMethod("getTypeId");
        }
        return structureModifier.invokeMethod(
                (invoker) -> invoker.getMethod().getDeclaringClass().getSuperclass() != NMS.getNBTBaseClass());
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

        final byte typeId = getTypeId(structureModifier);
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
