package me.alek.honey.netty.packet.cancellables;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.Cancellable;

public class CancellablePacketImpl implements Cancellable {

    private final PacketContainer<?> packetContainer;

    public CancellablePacketImpl(PacketContainer<?> packetContainer) {
        this.packetContainer = packetContainer;
    }

    @Override
    public void cancel() {
        HoneyPlugin.get().getNotificationService().showPacketFilterActionBar(packetContainer.getPlayer(), true, true);
        packetContainer.cancel();
    }

    @Override
    public boolean isCancelled() {
        return packetContainer.isCancelled();
    }
}
