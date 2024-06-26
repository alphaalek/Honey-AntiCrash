package me.alek.honey.netty.modelwrappers.nbt;

import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.structure.converters.ListConverter;
import me.alek.honey.util.reflection.ConstructorInvoker;
import me.alek.honey.util.reflection.ItemReflection;
import me.alek.honey.util.reflection.NMS;
import me.alek.honey.util.protocol.NetProtocol;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class NBTFactory {

    public static NBTCompound fromItem(ItemStack item, boolean createIfNone, boolean forceCreation) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }

        Object nmsItem = ItemReflection.getHandleItem(item);
        if (nmsItem == null) {
            return null;
        }

        NBTRoot root = new NBTRoot(ConfigurationOptions.ItemAnalysis.NBT.MAX_NBT_BASES);
        ReflectStructure<Object, ?> reflectStructure = ReflectStructureCache.acquireStructure(nmsItem.getClass());
        IStructureModifier<NBTBase<?, ?>> structureModifier = reflectStructure
                .withTypeWithoutCache(NMS.getNBTTagCompoundClass(), Converters.getNBTBaseConverter(root, null, forceCreation))
                .withTarget(nmsItem);

        NBTBase<?, ?> base = structureModifier.readField(0);
        if (base == null && createIfNone) {
            try {
                Object handle = NMS.getNBTTagCompoundClass().getDeclaredConstructor().newInstance();

                NBTCompound createdNBT = new NBTCompound(handle, root, null, true);
                structureModifier.writeField(0, createdNBT);
                return createdNBT;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return (NBTCompound) base;
    }

    public static <K, V> NBTBase<K, V> fromDelegate(Object handle, NBTRoot root, NBTBase<?, ?> owner, boolean forceCreation) {
        return copyFromType(getType(handle, false, root, owner, forceCreation), handle, root, owner, forceCreation);
    }

    public static <K, V> NBTBase<K, V> copyBase(NBTBase<K, V> base, NBTRoot root, NBTBase<?, ?> owner) {
        return copyFromType(base.getType(), base.getHandle(), root, owner, true);
    }

    @SuppressWarnings("unchecked")
    public static <K, V> NBTBase<K, V> copyFromType(NBTType type, Object handle, NBTRoot root, NBTBase<?, ?> owner, boolean forceCreation) {
        if (type == null || type == NBTType.UNKNOWN) {
            return null;
        }

        ConstructorInvoker<? extends NBTBase<?, ?>> constructorInvoker = type.getWrappedInvoker();
        if (constructorInvoker == null) {
            return null;
        }

        if (type == NBTType.LIST) {
            return (NBTBase<K, V>) constructorInvoker.invoke(handle, getListType(handle, root, owner, forceCreation), root, owner, forceCreation);
        }
        return (NBTBase<K, V>) constructorInvoker.invoke(handle, root, owner, forceCreation);
    }

    public static ReflectStructure<Object, ? > getDirectNBTStructure(Object handle) {
        if (handle == null) {
            return null;
        }
        return ReflectStructureCache.acquireStructure(handle.getClass());
    }

    @SuppressWarnings("unchecked")
    public static NBTType getListType(Object handle, NBTRoot root, NBTBase<?, ?> owner, boolean forceCreation) {
        if (handle == null) {
            return NBTType.UNKNOWN;
        }

        IStructureModifier<Byte> byteModifier = ReflectStructureCache.acquireStructure(NMS.getNBTTagListClass())
                .withType(byte.class)
                .withTarget(handle);
        byte typeId = byteModifier.readField(0);
        if (typeId != 0) {
            return NBTType.getFromTypeId(typeId);
        }

        IStructureModifier<ListConverter<Object, NBTBase<?, ?>>> listModifier = getDirectNBTStructure(handle)
            .withWeakType(List.class, Converters.<NBTBase<?, ?>>getNBTListConverter(root, owner, forceCreation))
            .withTarget(handle);
        List<? extends NBTBase<?, ?>> list = listModifier.readFieldAndApplyMapping(0)
                .getDelegatedList();
        if (list.isEmpty()) {
            return NBTType.UNKNOWN;
        }

        NBTBase<?, ?> value = list.get(0);
        if (value == null) {
            return NBTType.UNKNOWN;
        }

        return value.getType();
    }

    public static byte getTypeId(IStructureModifier<Byte> structureModifier) {
        if (structureModifier == null) {
            return (byte) -1;
        }

        if (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_17)) {
            return structureModifier.invokeMethod(null, "getTypeId", 0);
        }

        return structureModifier.invokeMethod(null,
                (index, invoker) -> invoker.getHandle().getDeclaringClass().equals(NMS.getNBTBaseClass()));
    }

    public static NBTType getType(Object handle, boolean ignoreNestedLists, NBTRoot root, NBTBase<?, ?> owner, boolean forceCreation) {
        if (handle == null) {
            return NBTType.UNKNOWN;
        }

        ReflectStructure<Object, ?> reflectStructure = ReflectStructureCache.acquireStructure(NMS.getNBTBaseClass());
        IStructureModifier<Byte> structureModifier = reflectStructure
                .withType(byte.class)
                .withTarget(handle);
        byte typeId = getTypeId(structureModifier);

        NBTType type = NBTType.getFromTypeId(typeId);
        if (type == NBTType.LIST && !ignoreNestedLists) {
            switch (getListType(handle, root, owner, forceCreation)) {
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
