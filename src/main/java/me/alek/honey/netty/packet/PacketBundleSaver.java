package me.alek.honey.netty.packet;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class PacketBundleSaver {
    private static final Map<Set<PacketTypeEnum>, Integer> PACKET_SAVES_SIZES = new HashMap<>();
    private static byte MAX_PACKET_ID;

    private final Map<Set<PacketTypeEnum>, Deque<PacketContainer<?>>> PACKET_SAVES = new HashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    static {
        reloadFilters();
    }

    public static void reloadFilters() {
        PACKET_SAVES_SIZES.clear();
        PACKET_SAVES_SIZES.putAll(PacketBundleFilters.getPacketSaves());

        byte max = 0;
        for (PacketTypeEnum packetType : PACKET_SAVES_SIZES.keySet().stream().flatMap(Set::stream).collect(Collectors.toSet())) {
            if (packetType.getPacketId() > max)
                max = packetType.getPacketId();
        }
        MAX_PACKET_ID = max;
    }

    private final List<PacketSaverQueue>[] packets;
    private final LinkedList<PacketContainer<?>> commonPackets = new LinkedList<>();

    public PacketBundleSaver() {
        packets = new List[MAX_PACKET_ID + 1];

        for (Set<PacketTypeEnum> packetTypeSet : PACKET_SAVES_SIZES.keySet()) {
            for (PacketTypeEnum packetType : packetTypeSet) {
                List<PacketSaverQueue> packets = this.packets[packetType.getPacketId()];
                if (packets == null) {
                    this.packets[packetType.getPacketId()] = (packets = new ArrayList<>());
                }
                boolean hasForPacketTypes = false;
                for (PacketSaverQueue queue : packets) {
                    if (queue.packetTypes.equals(packetTypeSet)) {
                        hasForPacketTypes = true;
                        break;
                    }
                }
                if (!hasForPacketTypes) {
                    int size = PACKET_SAVES_SIZES.get(packetTypeSet);
                    if (size == 0) continue;

                    packets.add(new PacketSaverQueue(packetTypeSet, size));
                }
            }
        }
    }

    public void transferFrom(PacketBundleSaver bundleSaver) {
        lock.writeLock().lock();
        try {
            PacketContainer<?> packet;
            while ((packet = bundleSaver.getAny()) != null) {
                add(packet);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private class PacketSaverQueue {

        private final Set<PacketTypeEnum> packetTypes;
        private final int size;
        private final Deque<PacketContainer<?>> packets;

        public PacketSaverQueue(Set<PacketTypeEnum> packetTypes, int size) {
            this.packetTypes = packetTypes;
            this.size = size;
            packets = PACKET_SAVES.computeIfAbsent(packetTypes, (d) -> new LinkedList<>());
        }

        public void add(PacketContainer<?> packet) {
            if (!packetTypes.contains(packet.getType())) return;

            packets.add(packet);
            if (packets.size() > size) packets.remove();
        }
    }

    private synchronized List<PacketSaverQueue> getQueue(PacketTypeEnum packetType) {
        if (packetType.getPacketId() > MAX_PACKET_ID) return null;

        return packets[packetType.getPacketId()];
    }

    public <WP extends WrappedPacket<WP>> void add(PacketContainer<WP> packet) {
        commonPackets.add(packet);

        List<PacketSaverQueue> queues = getQueue(packet.getType());
        if (queues == null) return;

        for (PacketSaverQueue queue : queues) {
            queue.add(packet);
        }
    }

    public <WP extends WrappedPacket<WP>> PacketContainer<WP> get(PacketTypeEnum packetType) {
        List<PacketSaverQueue> queues = getQueue(packetType);
        if (queues == null) return null;

        for (PacketSaverQueue queue : queues) {
            PacketContainer<?> packet = poll(queue);
            if (packet != null && packet.getType() == packetType)
                return (PacketContainer<WP>) packet;
        }
        return null;
    }

    public List<PacketContainer<?>> get(PacketTypeEnum packetType, int amount) {
        List<PacketSaverQueue> queues = getQueue(packetType);
        if (queues == null || amount == 0) return null;

        List<PacketContainer<?>> mostRecent = new ArrayList<>();
        for (PacketSaverQueue queue : queues) {
            List<PacketContainer<?>> packets = new ArrayList<>();
            PacketContainer<?> packet;
            while ((packet = poll(queue)) != null) {
                if (packet.getType() == packetType) {
                    packets.add(packet);
                }
                if (packets.size() == amount) break;
            }
            if (mostRecent.size() < packets.size() || packets.get(packets.size() - 1).getTime() < mostRecent.get(mostRecent.size() - 1).getTime()) {
                mostRecent.clear();
                mostRecent.addAll(packets);
            }
        }
        return mostRecent;
    }

    public <WP extends WrappedPacket<WP>> PacketContainer<WP> get(Set<PacketTypeEnum> packetTypeSet) {
        lock.readLock().lock();
        try {
            filterPacketTypes(packetTypeSet);

            PacketTypeEnum first = packetTypeSet.iterator().next();

            List<PacketSaverQueue> queues = getQueue(first);
            if (queues == null) return null;

            for (PacketSaverQueue queue : queues) {
                if (queue.packetTypes.equals(packetTypeSet))
                    return poll(queue);
            }
        } finally {
            lock.readLock().unlock();
        }
        return null;
    }

    public List<PacketContainer<?>> get(Set<PacketTypeEnum> packetTypeSet, int amount) {
        lock.readLock().lock();
        try {
            filterPacketTypes(packetTypeSet);

            PacketTypeEnum first = packetTypeSet.iterator().next();

            List<PacketSaverQueue> queues = getQueue(first);
            if (queues == null || amount == 0) return null;

            List<PacketContainer<?>> packets = new ArrayList<>();
            for (PacketSaverQueue queue : queues) {
                if (queue.packetTypes.equals(packetTypeSet)) {
                    PacketContainer<?> packet;
                    while ((packet = poll(queue)) != null) {
                        packets.add(packet);

                        if (packets.size() == amount) return packets;
                    }
                }
            }
            return packets;
        } finally {
            lock.readLock().unlock();
        }
    }

    private void filterPacketTypes(Set<PacketTypeEnum> packetTypes) {
        Set<PacketTypeEnum> filteredPacketTypes = PacketBundleFilters.getFilterAllPackets();
        packetTypes.removeIf(filteredPacketTypes::contains);
    }

    private <WP extends WrappedPacket<WP>> PacketContainer<WP> poll(PacketSaverQueue queue) {
        PacketContainer<WP> packet = (PacketContainer<WP>) queue.packets.poll();
        removeFromSavers(packet);

        return packet;
    }

    public void removeFromSavers(PacketContainer<?> packet) {
        if (packet == null) return;

        List<PacketSaverQueue> queues = getQueue(packet.getType());
        if (queues == null) return;

        for (PacketSaverQueue queue : queues) {
            queue.packets.remove(packet);
        }
    }

    public <WP extends WrappedPacket<WP>> PacketContainer<WP> getAny() {
        lock.readLock().lock();
        try {
            return (PacketContainer<WP>) commonPackets.poll();
        } finally {
            lock.readLock().unlock();
        }
    }

    public void clear() {
        lock.readLock().lock();
        try {
            for (Deque<PacketContainer<?>> packetList : PACKET_SAVES.values()) {
                packetList.clear();
            }
            commonPackets.clear();
        } finally {
            lock.readLock().unlock();
        }
    }
}
