package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.checks.helper.wrappers.models.WrappedBlockPosition;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import org.bukkit.inventory.ItemStack;

public class WrappedPlayInBlockPlace extends WrappedPacket<WrappedPlayInBlockPlace> {


    public WrappedPlayInBlockPlace(Object rawPacket, PacketContainer<WrappedPlayInBlockPlace> packetContainer) {
        super(rawPacket, packetContainer);
    }

    public ItemStack getItem() {
       return getContainer().getItems().read(0);
    }

    public WrappedBlockPosition getBlockPosition() {
        return getContainer().getBlockPositions().read(1);
    }

    public long getTimestamp() {
        return getContainer().getLongs().read(0);
    }

}
