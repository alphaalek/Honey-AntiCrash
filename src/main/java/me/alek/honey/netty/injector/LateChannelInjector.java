package me.alek.honey.netty.injector;

import io.netty.channel.Channel;
import me.alek.honey.netty.api.NettyChannelProxy;
import me.alek.honey.netty.api.event.impl.inject.InjectEvent;
import org.bukkit.entity.Player;

public class LateChannelInjector implements NettyChannelProxy {


    @Override
    public void inject() {
        CommonChannelInjector.inject(InjectEvent.InjectCallback.SUCCESS, InjectEvent.InjectType.LATE, InjectEvent.InjectBound.SERVER);
    }

    @Override
    public void eject() {
    }

    @Override
    public void injectPlayer(Player player, boolean reallyInject) {
        CommonChannelInjector.injectPlayer(player, InjectEvent.InjectType.LATE, reallyInject);
    }

    @Override
    public void ejectPlayer(Player player) {
        CommonChannelInjector.ejectPlayer(player, InjectEvent.InjectType.LATE);
    }

    public boolean hasInjected(Player player) {
        return CommonChannelInjector.hasInjected(player);
    }

    @Override
    public void writePacket(Player player, Object rawPacket) {
        CommonChannelInjector.writePacket(player, rawPacket);
    }

    @Override
    public void writePacket(Channel channel, Object rawPacket) {
        CommonChannelInjector.writePacket(channel, rawPacket);
    }

    @Override
    public void flushPackets(Player player) {
        CommonChannelInjector.flushPackets(player);
    }

    @Override
    public void flushPackets(Channel channel) {
        CommonChannelInjector.flushPackets(channel);
    }

    @Override
    public void receivePacket(Player player, Object rawPacket) {
        CommonChannelInjector.receivePacket(player, rawPacket);
    }

    @Override
    public void receivePacket(Channel channel, Object rawPacket) {
        CommonChannelInjector.receivePacket(channel, rawPacket);
    }

    @Override
    public PlayerChannelDuplexHandler getHandler(Channel channel) {
        return CommonChannelInjector.getHandler(channel);
    }

    @Override
    public PlayerPacketDecoder getDecoder(Channel channel) {
        return CommonChannelInjector.getDecoder(channel);
    }
}
