package me.alek.honey.netty.injector;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Version;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.util.LibraryVersion;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.MinecraftReflection;

import java.util.Map;

public class BeginInitProtocolInjector extends ChannelInboundHandlerAdapter {

    public static volatile boolean CHECKED_NETTY_VERSION;
    public static volatile LibraryVersion NETTY_VERSION;
    public static final LibraryVersion MODERN_NETTY_VERSION = new LibraryVersion(4, 1, 24);

    private static LibraryVersion resolveNettyVersion() {
        Map<String, Version> nettyArtifacts = Version.identify();

        Version version = nettyArtifacts.getOrDefault("netty-common", nettyArtifacts.get("netty-all"));
        if (version != null) {
            String strVer = version.artifactVersion()
                    .replaceAll("[^\\d.]", "");
            if (strVer.endsWith(".")) {
                strVer = strVer.substring(0, strVer.length() - 1);
            }
            return new LibraryVersion(strVer);
        }

        return null;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof Channel) || MinecraftReflection.isFakeChannel((Channel) msg)) {
            return;
        }
        Channel channel = (Channel) msg;

        // resolve netty version
        if (NETTY_VERSION == null && !CHECKED_NETTY_VERSION) {
            NETTY_VERSION = resolveNettyVersion();
            CHECKED_NETTY_VERSION = true;
        }

        String channelInitName = HoneyPlugin.get().getPacketLibrary().getChannelInitName();
        // Depends on netty version, if we can't resolve that, we just check server version.
        if ((NETTY_VERSION != null && NETTY_VERSION.isNewerThan(MODERN_NETTY_VERSION))
                || NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_12)) {
            channel.pipeline().addLast(channelInitName, new EndInitProtocolInjector.Initializer_v1_12());
        } else {
            channel.pipeline().addLast(channelInitName, new EndInitProtocolInjector.Initializer_v1_8());
        }

        super.channelRead(ctx, msg);
    }
}
