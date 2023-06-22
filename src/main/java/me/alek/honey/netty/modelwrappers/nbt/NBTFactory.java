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

        return (NBTCompound) structureModifier.readField(0);
    }

    public static NBTBase<?, ?> fromDelegate(Object nbt) {
        NBTType type = getType(nbt);
        ConstructorInvoker<? extends NBTBase<?, ?>> constructorInvoker = type.getConstructorInvoker();

        return (NBTBase<?, ?>) constructorInvoker.invoke(nbt);
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
