package me.alek.honey.netty.utility.protocol;

public class NetProtocolRange {

    public static final NetProtocolRange ALL = NetProtocolRange.since(NetProtocol.getOldest());

    public static NetProtocolRange since(NetProtocol protocol) {
        return new NetProtocolRange(protocol, NetProtocol.getLatest());
    }

    public static NetProtocolRange after(NetProtocol protocol) {
        return new NetProtocolRange(protocol.getAfter(), NetProtocol.getLatest());
    }

    public static NetProtocolRange between(NetProtocol protocol1, NetProtocol protocol2) {
        return new NetProtocolRange(protocol1, protocol2);
    }

    public static NetProtocolRange betweenWithout(NetProtocol protocol1, NetProtocol protocol2) {
        return new NetProtocolRange(protocol1.getAfter(), protocol2.getBefore());
    }

    public static NetProtocolRange until(NetProtocol protocol) {
        return new NetProtocolRange(NetProtocol.getOldest(), protocol);
    }

    public static NetProtocolRange before(NetProtocol protocol) {
        return new NetProtocolRange(NetProtocol.getOldest(), protocol.getBefore());
    }

    private final NetProtocol bound;
    private final NetProtocol to;

    private NetProtocolRange(NetProtocol bound, NetProtocol to) {
        this.bound = bound;
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
}
