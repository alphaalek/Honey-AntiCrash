package me.alek.honey.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.packet.DummyPacketContainer;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import org.bukkit.Bukkit;

import java.util.List;

@ChannelHandler.Sharable
public class ViaClientboundInterceptor extends MessageToMessageEncoder<ByteBuf> {

    public static final String NAME = "honey-via-clientbound-interceptor";

    private final HoneyPacketHandler packetHandler;

    public ViaClientboundInterceptor(HoneyPacketHandler packetHandler) {
        this.packetHandler = packetHandler;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> list) {
        PacketDataSerializer serializer = new PacketDataSerializer(buf);
        PacketType type = this.packetHandler.getPacketType(serializer, true, this.packetHandler.getSimulatedClientboundProtocolState(), PacketBound.CLIENT, true);
        if (!PacketTypes.isRegistered(type)) {
            this.packetHandler.getProtocolSwapHandler().swapProtocolOutbound(DummyPacketContainer.create(type, this.packetHandler.getPlayer()));
        }

        list.add(buf.retain());
    }
}
