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
    private double recentDecodeStart = System.currentTimeMillis();
    private double recentDeltaReceived = 0;
    private boolean hasRead;
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

        double currentTime = System.currentTimeMillis();;
        recentDeltaReceived = currentTime - recentDecodeStart;
        recentDecodeStart = currentTime;

        list.add(byteBuf.readBytes(byteBuf.readableBytes()));
    }


    public double getRecentDeltaReceived() {
        return recentDeltaReceived;
    }

    public double getRecentDeltaDecode() {
        return System.currentTimeMillis() - recentDecodeStart;
    }

    public double getRecentStartDecode() {
        return recentDecodeStart;
    }
}
