package me.alek.honey.netty.utility;

import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.packet.type.RangedPacketTypeEnum;
import org.bukkit.Bukkit;

import java.util.*;

public enum NetProtocol {
    v1_8(47), v1_8_1(47), v1_8_2(47), v1_8_3(47), v1_8_4(47), v1_8_5(47), v1_8_6(47), v1_8_7(47), v1_8_8(47),
    v1_9(107), v1_9_2(109), v1_9_4(110),
    v1_10(210), v1_10_1(210), v1_10_2(210),
    v1_11(315), v1_11_1(316), v1_11_2(316),
    v1_12(335), v1_12_1(338), v1_12_2(340),
    v1_13(393), v1_13_1(401), v1_13_2(404),
    v1_14(477), v1_14_1(480), v1_14_2(485), v1_14_3(490), v1_14_4(498),
    v1_15(573), v1_15_1(575), v1_15_2(578),
    v1_16_1(736), v1_16_2(751), v1_16_3(753), v1_16_4(754), v1_16_5(754),
    v1_17(755), v1_17_1(756),
    v1_18(757), v1_18_1(757), v1_18_2(758),
    v1_19(759), v1_19_1(760), v1_19_2(760), v1_19_3(761), v1_19_4(762),
    v1_20(763), v1_20_1(763), v1_20_2(764), v_1_20_3(765), v1_20_4(765),
    UNKNOWN(-1);

    private static NetProtocol PROTOCOL_CACHE;
    private static final HashMap<Integer, List<NetProtocol>> protocolVersionMap = new HashMap<>();
    private static final List<NetProtocol> PROTOCOLS = new ArrayList<>();
    private static final List<NetProtocol> REVERSED_PROTOCOLS = new ArrayList<>();

    static {
        REVERSED_PROTOCOLS.addAll(Arrays.asList(NetProtocol.values()));
        PROTOCOLS.addAll(REVERSED_PROTOCOLS);

        Collections.reverse(REVERSED_PROTOCOLS);

        for (NetProtocol version : PROTOCOLS) {

            int protocol = version.getProtocolVersion();
            if (!protocolVersionMap.containsKey(protocol))
                protocolVersionMap.put(protocol, new ArrayList<>());
            protocolVersionMap.get(protocol).add(version);
        }
    }

    private final int protocolVersion;

    NetProtocol(int protocol) {
        this.protocolVersion = protocol;
    }

    private static NetProtocol acquireProtocol() {
        for (NetProtocol version : REVERSED_PROTOCOLS) {
            final String name = version.name().replace("v", "").substring(1).replace("_", ".");
            if (Bukkit.getVersion().contains(name))
                return version;
        }
        NetProtocol fallbackVersion = PacketLibrary.get().getSettings().getFallbackProtocol();
        if (fallbackVersion != null)
            return fallbackVersion;

        return UNKNOWN;
    }

    public static NetProtocol getProtocol() {
        if (PROTOCOL_CACHE == null) PROTOCOL_CACHE = acquireProtocol();

        if (PROTOCOL_CACHE == NetProtocol.UNKNOWN)
            throw new RuntimeException("Could not get server version!");

        return PROTOCOL_CACHE;
    }

    public boolean isOlderThan(NetProtocol target) {
        return getProtocolVersion() < target.getProtocolVersion();
    }

    public boolean isOlderThanOrEqual(NetProtocol target) {
        return getProtocolVersion() <= target.getProtocolVersion();
    }

    public boolean isNewerThan(NetProtocol target) {
        return getProtocolVersion() > target.getProtocolVersion();
    }

    public boolean isNewerThanOrEqual(NetProtocol target) {
        return getProtocolVersion() >= target.getProtocolVersion();
    }

    public boolean isBetween(NetProtocol target1, NetProtocol target2) {
        return (isOlderThanOrEqual(target1) && isNewerThanOrEqual(target2)) || (isOlderThanOrEqual(target2) && isNewerThanOrEqual(target1));
    }

    public boolean isEqual(NetProtocol target) {
        return this == target;
    }

    public NetProtocol getBefore() {
        if (ordinal() == 0) {
            return this;
        }
        NetProtocol protocol = NetProtocol.values()[ordinal() - 1];
        if (protocol.getProtocolVersion() == getProtocolVersion())
            return protocol.getBefore();

        return protocol;
    }

    public NetProtocol getAfter() {
        if (ordinal() == REVERSED_PROTOCOLS.get(1).ordinal()) {
            return this;
        }
        NetProtocol protocol = NetProtocol.values()[ordinal() + 1];
        if (protocol.getProtocolVersion() == getProtocolVersion())
            return protocol.getAfter();

        return protocol;
    }

    public static boolean matchProtocol(RangedPacketTypeEnum rangedPacketType, NetProtocol protocol) {
        NetProtocolRange range = rangedPacketType.getRange();
        return range.match(protocol);
    }

    public static boolean matchProtocol(RangedPacketTypeEnum rangedPacketType) {
         return matchProtocol(rangedPacketType, PROTOCOL_CACHE);
    }

    public static NetProtocol getLatest() {
        return REVERSED_PROTOCOLS.get(1);
    }

    public static NetProtocol getOldest() {
        return NetProtocol.values()[0];
    }

    public int getProtocolVersion() {
        return protocolVersion;
    }
}
