package me.alek.honey.netty.injector;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.EmptyByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.modelwrappers.network.WrappedEnumProtocol;
import me.alek.honey.netty.modelwrappers.network.WrappedEnumProtocolDirection;
import me.alek.honey.netty.modelwrappers.network.WrappedNetworkManager;
import me.alek.honey.netty.modelwrappers.network.WrappedPacketDataSerializer;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.utility.reflect.ConstructorInvoker;
import me.alek.honey.netty.utility.reflect.NMSUtils;
import me.alek.honey.netty.utility.reflect.Reflection;
import me.alek.honey.services.ViolationService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class PlayerPacketDecoder extends ByteToMessageDecoder {

    private static final ConstructorInvoker<Object> packetDataSerializerInvoker = Reflection.getConstructor(NMSUtils.getPacketDataSerializerClass(), ByteBuf.class);
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
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> list) throws Exception {
        // undgå packet decode sendt fra spillere i game state der alligevel ikke er online
        if (player != null && !player.isOnline()) {
            if (byteBuf != null && byteBuf.refCnt() > 0) {
                byteBuf.clear();
            }
            return;
        }
        // ugyldig byte buf og behøver derfor ikke blive decoded
        if (byteBuf == null || byteBuf instanceof EmptyByteBuf || byteBuf.readableBytes() == 0) {
            if (byteBuf != null && byteBuf.refCnt() > 0) {
                byteBuf.clear();
            }
            HoneyPlugin.get().getViolationService().punish(player, "Netty.D");
            return;
        }
        // hvis den spiller bliver punished, skal der ikke tillades flere packets sendt fra vedkommende
        if (player != null) {
            if (HoneyPlugin.get().getViolationService().isPunishing(player)) {
                return;
            }
            // tjek om den packet størrelse er forbudt eller ej
            if (byteBuf.readableBytes() > ConfigurationOptions.Netty.MAX_BYTES_PER_PACKET) {
                if (byteBuf.refCnt() > 0) {
                    byteBuf.clear();
                }
                HoneyPlugin.get().getViolationService().punish(player, "Netty.E");
                return;
            }
            if (HoneyPlugin.get().getViolationService().flag(player, byteBuf, ViolationService.CheckKey.DEFAULT.setBuffer(true), byteBuf.readableBytes(), null, "NettyF", ConfigurationOptions.Netty.NettyF.get())) {
                return;
            }
            WrappedPacketDataSerializer dataSerializer = new WrappedPacketDataSerializer(packetDataSerializerInvoker.invoke(byteBuf), byteBuf);
            dataSerializer.skipBytes(1);

            WrappedEnumProtocol enumProtocol = Converters.getEnumProtocolConverter().convertDelegate(
                    ctx.channel().attr(WrappedNetworkManager.getEnumProtocolKey()).get()
            );
            LocalDateTime time = LocalDateTime.now();
            WrappedPacket<?> packet = enumProtocol.createPacket(WrappedEnumProtocolDirection.SERVERBOUND, dataSerializer.readVarInt());
            packet.acceptSerializer(dataSerializer);
            Bukkit.broadcastMessage("serializer tog " + Duration.between(time, LocalDateTime.now()).toMillis() + "ms");

            dataSerializer.resetReaderIndex();
        }
        list.add(byteBuf.readBytes(byteBuf.readableBytes()));
    }
}
