package me.alek.honey.netty.injector.connection;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Version;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.util.NMS;
import me.alek.honey.netty.util.NetProtocol;
import me.alek.honey.util.LibVersion;

import java.util.Map;

public class BeginInitProtocolInjector extends ChannelInboundHandlerAdapter {

    public static boolean CHECKED_NETTY_VERSION;
    public static LibVersion NETTY_VERSION;
    public static final LibVersion MODERN_NETTY_VERSION = new LibVersion(4, 1, 24);

    private static LibVersion resolveNettyVersion() {
        Map<String, Version> nettyArtifacts = Version.identify();

        Version version = nettyArtifacts.getOrDefault("netty-common", nettyArtifacts.get("netty-all"));
        if (version != null) {

            String stringVersion = version.artifactVersion();
            stringVersion = stringVersion.replaceAll("[^\\d.]", "");

            if (stringVersion.endsWith(".")) {
                stringVersion = stringVersion.substring(0, stringVersion.length() - 1);
            }
            return new LibVersion(stringVersion);
        }
        return null;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof Channel) || NMS.isFakeChannel((Channel) msg)) return;

        Channel channel = (Channel) msg;
        //Resolve netty version only once.
        if (NETTY_VERSION == null && !CHECKED_NETTY_VERSION) {
            NETTY_VERSION = resolveNettyVersion();
            CHECKED_NETTY_VERSION = true;
        }

        String channelInitName = HoneyPlugin.get().getPacketLibrary().getChannelInitName();

        //Depends on netty version, if we cannot resolve that we just check server version.
        if ((NETTY_VERSION != null && NETTY_VERSION.isNewerThan(MODERN_NETTY_VERSION))
                || NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_12)) {
            channel.pipeline().addLast(channelInitName, new EndInitProtocolInjector.Initializer_v1_12());
        } else {
            channel.pipeline().addLast(channelInitName, new EndInitProtocolInjector.Initializer_v1_8());
        }

        super.channelRead(ctx, msg);
    }
}
