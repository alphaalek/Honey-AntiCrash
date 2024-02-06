package me.alek.honey.netty.packet;

import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.stream.Collectors;

public class PacketBundleSaver {

    private static final Map<Set<PacketTypeEnum>, Integer> PACKET_SAVES = PacketBundleFilters.getPacketSaves();
    private static final byte MAX_PACKET_ID;

    static {
        byte max = 0;
        for (PacketTypeEnum packetType : PACKET_SAVES.keySet().stream().flatMap(Set::stream).collect(Collectors.toSet())) {
            if (packetType.getPacketId() > max)
                max = packetType.getPacketId();
        }
        MAX_PACKET_ID = max;
    }

    private final List<PacketSaverQueue>[] packets;
    private final LinkedList<PacketContainer<?>> commonPackets = new LinkedList<>();

    public PacketBundleSaver() {
        packets = new List[MAX_PACKET_ID];

        for (Set<PacketTypeEnum> packetTypeSet : PACKET_SAVES.keySet()) {
            for (PacketTypeEnum packetType : packetTypeSet) {
                List<PacketSaverQueue> packets = this.packets[packetType.getPacketId() - 1];
                if (packets == null) {
                    this.packets[packetType.getPacketId() - 1] = (packets = new ArrayList<>());
                }
                boolean hasForPacketTypes = false;
                for (PacketSaverQueue queue : packets) {
                    if (queue.packetTypes.equals(packetTypeSet)) {
                        hasForPacketTypes = true;
                        break;
                    }
                }
                if (!hasForPacketTypes) {
                    int size = PACKET_SAVES.get(packetTypeSet);
                    if (size == 0) continue;

                    packets.add(new PacketSaverQueue(packetTypeSet, size));
                }
            }
        }
    }

    private static class PacketSaverQueue {

        private final Set<PacketTypeEnum> packetTypes;
        private final int size;

        private final Deque<PacketContainer<?>> packets = new LinkedList<>();

        public PacketSaverQueue(Set<PacketTypeEnum> packetTypes, int size) {
            this.packetTypes = packetTypes;
            this.size = size;
        }

        public void add(PacketContainer<?> packet) {
            if (!packetTypes.contains(packet.getType())) return;

            packets.add(packet);
            if (packets.size() > size) packets.remove();
        }
    }

    private List<PacketSaverQueue> getQueue(PacketTypeEnum packetType) {
        if (packetType.getPacketId() > MAX_PACKET_ID) return null;

        return packets[packetType.getPacketId() - 1];
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
            PacketContainer<?> packet = getAndRemoveFromOther(queue);
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
            while ((packet = getAndRemoveFromOther(queue)) != null) {
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
        return null;
    }

    public <WP extends WrappedPacket<WP>> PacketContainer<WP> get(Set<PacketTypeEnum> packetTypeSet) {
        PacketTypeEnum first = packetTypeSet.iterator().next();

        List<PacketSaverQueue> queues = getQueue(first);
        if (queues == null) return null;

        for (PacketSaverQueue queue : queues) {
            if (queue.packetTypes.equals(packetTypeSet))
                return getAndRemoveFromOther(queue);
        }
        return null;
    }

    public List<PacketContainer<?>> get(Set<PacketTypeEnum> packetTypeSet, int amount) {
        PacketTypeEnum first = packetTypeSet.iterator().next();

        List<PacketSaverQueue> queues = getQueue(first);
        if (queues == null || amount == 0) return null;

        List<PacketContainer<?>> packets = new ArrayList<>();
        for (PacketSaverQueue queue : queues) {
            if (queue.packetTypes.equals(packetTypeSet)) {
                PacketContainer<?> packet;
                while ((packet = getAndRemoveFromOther(queue)) != null) {
                    packets.add(packet);

                    if (packets.size() == amount) return packets;
                }
            }
        }
        return packets;
    }

    private <WP extends WrappedPacket<WP>> PacketContainer<WP> getAndRemoveFromOther(PacketSaverQueue queue) {
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
        return (PacketContainer<WP>) commonPackets.poll();
    }
}
