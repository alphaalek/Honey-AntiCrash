package me.alek.honey.netty.packet;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import org.bukkit.Bukkit;

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

    private PacketBundleSaver transferedTo;

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

    public void transferTo(PacketBundleSaver bundleSaver) {
        lock.writeLock().lock();
        try {
            PacketContainer<?> packet;
            while ((packet = getOldest(true)) != null) {
                bundleSaver.add(packet);
            }
            transferedTo = bundleSaver;
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
        addToCommon(packet);

        List<PacketSaverQueue> queues = getQueue(packet.getType());
        if (queues == null) return;

        for (PacketSaverQueue queue : queues) {
            queue.add(packet);
        }
    }

    public <WP extends WrappedPacket<WP>> void addToCommon(PacketContainer<WP> packet) {
        commonPackets.add(packet);
    }

    public <WP extends WrappedPacket<WP>> PacketContainer<WP> get(PacketTypeEnum packetType, boolean remove) {
        List<PacketSaverQueue> queues = getQueue(packetType);
        if (queues == null) return null;

        for (PacketSaverQueue queue : queues) {
            PacketContainer<?> packet = get(queue, remove);
            if (packet != null && packet.getType() == packetType)
                return (PacketContainer<WP>) packet;
        }
        return null;
    }

    public List<PacketContainer<?>> get(PacketTypeEnum packetType, int amount, boolean remove) {
        List<PacketSaverQueue> queues = getQueue(packetType);
        if (queues == null || amount == 0) return null;

        List<PacketContainer<?>> mostRecent = new ArrayList<>();
        for (PacketSaverQueue queue : queues) {
            List<PacketContainer<?>> packets = getPackets(queue, amount, remove);

            if (mostRecent.size() < packets.size() || packets.get(packets.size() - 1).getTime() < mostRecent.get(mostRecent.size() - 1).getTime()) {
                mostRecent.clear();
                mostRecent.addAll(packets);
            }
        }
        return mostRecent;
    }

    public <WP extends WrappedPacket<WP>> PacketContainer<WP> get(Set<PacketTypeEnum> packetTypeSet, boolean remove) {
        lock.readLock().lock();

        try {
            if (transferedTo != null) {
                return transferedTo.get(packetTypeSet, remove);
            }

            filterPacketTypes(packetTypeSet);

            PacketTypeEnum first = packetTypeSet.iterator().next();

            List<PacketSaverQueue> queues = getQueue(first);
            if (queues == null) return null;

            for (PacketSaverQueue queue : queues) {
                if (queue.packetTypes.equals(packetTypeSet))
                    return get(queue, remove);
            }
        } finally {
            lock.readLock().unlock();
        }
        return null;
    }

    public List<PacketContainer<?>> get(Set<PacketTypeEnum> packetTypeSet, int amount, boolean remove) {
        if (amount == 0) return null;

        lock.readLock().lock();
        try {
            if (transferedTo != null) {
                return transferedTo.get(packetTypeSet, amount, remove);
            }

            filterPacketTypes(packetTypeSet);

            PacketTypeEnum first = packetTypeSet.iterator().next();

            List<PacketSaverQueue> queues = getQueue(first);
            if (queues == null) return null;

            for (PacketSaverQueue queue : queues) {
                if (queue.packetTypes.equals(packetTypeSet)) {
                    return getPackets(queue, amount, remove);
                }
            }
            return new ArrayList<>();
        } finally {
            lock.readLock().unlock();
        }
    }

    private List<PacketContainer<?>> getPackets(PacketSaverQueue queue, int amount, boolean remove) {
        return remove ? pollPackets(queue, amount) : peekPackets(queue, amount);
    }

    private List<PacketContainer<?>> pollPackets(PacketSaverQueue queue, int amount) {
        List<PacketContainer<?>> packets = new ArrayList<>();
        PacketContainer<?> packet;
        while ((packet = poll(queue)) != null) {
            packets.add(packet);

            if (packets.size() == amount) return packets;
        }
        return packets;
    }

    private List<PacketContainer<?>> peekPackets(PacketSaverQueue queue, int amount) {
        List<PacketContainer<?>> packets = new ArrayList<>();
        for (PacketContainer<?> packet : queue.packets) {
            packets.add(packet);

            if (packets.size() == amount) return packets;
        }
        return packets;
    }

    private void filterPacketTypes(Set<PacketTypeEnum> packetTypes) {
        Set<PacketTypeEnum> filteredPacketTypes = PacketBundleFilters.FILTER_ALL_PACKETS;
        packetTypes.removeIf(filteredPacketTypes::contains);
    }

    private <WP extends WrappedPacket<WP>> PacketContainer<WP> get(PacketSaverQueue queue, boolean remove) {
        if (!remove) return (PacketContainer<WP>) queue.packets.peek();

        return poll(queue);
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

    public <WP extends WrappedPacket<WP>> PacketContainer<WP> getOldest(boolean remove) {
        lock.readLock().lock();
        try {
            if (transferedTo != null) {
                return transferedTo.getOldest(remove);
            }

            return (PacketContainer<WP>) (remove ? commonPackets.poll() : commonPackets.peek());
        } finally {
            lock.readLock().unlock();
        }
    }

    public <WP extends WrappedPacket<WP>> PacketContainer<WP> getRecent(boolean remove) {
        lock.readLock().lock();
        try {
            if (transferedTo != null) {
                return transferedTo.getRecent(remove);
            }

            return (PacketContainer<WP>) (remove ? commonPackets.pollLast() : commonPackets.peekLast());
        } finally {
            lock.readLock().unlock();
        }
    }

    public void clear() {
        lock.readLock().lock();
        try {
            if (transferedTo != null) {
                transferedTo.clear();
                return;
            }

            for (Deque<PacketContainer<?>> packetList : PACKET_SAVES.values()) {
                packetList.clear();
            }
            commonPackets.clear();
        } finally {
            lock.readLock().unlock();
        }
    }
}
