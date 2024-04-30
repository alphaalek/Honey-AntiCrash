package me.alek.honey.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.modelwrappers.WrappedBlockPosition;
import me.alek.honey.netty.util.FieldAccessor;
import me.alek.honey.packetwrappers.play.client.common.WrappedAbstractBlockInteraction;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.util.NetProtocol;
import me.alek.honey.netty.util.NMS;

import java.util.concurrent.atomic.AtomicReference;

public class WrappedPlayInUseItem extends WrappedAbstractBlockInteraction<WrappedPlayInUseItem> {

    private final boolean MODERN = NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_14);

    private static final Class<Object> BLOCK_POSITION_CLASS = NMS.getBlockPositionClass();

    public WrappedPlayInUseItem(Object rawPacket, PacketContainer<WrappedPlayInUseItem> packetContainer) {
        super(rawPacket, packetContainer);
    }

    @Override
    public WrappedBlockPosition getBlockPosition() {
        if (!MODERN) {
            return getReflectStructure().withType(BLOCK_POSITION_CLASS, Converters.getBlockPositionConverter())
                .withTarget(getHandle()).readField(0);
        }
        final Object movingObjectPositionBlock = getReflectStructure().withType(NMS.getMovingObjectPositionBlockClass())
            .withTarget(getHandle()).readField(0);

        return ReflectStructureCache.acquireStructure(NMS.getMovingObjectPositionBlockClass())
            .withType(BLOCK_POSITION_CLASS, Converters.getBlockPositionConverter())
            .withTarget(movingObjectPositionBlock).readField(0);
    }
}
