package me.alek.honey.netty.injector;

import io.netty.channel.Channel;
import org.bukkit.entity.Player;

public interface AbstractNettyChannelInjector {

    void inject();

    void eject();

    void injectPlayer(Player player, boolean reallyInject);

    void ejectPlayer(Player player);

    boolean hasInjected(Player player);

    void writePacket(Player player, Object rawPacket);

    void writePacket(Channel channel, Object rawPacket);

    void flushPackets(Player player);

    void flushPackets(Channel channel);

    void receivePacket(Player player, Object rawPacket);

    void receivePacket(Channel channel, Object rawPacket);

    AbstractPipelineProxy getHandler(Channel channel);

    AbstractPipelineProxy getDecoder(Channel channel);

}

