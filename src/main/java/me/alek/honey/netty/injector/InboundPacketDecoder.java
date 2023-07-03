package me.alek.honey.netty.injector;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.EmptyByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.modelwrappers.network.WrappedEnumProtocol;
import me.alek.honey.netty.modelwrappers.network.WrappedEnumProtocolDirection;
import me.alek.honey.netty.modelwrappers.network.WrappedNetworkManager;
import me.alek.honey.netty.modelwrappers.network.WrappedPacketDataSerializer;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.utility.reflect.ConstructorInvoker;
import me.alek.honey.netty.utility.reflect.NMSUtils;
import me.alek.honey.netty.utility.reflect.Reflection;
import me.alek.honey.services.ViolationService;
import org.bukkit.entity.Player;

import java.util.List;

public class InboundPacketDecoder extends ByteToMessageDecoder {

    private static final ConstructorInvoker<Object> packetDataSerializerInvoker = Reflection.getConstructor(NMSUtils.getPacketDataSerializerClass(), ByteBuf.class);
    private volatile Player player;

    public InboundPacketDecoder() {

    }

    public InboundPacketDecoder(Player player) {
        this.player = player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> list) throws Exception {
        // undgå packet decode sendt fra spillere i game state der alligevel ikke er online
        try {
            if (player != null && !player.isOnline()) {
                if (byteBuf != null && byteBuf.refCnt() > 0) {
                    byteBuf.clear();
                }
                return;
            }
            // ugyldig byte buf og behøver derfor ikke blive decoded
            if (ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyD.class).isEnabled()) {
                if (player != null && player.isOnline() && (byteBuf instanceof EmptyByteBuf || byteBuf.readableBytes() == 0)) {
                    if (byteBuf.refCnt() > 0) {
                        byteBuf.clear();
                    }
                    NMSUtils.getChannel(player).close();
                    return;
                }
            }
            // hvis den spiller bliver punished, skal der ikke tillades flere packets sendt fra vedkommende
            if (player != null) {
                if (HoneyPlugin.get().getViolationService().isPunishing(player)) {
                    return;
                }
                // tjek om den packet størrelse er forbudt
                if (ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyE.class).isEnabled()) {

                    int readableBytes = byteBuf.readableBytes();
                    if (readableBytes > ConfigurationOptions.Netty.NettyE.MAX) {
                        if (byteBuf.refCnt() > 0) {
                            byteBuf.clear();
                        }
                        HoneyPlugin.get().getViolationService().punish(player, "Netty.E");
                        return;
                    }
                }
                // tjek packet buffer
                if (ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyF.class).isEnabled()) {
                    if (HoneyPlugin.get().getViolationService().flag(player, byteBuf, ViolationService.CheckKey.DEFAULT.clone().setBuffer(true), byteBuf.readableBytes(), null, "Netty.F", ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyF.class))) {
                        return;
                    }
                }
                // tjek ugyldig packet
                ByteBuf wrappedByteBuf = Unpooled.wrappedBuffer(byteBuf);
                WrappedPacketDataSerializer dataSerializer = new WrappedPacketDataSerializer(packetDataSerializerInvoker.invoke(wrappedByteBuf), wrappedByteBuf);

                WrappedEnumProtocol enumProtocol = Converters.getEnumProtocolConverter().convertDelegate(
                        ctx.channel().attr(WrappedNetworkManager.getEnumProtocolKey()).get()
                );

                int id = dataSerializer.readVarInt();
                Object packet = enumProtocol.createPacket(WrappedEnumProtocolDirection.SERVERBOUND, id);

                if (packet == null && ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyG.class).isEnabled()) {
                    if (byteBuf.refCnt() > 0) {
                        byteBuf.clear();
                    }
                    HoneyPlugin.get().getViolationService().punish(player, "Netty.G");
                    return;
                }
                // tjek heavy serializer
                if (packet != null && ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyH.class).isEnabled()) {
                    int bufferIndex = 0;
                    while (dataSerializer.isReadable()) {
                        bufferIndex += Math.abs(dataSerializer.readByte());
                    }
                    if (bufferIndex > ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyH.class).getThreshold()) {
                        if (byteBuf.refCnt() > 0) {
                            byteBuf.clear();
                        }
                        HoneyPlugin.get().getViolationService().flag(player, byteBuf, new ViolationService.CheckKey(this, "NettyH"), 1, null, "Netty.H", ConfigurationOptions.getVlSetting(ConfigurationOptions.Netty.NettyH.class));
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        list.add(byteBuf.readBytes(byteBuf.readableBytes()));
    }
}
