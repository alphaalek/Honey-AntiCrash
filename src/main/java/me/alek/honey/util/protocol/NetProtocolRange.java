package me.alek.honey.util.protocol;

import java.util.ArrayList;
import java.util.List;

public class NetProtocolRange {

    public static final NetProtocolRange ALL = NetProtocolRange.since(NetProtocol.getOldest());

    public static NetProtocolRange all() {
        return NetProtocolRange.since(NetProtocol.getOldest());
    }

    public static NetProtocolRange since(NetProtocol protocol) {
        return new NetProtocolRange(protocol, NetProtocol.getLatest());
    }

    public static NetProtocolRange until(NetProtocol protocol) {
        return new NetProtocolRange(NetProtocol.getOldest(), protocol);
    }

    public static NetProtocolRange between(NetProtocol from, NetProtocol to) {
        return new NetProtocolRange(from, to);
    }

    public static NetProtocolRange only(NetProtocol protocol) {
        return new NetProtocolRange(protocol);
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

    private final NetProtocol from;
    private final NetProtocol to;
    private final List<NetProtocolRange> exclude = new ArrayList<>();

    private NetProtocolRange(NetProtocol from, NetProtocol to) {
        this.from = NetProtocol.get(from);
        this.to = NetProtocol.get(to);
    }

    private NetProtocolRange(NetProtocol protocol) {
        this(protocol, protocol);
    }

    public NetProtocol getFrom() {
        return this.from;
    }

    public NetProtocol getTo() {
        return this.to;
    }

    public NetProtocolRange exclude(NetProtocol protocol) {
        return exclude(NetProtocolRange.only(protocol));
    }

    public NetProtocolRange exclude(NetProtocolRange range) {
        this.exclude.add(range);
        return this;
    }

    public boolean match(NetProtocol protocol) {
        return protocol.isBetween(this.from, this.to) && this.exclude.stream().noneMatch(p -> p.match(protocol));
    }

    public boolean match() {
        return match(NetProtocol.getProtocol());
    }

    public interface Ranged {

        NetProtocolRange getRange();
    }
}