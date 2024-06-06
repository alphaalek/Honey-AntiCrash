package me.alek.honey.netty.packet.bundle;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.packetwrappers.WrappedPacket;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class PacketBundleSaver {
    private static final Set<PacketSaverWindow> PACKET_SAVER_WINDOWS = new HashSet<>();
    private static int MAX_PACKET_ID;

    public final Map<PacketSaverWindow, Deque<PacketContainer<?>>> packetSaves = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    static {
        reloadFilters();
    }

    public static void reloadFilters() {
        PACKET_SAVER_WINDOWS.clear();
        PACKET_SAVER_WINDOWS.addAll(PacketBundleFilters.getPacketSaves());

        int max = 0;
        for (PacketTypeEnum packetType : PACKET_SAVER_WINDOWS.stream().flatMap((w) -> w.getPacketTypes().stream()).collect(Collectors.toSet())) {
            if (packetType.getHoneyID() > max)
                max = packetType.getHoneyID();
        }
        MAX_PACKET_ID = max;
    }

    private final List<PacketSaverQueue>[] packets;

    public PacketBundleSaver() {
        packets = new List[MAX_PACKET_ID + 1];

        for (PacketSaverWindow window : PACKET_SAVER_WINDOWS) {
            if (window.size() == 0) continue;

            Set<PacketTypeEnum> packetTypeSet = window.getPacketTypes();
            for (PacketTypeEnum packetType : packetTypeSet) {
                List<PacketSaverQueue> packets = this.packets[packetType.getHoneyID()];
                if (packets == null) {
                    this.packets[packetType.getHoneyID()] = (packets = new ArrayList<>());
                }
                boolean hasWindow = false;
                for (PacketSaverQueue queue : packets) {
                    if (queue.getWindow().equals(window)) {
                        hasWindow = true;
                        break;
                    }
                }
                if (!hasWindow) {
                    packets.add(new PacketSaverQueue(this, window));
                }
            }
        }
    }

    public Map<PacketSaverWindow, Deque<PacketContainer<?>>> getPacketSaves() {
        return packetSaves;
    }

    private synchronized List<PacketSaverQueue> getQueue(PacketTypeEnum packetType) {
        if (packetType.getHoneyID() > MAX_PACKET_ID) return null;

        return packets[packetType.getHoneyID()];
    }

    public void add(PacketContainer<? extends WrappedPacket<?>> packet) {
        List<PacketSaverQueue> queues = getQueue(packet.getType());
        if (queues == null) return;

        for (PacketSaverQueue queue : queues) {
            queue.add(packet);
        }
    }

    public PacketContainer<? extends WrappedPacket<?>> get(PacketTypeEnum packetType, boolean remove) {
        List<PacketSaverQueue> queues = getQueue(packetType);
        if (queues == null) return null;

        for (PacketSaverQueue queue : queues) {
            PacketContainer<?> packet = get(queue, remove);
            if (packet != null && packet.getType() == packetType)
                return packet;
        }
        return null;
    }

    public List<PacketContainer<?>> get(PacketTypeEnum packetType, int amount, boolean remove) {
        List<PacketSaverQueue> queues = getQueue(packetType);
        if (queues == null || amount == 0) return null;

        List<PacketContainer<?>> mostRecent = new ArrayList<>();
        for (PacketSaverQueue queue : queues) {
            List<PacketContainer<?>> packets = getPackets(queue, amount, remove);

            if (mostRecent.size() < packets.size() ||
                    !packets.isEmpty() && packets.get(packets.size() - 1).getTime() <
                            (mostRecent.isEmpty() ? Integer.MAX_VALUE : mostRecent.get(mostRecent.size() - 1).getTime())) {
                mostRecent.clear();
                mostRecent.addAll(packets);
            }
        }
        return mostRecent;
    }

    public PacketContainer<? extends WrappedPacket<?>> get(Set<PacketTypeEnum> packetTypeSet, boolean remove) {
        lock.readLock().lock();

        try {
            filterPacketTypes(packetTypeSet);

            PacketTypeEnum first = packetTypeSet.iterator().next();

            List<PacketSaverQueue> queues = getQueue(first);
            if (queues == null) return null;

            for (PacketSaverQueue queue : queues) {
                if (queue.getWindow().getPacketTypes().equals(packetTypeSet) && !queue.isEmpty())
                    return get(queue, remove);
            }
        } finally {
            lock.readLock().unlock();
        }
        return null;
    }

    public List<PacketContainer<?>> get(Set<PacketTypeEnum> packetTypeSet, int amount, boolean remove) {
        if (amount == 0) return Collections.emptyList();

        lock.readLock().lock();
        try {
            filterPacketTypes(packetTypeSet);

            PacketTypeEnum first = packetTypeSet.iterator().next();

            List<PacketSaverQueue> queues = getQueue(first);
            if (queues == null) return null;

            PacketSaverQueue largestQueue = null;
            for (PacketSaverQueue queue : queues) {
                if (queue.getWindow().getPacketTypes().equals(packetTypeSet)
                        && (largestQueue == null || largestQueue.size() < queue.size())) {
                    largestQueue = queue;
                }
            }
            if (largestQueue != null)
                return getPackets(largestQueue, amount, remove);
            else
                return Collections.emptyList();
        } finally {
            lock.readLock().unlock();
        }
    }

    private List<PacketContainer<?>> getPackets(PacketSaverQueue queue, int amount, boolean remove) {
        List<PacketContainer<?>> packets = remove ? pollPackets(queue, amount, false) : peekPackets(queue, amount, false);
        Collections.reverse(packets);
        return packets;
    }

    private List<PacketContainer<?>> pollPackets(PacketSaverQueue queue, int amount, boolean fromHead) {
        List<PacketContainer<?>> packets = new LinkedList<>();
        PacketContainer<?> packet;
        while ((packet = poll(queue, fromHead)) != null) {
            packets.add(packet);

            if (packets.size() == amount) return packets;
        }
        return packets;
    }

    private List<PacketContainer<?>> peekPackets(PacketSaverQueue queue, int amount, boolean fromHead) {
        List<PacketContainer<?>> packets = new LinkedList<>();

        List<PacketContainer<?>> queuePackets = new ArrayList<>(queue.getPackets());
        if (!fromHead) Collections.reverse(queuePackets);

        for (PacketContainer<?> packet : queuePackets) {
            packets.add(packet);

            if (packets.size() == amount) return packets;
        }
        return packets;
    }

    private void filterPacketTypes(Set<PacketTypeEnum> packetTypes) {
        Set<PacketTypeEnum> filteredPacketTypes = PacketBundleFilters.FILTER_ALL_PACKETS;
        packetTypes.removeIf(filteredPacketTypes::contains);
    }

    private PacketContainer<? extends WrappedPacket<?>> get(PacketSaverQueue queue, boolean remove) {
        if (!remove) return queue.getPackets().peek();

        return poll(queue, true);
    }

    private PacketContainer<? extends WrappedPacket<?>> poll(PacketSaverQueue queue, boolean fromHead) {
        PacketContainer<? extends WrappedPacket<?>> packet = fromHead ? queue.getPackets().poll() : queue.getPackets().pollLast();
        removeFromSavers(packet);

        return packet;
    }

    public void removeFromSavers(PacketContainer<?> packet) {
        if (packet == null) return;

        List<PacketSaverQueue> queues = getQueue(packet.getType());
        if (queues == null) return;

        for (PacketSaverQueue queue : queues) {
            queue.getPackets().remove(packet);
        }
    }

    public void clear() {
        lock.readLock().lock();
        try {
            for (Deque<PacketContainer<?>> packetList : packetSaves.values()) {
                packetList.clear();
            }
        } finally {
            lock.readLock().unlock();
        }
    }
}
