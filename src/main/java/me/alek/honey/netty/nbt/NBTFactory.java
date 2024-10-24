package me.alek.honey.netty.nbt;

import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.structure.converters.ListConverter;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.ConstructorInvoker;
import me.alek.honey.util.reflection.ItemReflection;
import me.alek.honey.util.reflection.NMS;
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

        NBTRoot root = NBTRoot.createNewDefault();

        ReflectStructure<Object, ?> reflectStructure = ReflectStructure.Provider.acquireStructure(nmsItem.getClass());
        IStructureModifier<NBTBase<?, ?>> structureModifier = reflectStructure
                .withTypeWithoutCache(NMS.getNBTTagCompoundClass(), Converters.getNBTBaseConverter(root, null, forceCreation))
                .withTarget(nmsItem);

        NBTBase<?, ?> base = structureModifier.readField(0);
        if (base == null && createIfNone) {
            try {
                Object handle = NMS.getNBTTagCompoundClass().getDeclaredConstructor().newInstance();

                NBTCompound nbt = new NBTCompound(handle, root, null, forceCreation);
                structureModifier.writeField(0, nbt);
                return nbt;
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

        if (type == NBTType.ARRAY) {
            return (NBTBase<K, V>) constructorInvoker.invoke(handle, getArrayType(handle, root, owner, forceCreation), root, owner, forceCreation);
        } else {
            return (NBTBase<K, V>) constructorInvoker.invoke(handle, root, owner, forceCreation);
        }
    }

    @SuppressWarnings("unchecked")
    public static NBTType getArrayType(Object handle, NBTRoot root, NBTBase<?, ?> owner, boolean forceCreation) {
        if (handle == null) {
            return NBTType.UNKNOWN;
        }

        IStructureModifier<Byte> byteModifier = ReflectStructure.Provider.acquireStructure(NMS.getNBTTagListClass())
                .withBytes()
                .withTarget(handle);
        byte typeId = byteModifier.readField(0);
        if (typeId != 0) {
            return NBTType.getFromTypeId(typeId);
        }

        IStructureModifier<ListConverter<Object, NBTBase<?, ?>>> arrayStructure = ReflectStructure.Provider.acquireStructure(handle.getClass())
            .withWeakType(List.class, Converters.<NBTBase<?, ?>>getNBTListConverter(root, owner, forceCreation))
            .withTarget(handle);
        List<? extends NBTBase<?, ?>> array = arrayStructure.readFieldAndApplyMapping(0)
                .getDelegatedList();
        if (array.isEmpty()) {
            return NBTType.UNKNOWN;
        }

        NBTBase<?, ?> value = array.get(0);
        if (value == null) {
            return NBTType.UNKNOWN;
        }

        return value.getType();
    }

    public static byte getTypeId(IStructureModifier<Byte> structureModifier) {
        if (structureModifier == null) {
            return -1;
        }

        if (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_17)) {
            return structureModifier.invokeMethod(null, "getTypeId", 0);
        } else {
            return structureModifier.invokeMethod(null,
                    (index, invoker) -> invoker.getHandle().getDeclaringClass().equals(NMS.getNBTBaseClass()));
        }
    }

    public static NBTType getType(Object handle, boolean ignoreNestedArrays, NBTRoot root, NBTBase<?, ?> owner, boolean forceCreation) {
        if (handle == null) {
            return NBTType.UNKNOWN;
        }

        ReflectStructure<Object, ?> reflectStructure = ReflectStructure.Provider.acquireStructure(NMS.getNBTBaseClass());
        IStructureModifier<Byte> structureModifier = reflectStructure
                .withBytes()
                .withTarget(handle);

        byte typeId = getTypeId(structureModifier);

        NBTType type = NBTType.getFromTypeId(typeId);
        if (type == NBTType.ARRAY && !ignoreNestedArrays) {
            switch (getArrayType(handle, root, owner, forceCreation)) {
                case COMPOUND:
                    return NBTType.ARRAY_OF_COMPOUND;
                case ARRAY:
                    return NBTType.ARRAY_OF_ARRAY;
                default:
                    break;
            }
        }

        return type;
    }
}
