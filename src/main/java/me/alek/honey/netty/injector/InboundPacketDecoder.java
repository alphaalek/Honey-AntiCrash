package me.alek.honey.netty.injector;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.api.packet.PacketProcessor;
import me.alek.honey.services.LoggerService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public class InboundPacketDecoder extends ByteToMessageDecoder implements AbstractPipelineProxy {

    private static final PacketProcessor packetProcessor = PacketLibrary.get().getPacketProcessor();
    private double recentDecodeStart = getCurrentTime();
    private double recentDeltaReceived = 0;
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
        recentDeltaReceived = currentTime - recentDecodeStart;
        recentDecodeStart = currentTime;

        list.add(byteBuf.readBytes(byteBuf.readableBytes()));
    }

    public double getCurrentTime() {
        return ((double)System.nanoTime()) / 1_000_000;
    }

    public double getRecentDeltaReceived() {
        return recentDeltaReceived;
    }

    public double getRecentDeltaDecode() {
        return getCurrentTime() - recentDecodeStart;
    }

    public double getRecentStartDecode() {
        return recentDecodeStart;
    }
}
