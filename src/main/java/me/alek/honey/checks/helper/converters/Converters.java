package me.alek.honey.checks.helper.converters;

import me.alek.honey.checks.helper.wrappers.models.WrappedBlockPosition;
import me.alek.honey.netty.api.packet.IStructureModifier;
import me.alek.honey.netty.packet.structure.ReflectStructure;
import me.alek.honey.netty.packet.structure.ReflectStructureCache;
import me.alek.honey.netty.utility.reflect.ConstructorInvoker;
import me.alek.honey.netty.utility.reflect.NMSUtils;
import me.alek.honey.netty.utility.reflect.Reflection;

public class Converters {

    private static TwinConverter<Object, WrappedBlockPosition> BLOCK_POSITION_CONVERTER;

    public static TwinConverter<Object, WrappedBlockPosition> getBlockPositionConverter() {

        if (BLOCK_POSITION_CONVERTER == null) {

            BLOCK_POSITION_CONVERTER = new TwinConverter<Object, WrappedBlockPosition>() {
                @Override
                public WrappedBlockPosition convertDelegate(Object delegate) {
                    if (!NMSUtils.isBlockPosition(delegate)) {
                        return null;
                    }
                    ReflectStructure<Object, Object> blockPositionStructure = ReflectStructureCache.acquireStructure(NMSUtils.getBaseBlockPositionClass());
                    IStructureModifier<Integer> intStructure = blockPositionStructure.withType(int.class).withTarget(delegate);

                    if (intStructure.size() < 3) {
                        throw new RuntimeException("Reflection fejl");
                    }

                    return new WrappedBlockPosition(intStructure.read(0), intStructure.read(1), intStructure.read(2));
                }

                @Override
                public Object convertDeclaring(WrappedBlockPosition blockPosition) {
                    ConstructorInvoker blockPositionConstructor = getDelegatedConstructor();
                    if (blockPositionConstructor == null) {
                        throw new RuntimeException("Reflection fejl");
                    }
                    try {
                        return blockPositionConstructor.invoke(
                                blockPosition.getX(), blockPosition.getY(), blockPosition.getZ());
                    } catch (Exception ex) {
                        throw new RuntimeException("Reflection fejl");
                    }
                }

                @Override
                public ConstructorInvoker getDelegatedConstructor() {
                    try {
                        return Reflection.getConstructor(
                                NMSUtils.getBlockPositionClass(), int.class, int.class, int.class);
                    } catch (Exception ex) {
                        return null;
                    }
                }
            };
        }
        return BLOCK_POSITION_CONVERTER;
    }
}
