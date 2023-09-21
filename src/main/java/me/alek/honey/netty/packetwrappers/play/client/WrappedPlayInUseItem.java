package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.modelwrappers.WrappedBlockPosition;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.packetwrappers.play.client.common.WrappedAbstractBlockInteraction;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.utility.protocol.Protocol;
import me.alek.honey.netty.utility.reflect.NMSUtils;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

public class WrappedPlayInUseItem extends WrappedAbstractBlockInteraction<WrappedPlayInUseItem> {

    private final boolean MODERN = Protocol.getProtocol().isNewerThanOrEqual(Protocol.v1_14);

    private static final Class<Object> BLOCK_POSITION_CLASS = NMSUtils.getBlockPositionClass();

    public WrappedPlayInUseItem(Object rawPacket, PacketContainer<WrappedPlayInUseItem> packetContainer) {
        super(rawPacket, packetContainer);
    }

    @Override
    public WrappedBlockPosition getBlockPosition() {
        if (!MODERN) {
            return getReflectStructure().withType(BLOCK_POSITION_CLASS, Converters.getBlockPositionConverter())
                .withTarget(getHandle()).readField(0);
        }
        final Object movingObjectPositionBlock = getReflectStructure().withType(NMSUtils.getMovingObjectPositionBlockClass())
            .withTarget(getHandle()).readField(0);

        return ReflectStructureCache.acquireStructure(NMSUtils.getMovingObjectPositionBlockClass())
            .withType(BLOCK_POSITION_CLASS, Converters.getBlockPositionConverter())
            .withTarget(movingObjectPositionBlock).readField(0);
    }
}
