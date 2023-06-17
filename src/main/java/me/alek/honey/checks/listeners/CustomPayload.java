package me.alek.honey.checks.listeners;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import me.alek.honey.checks.*;
import me.alek.honey.checks.helper.ItemHelper;
import me.alek.honey.checks.impl.PayloadA;
import me.alek.honey.checks.impl.PayloadB;
import me.alek.honey.checks.impl.PayloadC;
import me.alek.honey.checks.impl.PayloadD;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInCustomPayload;
import me.alek.honey.netty.utility.reflect.ConstructorInvoker;
import me.alek.honey.netty.utility.reflect.MethodInvoker;
import me.alek.honey.netty.utility.reflect.Reflection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class CustomPayload extends AbstractPacketListener<WrappedPlayInCustomPayload> implements IPacketListener {

    private final Class<?> PACKET_SERIALIZER_CLASS = Reflection.getFuzzyClass("{nms}.PacketDataSerializer", "{nms}.network.PacketDataSerializer");
    private final ConstructorInvoker PACKET_SERIALIZER_CONSTRUCTOR = Reflection.getConstructor(PACKET_SERIALIZER_CLASS, ByteBuf.class);
    private final MethodInvoker ITEMSTACK_SERIALIZER_INVOKER = Reflection.getMethodFromReturnType(PACKET_SERIALIZER_CLASS, 0, ItemHelper.getNMSItemStackClass());

    @Override
    public boolean onPrePacketReceive(Player player, PacketContainer<WrappedPlayInCustomPayload> packetContainer, CheckContextContainer container) {
        ByteBuf priorSerializer;
        try {
            priorSerializer = (ByteBuf) packetContainer.getPacket().getDataSerializer();
        } catch (Exception ex) {
            return false;
        }
        ByteBuf newSerializer = (ByteBuf) PACKET_SERIALIZER_CONSTRUCTOR.invoke(Unpooled.wrappedBuffer(priorSerializer));

        ItemStack bukkitItem = null;
        try {
            Object item = ITEMSTACK_SERIALIZER_INVOKER.invokeWithException(newSerializer);
            bukkitItem = ItemHelper.asBukkitItem(item);
        } catch (Exception ex) {
        }

        container.putSerializer(priorSerializer);
        container.putItem(bukkitItem);

        return true;
    }

    @Override
    public void onPostPacketReceive(Player player, PacketContainer<WrappedPlayInCustomPayload> packetContainer) {

    }

    @Override
    public CheckVisitor<WrappedPlayInCustomPayload> getVisitor() {
        CheckVisitor<WrappedPlayInCustomPayload> visitor = new CheckVisitor<>();
        visitor.visit(new PayloadA());
        visitor.visit(new PayloadB());
        visitor.visit(new PayloadC());
        visitor.visit(new PayloadD());
        return visitor;
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> Collections.singletonList(PacketType.Play.Client.CUSTOM_PAYLOAD);
    }

}
