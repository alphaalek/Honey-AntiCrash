package me.alek.honey.netty.packet.cancellables;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packet.CancellablePacketObserver;
import org.bukkit.Bukkit;

public class CancellablePacketDelegateImpl implements CancellablePacketObserver {

    private final PacketContainer<?> packetContainer;

    public CancellablePacketDelegateImpl(PacketContainer<?> packetContainer) {
        this.packetContainer = packetContainer;
    }

    @Override
    public void cancel() {
        packetContainer.cancel();
    }
}
