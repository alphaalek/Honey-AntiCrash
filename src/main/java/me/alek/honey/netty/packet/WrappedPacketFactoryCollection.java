package me.alek.honey.netty.packet;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.ByteWrappedPacket;
import me.alek.honey.netty.packetwrappers.ObjectWrappedPacket;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.packetwrappers.WrappedPacketFactory;
import me.alek.honey.netty.packetwrappers.handshake.client.WrappedHandshakeInSetProtocol;
import me.alek.honey.netty.packetwrappers.login.client.WrappedLoginInLoginStart;
import me.alek.honey.netty.packetwrappers.play.client.*;
import me.alek.honey.netty.packetwrappers.play.server.*;
import me.alek.honey.util.Array;

import java.util.EnumMap;
import java.util.function.BiFunction;

public class WrappedPacketFactoryCollection {

    private final static Array<WrappedPacketFactory.ObjectWrappedPacketFactory<? extends WrappedPacket<?>>> OBJECT_WRAPPERS = new Array<>();
    private final static EnumMap<PacketContainer.PipelineState, Array<WrappedPacketFactory.ByteWrappedPacketFactory<? extends WrappedPacket<?>>>> BYTE_WRAPPERS = new EnumMap<>(PacketContainer.PipelineState.class);

    public static void loadWrappers() {
        loadByteWrapper(PacketTypes.Play.Client.BLOCK_PLACE, PacketContainer.PipelineState.INITIAL_BYTE, WrappedPlayInBlockPlace::new);
        loadByteWrapper(PacketTypes.Play.Client.CREATIVE_INVENTORY_ACTION, PacketContainer.PipelineState.INITIAL_BYTE, WrappedPlayInSetCreativeSlot::new);
        loadByteWrapper(PacketTypes.Play.Client.CLICK_WINDOW, PacketContainer.PipelineState.INITIAL_BYTE, WrappedPlayInWindowClick::new);
        loadByteWrapper(PacketTypes.Play.Client.CUSTOM_PAYLOAD, PacketContainer.PipelineState.INITIAL_BYTE, WrappedPlayInCustomPayload.ByteWrappedPlayInCustomPayload::new);

        loadByteWrapper(PacketTypes.Handshake.Client.SET_PROTOCOL, PacketContainer.PipelineState.FINAL_BYTE, WrappedHandshakeInSetProtocol::new);

        loadObjectWrapper(PacketTypes.Play.Client.PLAYER_DIGGING, WrappedPlayInBlockDig::new);
        loadObjectWrapper(PacketTypes.Play.Client.CHAT_MESSAGE, WrappedPlayInChat::new);
        loadObjectWrapper(PacketTypes.Play.Client.CHAT_COMMAND, WrappedPlayInChat::new);
        loadObjectWrapper(PacketTypes.Play.Client.HELD_ITEM_CHANGE, WrappedPlayInHeldItemSlot::new);
        loadObjectWrapper(PacketTypes.Play.Client.PLAYER_POSITION, WrappedPlayInPosition::new);
        loadObjectWrapper(PacketTypes.Play.Client.PLAYER_POS_AND_ROT, WrappedPlayInPositionLook::new);
        loadObjectWrapper(PacketTypes.Play.Client.PLAYER_ROTATION, WrappedPlayInLook::new);
        loadObjectWrapper(PacketTypes.Play.Client.PLAYER_MOVEMENT, MovementPacketWrapper.Status::new);
        loadObjectWrapper(PacketTypes.Play.Client.GROUND, WrappedPlayInGround::new);
        loadObjectWrapper(PacketTypes.Play.Client.UPDATE_SIGN, WrappedPlayInUpdateSign::new);
        loadObjectWrapper(PacketTypes.Play.Client.TAB_COMPLETE, WrappedPlayInTabComplete::new);
        loadObjectWrapper(PacketTypes.Login.Client.LOGIN_START, WrappedLoginInLoginStart::new);

        loadObjectWrapper(PacketTypes.Play.Server.PLAYER_CHAT, WrappedPlayOutChat::new);
        loadObjectWrapper(PacketTypes.Play.Server.DISCONNECT, WrappedPlayOutDisconnect::new);
        loadObjectWrapper(PacketTypes.Play.Server.OPEN_WINDOW, WrappedPlayOutOpenWindow::new);
        loadObjectWrapper(PacketTypes.Play.Server.PLAYER_POSITION, WrappedPlayOutPosition::new);
        loadObjectWrapper(PacketTypes.Play.Server.TAB_COMPLETE, WrappedPlayOutTabComplete::new);
    }

    private static <WP extends WrappedPacket<WP>> void loadObjectWrapper(PacketType type, BiFunction<Object, PacketContainer<WP>, ObjectWrappedPacket<WP>> invoker) {
        OBJECT_WRAPPERS.set(type.getHoneyID(), WrappedPacketFactory.ObjectWrappedPacketFactory.factoryFromSupplier(invoker));
    }

    private static <WP extends WrappedPacket<WP>> void loadByteWrapper(PacketType type, PacketContainer.PipelineState state, BiFunction<PacketDataSerializer, PacketContainer<WP>, ByteWrappedPacket<WP>> invoker) {
        if (state == PacketContainer.PipelineState.OBJECT) {
            throw new IllegalArgumentException("Object state");
        }

        BYTE_WRAPPERS.computeIfAbsent(state, __ -> new Array<>())
                .set(type.getHoneyID(), WrappedPacketFactory.ByteWrappedPacketFactory.factoryFromSupplier(invoker));
    }

    @SuppressWarnings("unchecked")
    public static <WP extends WrappedPacket<WP>> ObjectWrappedPacket<WP> initWrappedPacket(PacketType type, Object raw, PacketContainer<WP> container) {
        WrappedPacketFactory.ObjectWrappedPacketFactory<WP> factory;
        if (!OBJECT_WRAPPERS.has(type)) {
            factory = WrappedPacketFactory.<WP>acquireHolder().OBJECT_FACTORY;
        } else {
            factory = (WrappedPacketFactory.ObjectWrappedPacketFactory<WP>) OBJECT_WRAPPERS.get(type.getHoneyID());
        }
        return factory.createPacket(raw, container);
    }

    @SuppressWarnings("unchecked")
    public static <WP extends WrappedPacket<WP>> ByteWrappedPacket<WP> initWrappedPacket(PacketType type, PacketContainer.PipelineState state, PacketDataSerializer serializer, PacketContainer<WP> container) {
        if (state == PacketContainer.PipelineState.OBJECT) {
            throw new IllegalArgumentException("Object state");
        }

        WrappedPacketFactory.ByteWrappedPacketFactory<WP> factory;
        Array<WrappedPacketFactory.ByteWrappedPacketFactory<? extends WrappedPacket<?>>> factories = BYTE_WRAPPERS.computeIfAbsent(state, __ -> new Array<>());
        if (!factories.has(type)) {
            factory = WrappedPacketFactory.<WP>acquireHolder().BYTE_FACTORY;
        } else {
            factory = (WrappedPacketFactory.ByteWrappedPacketFactory<WP>) factories.get(type.getHoneyID());
        }
        return factory.createPacket(serializer, container);
    }
}
