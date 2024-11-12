package me.alek.honey.util.protocol;

import org.bukkit.Bukkit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public enum NetProtocol {
    v1_8(47), v1_8_1(47), v1_8_2(47), v1_8_3(47), v1_8_4(47), v1_8_5(47), v1_8_6(47), v1_8_7(47), v1_8_8(47),
    v1_9(107), v1_9_1(108), v1_9_2(109), v1_9_3(110), v1_9_4(110),
    v1_10(210), v1_10_1(210), v1_10_2(210),
    v1_11(315), v1_11_1(316), v1_11_2(316),
    v1_12(335), v1_12_1(338), v1_12_2(340),
    v1_13(393), v1_13_1(401), v1_13_2(404),
    v1_14(477), v1_14_1(480), v1_14_2(485), v1_14_3(490), v1_14_4(498),
    v1_15(573), v1_15_1(575), v1_15_2(578),
    v1_16(735), v1_16_1(736), v1_16_2(751), v1_16_3(753), v1_16_4(754), v1_16_5(754),
    v1_17(755), v1_17_1(756),
    v1_18(757), v1_18_1(757), v1_18_2(758),
    v1_19(759), v1_19_1(760), v1_19_2(760), v1_19_3(761), v1_19_4(762),
    v1_20(763), v1_20_1(763), v1_20_2(764), v1_20_3(765), v1_20_4(765), v1_20_5(766), v1_20_6(766),

    LATEST(-1), OLDEST(-1), UNKNOWN(-1);

    private static final List<NetProtocol> invalid = Arrays.asList(LATEST, OLDEST, UNKNOWN);
    private static NetProtocol PROTOCOL;

    public static NetProtocol get(NetProtocol protocol) {
        if (protocol == LATEST) {
            return getLatest();
        } else if (protocol == OLDEST) {
            return getOldest();
        } else if (protocol == UNKNOWN) {
            return null;
        } else {
            return protocol;
        }
    }

    public static NetProtocol getByName(String name) {
        return Arrays.stream(NetProtocol.values()).filter(p -> p.getName().equals(name)).findFirst().orElse(null);
    }

    public static List<NetProtocol> getProtocols() {
        return Arrays.stream(NetProtocol.values()).filter(p -> p.protocolVersion != -1).collect(Collectors.toList());
    }

    private final int protocolVersion;

    NetProtocol(int protocol) {
        this.protocolVersion = protocol;
    }

    private static List<NetProtocol> getReversedProtocols() {
        List<NetProtocol> protocols = getProtocols();
        Collections.reverse(protocols);
        return protocols;
    }

    private static NetProtocol acquireProtocol() {
        if (PROTOCOL != null) {
            return PROTOCOL;
        }

        for (NetProtocol version : getReversedProtocols()) {
            if (Bukkit.getServer().getVersion().contains(version.getName())) {
                return version;
            }
        }

        return UNKNOWN;
    }

    public static NetProtocol getMajorFrom(int protocolVersion) {
        for (NetProtocol protocol : getReversedProtocols()) {
            if (protocolVersion >= protocol.getProtocolVersion()
                    && (protocolVersion < protocol.getAfter().getProtocolVersion()
                        || protocol.getAfter().isEqual(protocol))) {
                return protocol;
            }
        }

        return null;
    }

    public static NetProtocol getProtocol() {
        if (PROTOCOL == null) {
            PROTOCOL = acquireProtocol();
        }

        if (PROTOCOL == NetProtocol.UNKNOWN) {
            throw new RuntimeException("Could not get server version!");
        }

        return PROTOCOL;
    }

    @Override
    public String toString() {
        return name().substring(1);
    }

    public String getName() {
        return toString().replace("_", ".");
    }


    public int getProtocolVersion() {
        return Optional.ofNullable(get(this)).map(p -> p.protocolVersion).orElse(-1);
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
        return isOlderThanOrEqual(target1) && isNewerThanOrEqual(target2)
                || isOlderThanOrEqual(target2) && isNewerThanOrEqual(target1);
    }

    public boolean isEqual(NetProtocol target) {
        return getProtocolVersion() == target.getProtocolVersion();
    }

    public NetProtocol getBefore() {
        if (ordinal() == 0) {
            return this;
        }
        NetProtocol protocol = NetProtocol.values()[ordinal() - 1];
        if (protocol.getProtocolVersion() == getProtocolVersion()) {
            return protocol.getBefore();
        }

        return protocol;
    }

    public NetProtocol getAfter() {
        if (ordinal() == getProtocols().size() - 1) {
            return this;
        }
        NetProtocol protocol = NetProtocol.values()[ordinal() + 1];
        if (protocol.getProtocolVersion() == getProtocolVersion()) {
            return protocol.getAfter();
        }

        return protocol;
    }

    public static boolean matchProtocol(NetProtocolRange.Ranged range) {
         return range.getRange().match(PROTOCOL);
    }

    public static NetProtocol getLatest() {
        return getProtocols().get(getProtocols().size() - 1);
    }

    public static NetProtocol getOldest() {
        return getProtocols().get(0);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE_USE)
    public @interface From {

        NetProtocol from();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE_USE)
    public @interface To {

        NetProtocol to();
    }
}
