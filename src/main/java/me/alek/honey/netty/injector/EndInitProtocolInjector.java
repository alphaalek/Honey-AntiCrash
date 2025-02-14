package me.alek.honey.netty.injector;

import io.netty.channel.*;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.ChannelCloseInspection;
import me.alek.honey.netty.codec.LegacyProtocol;
import me.alek.honey.netty.handler.MinecraftHandlers;
import me.alek.honey.netty.structure.ReflectStructure;
import org.bukkit.Bukkit;

import java.util.logging.Level;

public class EndInitProtocolInjector {

    public static class Initializer_v1_8 extends ChannelInitializer<Channel> {

        @Override
        protected void initChannel(Channel channel) {
            channel.pipeline().addLast(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel channel) {
                    handleChannelInitialization(this, channel);
                }
            });
        }
    }

    public static class Initializer_v1_12 extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            handleChannelInitialization(this, ctx.channel());

            super.channelRegistered(ctx);
        }
    }

    private static void handleChannelInitialization(ChannelHandler handler, Channel channel) {
        if (!HoneyPlugin.get().isServerStarted()) {
            // update the channel field of the connection handler, because Minecraft is poop and doesn't do this initially
            // Note, seems like it's done in newer versions, but I don't want to check which one it changed
            Object connection = channel.pipeline().get(MinecraftHandlers.CONNECTION);
            ReflectStructure.Provider.acquireStructure(connection.getClass())
                            .withType(Channel.class).withTarget(connection)
                            .writeField(0, channel);

            ChannelCloseInspection.close(channel);
            return;
        }

        try {
            HoneyPlugin.get().getPacketLibrary().getPipelineInjector().injectChannel(channel);
        } catch (Exception ex) {
            HoneyPlugin.get().getLogger().getLogger().log(Level.SEVERE, "Error occurred when initializing channel", ex);

            if (channel.pipeline().context(handler) != null) {
                channel.pipeline().remove(handler);
            }

            ChannelCloseInspection.close(channel);
        }
    }
}
