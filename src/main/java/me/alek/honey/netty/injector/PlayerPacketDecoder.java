package me.alek.honey.netty.injector;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.EmptyByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import org.bukkit.entity.Player;

import java.util.List;

public class PlayerPacketDecoder extends ByteToMessageDecoder {

    private volatile Player player;

    public PlayerPacketDecoder() {

    }

    public PlayerPacketDecoder(Player player) {
        this.player = player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        // undgå packet decode sendt fra spillere i game state der alligevel ikke er online
        if (player != null && !player.isOnline()) {
            violate(player, byteBuf);
            return;
        }
        // ugyldig byte buf og behøver derfor ikke blive decoded
        if (byteBuf == null || byteBuf instanceof EmptyByteBuf || byteBuf.readableBytes() == 0) {
            violate(player, byteBuf);
            return;
        }
        // hvis den spiller bliver punished, skal der ikke tillades flere packets sendt fra vedkommende
        if (player != null) {
            if (HoneyPlugin.get().getViolationService().isPunishing(player)) {
                return;
            }
        }
        // tjek om den packet størrelse er forbudt eller ej
        if (byteBuf.readableBytes() > ConfigurationOptions.Netty.MAX_BYTES_PER_PACKET) {
            violate(player, byteBuf);
            return;
        }
        list.add(byteBuf.readBytes(byteBuf.readableBytes()));
    }

    public void violate(Player player, ByteBuf byteBuf) {
        HoneyPlugin.get().getViolationService().punish(player, "NettyD");
        if (byteBuf != null && byteBuf.refCnt() > 0) {
            byteBuf.clear();
        }
    }
}
