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
       return getPacket().getItems().read(0);
    }

    public WrappedBlockPosition getBlockPosition() {
        return getPacket().getBlockPositions().read(1);
    }

    public long getTimestamp() {
        return getPacket().getLongs().read(0);
    }

}
