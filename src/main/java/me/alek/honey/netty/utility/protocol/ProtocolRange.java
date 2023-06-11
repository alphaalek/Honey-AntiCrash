package me.alek.honey.netty.utility.protocol;

public class ProtocolRange {

    public static final ProtocolRange ALL = ProtocolRange.since(Protocol.getOldest());

    public static ProtocolRange since(Protocol protocol) {
        return new ProtocolRange(protocol, Protocol.getLatest());
    }

    public static ProtocolRange after(Protocol protocol) {
        return new ProtocolRange(protocol.getAfter(), Protocol.getLatest());
    }

    public static ProtocolRange between(Protocol protocol1, Protocol protocol2) {
        return new ProtocolRange(protocol1, protocol2);
    }

    public static ProtocolRange betweenWithout(Protocol protocol1, Protocol protocol2) {
        return new ProtocolRange(protocol1.getAfter(), protocol2.getBefore());
    }

    public static ProtocolRange until(Protocol protocol) {
        return new ProtocolRange(Protocol.getOldest(), protocol);
    }

    public static ProtocolRange before(Protocol protocol) {
        return new ProtocolRange(Protocol.getOldest(), protocol.getBefore());
    }

    private final Protocol bound;
    private final Protocol to;

    private ProtocolRange(Protocol bound, Protocol to) {
        this.bound = bound;
        this.to = to;
    }

    public Protocol getBound() {
        return bound;
    }

    public Protocol getTo() {
        return to;
    }

    public boolean match(Protocol protocol) {
        return protocol.isBetween(bound, to);
    }
}
