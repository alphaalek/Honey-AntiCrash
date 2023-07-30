package me.alek.honey.netty.modelwrappers.network;

import io.netty.util.AttributeKey;
import me.alek.honey.netty.api.packet.IStructureModifier;
import me.alek.honey.netty.modelwrappers.WrappedObject;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.utility.reflect.NMSUtils;

public class WrappedNetworkManager extends WrappedObject<WrappedNetworkManager> {

    public static AttributeKey<Object> getEnumProtocolKey() {
        ReflectStructure<Object, ?> structure = ReflectStructureCache.acquireStructure(NMSUtils.getNetworkManagerClass());
        ReflectStructure<AttributeKey<Object>, AttributeKey<Object>> specificStructure
                = structure.withWeakType(AttributeKey.class);
        IStructureModifier<AttributeKey<Object>> modifier = specificStructure.withTarget(null);
        return modifier.readField(0);
    }

    public WrappedNetworkManager(Object handle) {
        super(handle);
    }
}
