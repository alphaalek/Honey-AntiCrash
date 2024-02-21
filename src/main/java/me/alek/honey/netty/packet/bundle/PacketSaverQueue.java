package me.alek.honey.netty.packet.bundle;

import me.alek.honey.netty.api.packet.container.PacketContainer;

import java.util.Deque;
import java.util.LinkedList;

public class PacketSaverQueue {

    private final PacketSaverWindow window;
    private final Deque<PacketContainer<?>> packets;

    public PacketSaverQueue(PacketBundleSaver saver, PacketSaverWindow window) {
        this.window = window;
        packets = saver.getPacketSaves().computeIfAbsent(window, (d) -> new LinkedList<>());
    }

    public void add(PacketContainer<?> packet) {
        if (!window.getPacketTypes().contains(packet.getType())) return;

        packets.add(packet);
        if (packets.size() > window.size()) packets.remove();
    }

    public boolean isEmpty() {
        return packets.isEmpty();
    }

    public Deque<PacketContainer<?>> getPackets() {
        return packets;
    }

    public int size() {
        return packets.size();
    }

    public PacketSaverWindow getWindow() {
        return window;
    }
}