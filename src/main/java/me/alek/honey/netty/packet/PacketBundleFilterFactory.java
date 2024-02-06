package me.alek.honey.netty.packet;

import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.utility.NetProtocol;

public class PacketBundleFilterFactory {

    private static final PacketBundleFilter EMPTY_FILTER = createLenientFilter(0);
    private static final int LENIENT_FILTER_SIZE = EMPTY_FILTER.getAcceptedPacketTypes().length;

    public static int getLenientFilterSize() {
        return LENIENT_FILTER_SIZE;
    }

    public static PacketBundleFilter createSingletonFilter(PacketTypeEnum packetType) {
        return createSingletonFilter(new PacketTypeEnum[]{packetType});
    }

    public static PacketBundleFilter createSingletonFilter(PacketTypeEnum... packetType) {
        return createFilter(1, packetType);
    }

    public static PacketBundleFilter createEmptyFilter() {
        return EMPTY_FILTER;
    }

    public static PacketBundleFilter createLenientSingletonFilter() {
        return createLenientFilter(1);
    }

    public static PacketBundleFilter createLenientFilter(int size) {
        return createFilter(size);
    }

    public static PacketBundleFilter createFilter(int size, PacketTypeEnum... packetTypes) {
        if (size == 0 && EMPTY_FILTER != null) return EMPTY_FILTER;
        return new PacketBundleFilter() {
            @Override
            public int size() {
                return size;
            }

            @Override
            public PacketTypeEnum[] getAcceptedPacketTypes() {
                return packetTypes.length > 0 ? packetTypes
                        : PacketType.getTypesAvailableFor(PacketState.PLAY, PacketBound.CLIENT, NetProtocol.getProtocol()).toArray(new PacketTypeEnum[0]);
            }
        };
    }
}
