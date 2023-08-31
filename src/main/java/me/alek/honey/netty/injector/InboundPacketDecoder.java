package me.alek.honey.netty.injector;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.api.packet.PacketProcessor;
import org.bukkit.entity.Player;

import java.util.List;

public class InboundPacketDecoder extends ByteToMessageDecoder implements AbstractPipelineProxy {

    private static final PacketProcessor packetProcessor = PacketLibrary.get().getPacketProcessor();
    private double recentDecodeStart = getCurrentTime();
    private double recentDecodeDelta = 0;
    private Player player;

    public InboundPacketDecoder(Player player) {
        this.player = player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> list) {
        packetProcessor.callDecode(player, byteBuf);

        double currentTime = getCurrentTime();
        recentDecodeDelta = currentTime - recentDecodeStart;

        if (player != null && player.isOnline()) recentDecodeStart = currentTime;

        list.add(byteBuf.readBytes(byteBuf.readableBytes()));
    }

    public double getCurrentTime() {
        return ((double)System.nanoTime()) / 1_000;
    }

    public double getRecentDeltaReceived() {
        return recentDecodeDelta;
    }

    public double getRecentDecodeDelta() {
        return getCurrentTime() - recentDecodeStart;
    }

    public double getRecentDecodeStart() {
        return recentDecodeStart;
    }
}
