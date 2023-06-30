package me.alek.honey.netty.modelwrappers;

import me.alek.honey.netty.api.packet.IStructureModifier;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.utility.reflect.NMSUtils;

import javax.annotation.Nullable;

public class WrappedItem extends WrappedObject<WrappedItem> {

    private static final ReflectStructure<Object, ?> structure = ReflectStructureCache.acquireStructure(NMSUtils.getItemClass());
    private static final IStructureModifier<WrappedItem> itemModifier = structure
            .withType(NMSUtils.getItemClass(), Converters.getItemConverter()).withTarget(null); // static handle
    private static final IStructureModifier<WrappedRegistryMaterials<Object, Object>> registryModifier = structure
            .withWeakType(NMSUtils.getRegistryMaterialsClass(), Converters.getRegistryMaterialsConverter()).withTarget(null); // static handle

    public WrappedItem(@Nullable Object handle) {
        super(handle);
    }

    public static WrappedItem getItemById(int id) {
        return itemModifier.invokeMethod("getById", 0, id);
    }

    public static int getId(WrappedItem item) {
        return (item == null) ? 0 : registryModifier.readField(0).getIndexFor(item);
    }

}
