package me.alek.honey.netty.packet.cancellables;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packet.CancellablePacketObserver;

public class CancellablePacketDelegateImpl implements CancellablePacketObserver {

    private final PacketContainer<?> packetContainer;

    public CancellablePacketDelegateImpl(PacketContainer<?> packetContainer) {
        this.packetContainer = packetContainer;
    }

    @Override
    public void cancel() {
        HoneyPlugin.get().getNotificationService().showCancellingPackets(packetContainer.getPlayer());
        packetContainer.cancel();
    }
}
