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
    private long recentDecodeStart;
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
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> list) throws Exception {
        packetProcessor.callDecode(player, byteBuf);

        if (player != null && player.isOnline()) recentDecodeStart = System.currentTimeMillis();

        list.add(byteBuf.readBytes(byteBuf.readableBytes()));
    }

    public long getRecentDecodeStart() {
        return recentDecodeStart;
    }
}
