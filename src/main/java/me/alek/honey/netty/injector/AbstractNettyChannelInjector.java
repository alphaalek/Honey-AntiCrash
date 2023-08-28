package me.alek.honey.netty.injector;

import io.netty.channel.Channel;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import org.bukkit.entity.Player;

public interface AbstractNettyChannelInjector {

    void inject();

    void eject();

    void injectPlayer(Player player, boolean reallyInject);

    void ejectPlayer(Player player);

    boolean hasInjected(Player player);

    void writePacket(Player player, Object rawPacket);
    <WP extends WrappedPacket<WP>> void writePacket(Player player, WrappedPacket<WP> packet);

    void writePacket(Channel channel, Object rawPacket);
    <WP extends WrappedPacket<WP>> void writePacket(Channel channel, WrappedPacket<WP> packet);

    void flushPackets(Player player);

    void flushPackets(Channel channel);

    void receivePacket(Player player, Object rawPacket);

    void receivePacket(Channel channel, Object rawPacket);

    AbstractPipelineProxy getHandler(Channel channel);

    AbstractPipelineProxy getDecoder(Channel channel);

}

