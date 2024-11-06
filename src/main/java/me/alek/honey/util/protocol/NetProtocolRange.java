package me.alek.honey.util.protocol;

public class NetProtocolRange {

    public static final NetProtocolRange ALL = NetProtocolRange.since(NetProtocol.getOldest());

    public static NetProtocolRange since(NetProtocol protocol) {
        return new NetProtocolRange(protocol, NetProtocol.getLatest());
    }

    public static NetProtocolRange until(NetProtocol protocol) {
        return new NetProtocolRange(NetProtocol.getOldest(), protocol);
    }

    public static NetProtocolRange between(NetProtocol protocol1, NetProtocol protocol2) {
        return new NetProtocolRange(protocol1, protocol2);
    }

    public static NetProtocolRange only(NetProtocol protocol) {
        return new NetProtocolRange(protocol, protocol);
    }

    public static boolean matchProtocol(NetProtocol.To to, NetProtocol.From from) {
        return matchProtocol(NetProtocol.getProtocol(), to, from);
    }

    public static boolean matchProtocol(NetProtocol protocol, NetProtocol.To to, NetProtocol.From from) {
        NetProtocolRange range = NetProtocolRange.ALL;
        if (from != null && to != null) {
            range = NetProtocolRange.between(from.from(), to.to());
        } else if (from != null) {
            range = NetProtocolRange.since(from.from());
        } else if (to != null) {
            range = NetProtocolRange.until(to.to());
        }

        return range.match(protocol);
    }

    private final NetProtocol bound;
    private final NetProtocol to;

    private NetProtocolRange(NetProtocol bound, NetProtocol to) {
        if (bound == NetProtocol.OLDEST) {
            bound = NetProtocol.getOldest();
        }
        this.bound = bound;

        if (to == NetProtocol.LATEST) {
            to = NetProtocol.getLatest();
        }
        this.to = to;
    }

    public NetProtocol getBound() {
        return bound;
    }

    public NetProtocol getTo() {
        return to;
    }

    public boolean match(NetProtocol protocol) {
        return protocol.isBetween(bound, to);
    }

    public interface Ranged {

        NetProtocolRange getRange();
    }
}