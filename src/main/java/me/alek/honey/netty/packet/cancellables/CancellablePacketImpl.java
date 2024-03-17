package me.alek.honey.netty.packet.cancellables;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.CancellablePacketObserver;

public class CancellablePacketImpl implements CancellablePacketObserver {

    private final PacketContainer<?> packetContainer;

    public CancellablePacketImpl(PacketContainer<?> packetContainer) {
        this.packetContainer = packetContainer;
    }

    @Override
    public void cancel() {
        HoneyPlugin.get().getNotificationService().showCancellingPackets(packetContainer.getPlayer());
        packetContainer.cancel();
    }

    @Override
    public boolean isCancelled() {
        return packetContainer.isCancelled();
    }
}
