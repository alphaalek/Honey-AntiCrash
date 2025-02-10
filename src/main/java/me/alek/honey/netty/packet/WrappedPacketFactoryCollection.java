package me.alek.honey.netty.packet;

import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packetwrappers.*;
import me.alek.honey.netty.packetwrappers.impl.handshake.WrappedHandshakeInSetProtocol;
import me.alek.honey.netty.packetwrappers.impl.login.WrappedLoginInLoginStart;
import me.alek.honey.netty.packetwrappers.impl.play.client.*;
import me.alek.honey.netty.packetwrappers.impl.play.server.*;
import me.alek.honey.util.Array;
import me.alek.honey.util.IDRegistry;

import java.util.EnumMap;
import java.util.function.BiFunction;

public class WrappedPacketFactoryCollection {

    private final static Array<WrappedPacketFactory.ObjectWrappedPacketFactory<?>> OBJECT_WRAPPERS = new Array<>();
    private final static EnumMap<PacketContainer.PipelineState, Array<WrappedPacketFactory.ByteWrappedPacketFactory<?>>> BYTE_WRAPPERS = new EnumMap<>(PacketContainer.PipelineState.class);
    private final static Array<IDRegistry> TYPES_PIPELINE_STATES = new Array<>();

    public static void loadWrappers() {
        loadByteWrapper(PacketTypes.Play.Client.BLOCK_PLACE, PacketContainer.PipelineState.INITIAL_BYTE, WrappedPlayInBlockPlace::new);
        loadByteWrapper(PacketTypes.Play.Client.CREATIVE_INVENTORY_ACTION, PacketContainer.PipelineState.INITIAL_BYTE, WrappedPlayInCreativeInventoryAction::new);
        loadByteWrapper(PacketTypes.Play.Client.CLICK_WINDOW, PacketContainer.PipelineState.INITIAL_BYTE, WrappedPlayInWindowClick::new);
        loadByteWrapper(PacketTypes.Play.Client.CUSTOM_PAYLOAD, PacketContainer.PipelineState.INITIAL_BYTE, WrappedPlayInCustomPayload::new);
        loadByteWrapper(PacketTypes.Play.Client.BOOK_EDIT, PacketContainer.PipelineState.INITIAL_BYTE, WrappedPlayInBookEdit::new);

        loadByteWrapper(PacketTypes.Handshake.Client.SET_PROTOCOL, PacketContainer.PipelineState.INITIAL_BYTE, WrappedHandshakeInSetProtocol::new);
        loadByteWrapper(PacketTypes.Login.Client.HELLO, PacketContainer.PipelineState.INITIAL_BYTE, WrappedLoginInLoginStart::new);

        loadByteWrapper(PacketTypes.Play.Client.PLAYER_POSITION, PacketContainer.PipelineState.FINAL_BYTE, WrappedPlayInPosition::new);
        loadByteWrapper(PacketTypes.Play.Client.PLAYER_POS_ROT, PacketContainer.PipelineState.FINAL_BYTE, WrappedPlayInPositionAndRotation::new);
        loadByteWrapper(PacketTypes.Play.Client.PLAYER_ROTATION, PacketContainer.PipelineState.FINAL_BYTE, WrappedPlayInRotation::new);
        loadByteWrapper(PacketTypes.Play.Client.PLAYER_MOVEMENT, PacketContainer.PipelineState.FINAL_BYTE, WrappedPlayInFlying.Flying::new);
        loadByteWrapper(PacketTypes.Play.Client.UPDATE_SIGN, PacketContainer.PipelineState.FINAL_BYTE, WrappedPlayInUpdateSign::new);

        loadByteWrapper(PacketTypes.Play.Client.PICK_ITEM, PacketContainer.PipelineState.FINAL_BYTE, WrappedPlayInPickItem::new);

        loadObjectWrapper(PacketTypes.Play.Client.PLAYER_DIGGING, WrappedPlayInBlockDig::new);
        loadObjectWrapper(PacketTypes.Play.Client.CHAT_MESSAGE, WrappedPlayInChat::new);
        loadObjectWrapper(PacketTypes.Play.Client.CHAT_COMMAND, WrappedPlayInChat::new);
        loadObjectWrapper(PacketTypes.Play.Client.HELD_ITEM_CHANGE, WrappedPlayInHeldItemSlot::new);
        loadObjectWrapper(PacketTypes.Play.Client.TAB_COMPLETE, WrappedPlayInTabComplete::new);

        loadObjectWrapper(PacketTypes.Play.Server.CHAT, WrappedPlayOutChat::new);
        loadObjectWrapper(PacketTypes.Play.Server.DISCONNECT, WrappedPlayOutDisconnect::new);
        loadObjectWrapper(PacketTypes.Play.Server.OPEN_WINDOW, WrappedPlayOutOpenWindow::new);
        loadObjectWrapper(PacketTypes.Play.Server.PLAYER_POSITION, WrappedPlayOutPosition::new);
        loadObjectWrapper(PacketTypes.Play.Server.TAB_COMPLETE, WrappedPlayOutTabComplete::new);
    }

    private static <WP extends SourcedWrappedPacket<WP>> void loadObjectWrapper(PacketType type, BiFunction<Object, PacketContainer<WP>, ObjectWrappedPacket<WP>> invoker) {
        OBJECT_WRAPPERS.set(type.getHoneyID(), WrappedPacketFactory.ObjectWrappedPacketFactory.factoryFromSupplier(invoker));
        loadWrapper(type, PacketContainer.PipelineState.OBJECT);
    }

    private static <WP extends SourcedWrappedPacket<WP>> void loadByteWrapper(PacketType type, PacketContainer.PipelineState state, BiFunction<PacketDataSerializer, PacketContainer<WP>, ByteInWrappedPacket<WP>> invoker) {
        if (state == PacketContainer.PipelineState.OBJECT) {
            throw new IllegalArgumentException("Object state");
        }

        BYTE_WRAPPERS.computeIfAbsent(state, __ -> new Array<>())
                .set(type.getHoneyID(), WrappedPacketFactory.ByteWrappedPacketFactory.factoryFromSupplier(invoker));
        loadWrapper(type, state);
    }

    private static void loadWrapper(PacketType type, PacketContainer.PipelineState state) {
        TYPES_PIPELINE_STATES.get(type, IDRegistry::new).add(state.ordinal());
    }

    public static IDRegistry getPipelineStates(PacketType type) {
        return TYPES_PIPELINE_STATES.get(type);
    }

    public static boolean hasObjectWrappedPacket(PacketType type) {
        return OBJECT_WRAPPERS.has(type);
    }

    public static boolean hasByteWrappedPacket(PacketType type, PacketContainer.PipelineState state) {
        return BYTE_WRAPPERS.containsKey(state) && BYTE_WRAPPERS.get(state).has(type);
    }

    @SuppressWarnings("unchecked")
    public static <WP extends SourcedWrappedPacket<WP>> ObjectWrappedPacket<WP> initWrappedPacket(PacketType type, Object raw, PacketContainer<WP> container) {
        WrappedPacketFactory.ObjectWrappedPacketFactory<WP> factory;
        if (OBJECT_WRAPPERS.has(type)) {
            factory = (WrappedPacketFactory.ObjectWrappedPacketFactory<WP>) OBJECT_WRAPPERS.get(type.getHoneyID());
        } else {
            factory = WrappedPacketFactory.<WP>acquireHolder().OBJECT_FACTORY;
        }
        return factory.createPacket(raw, container);
    }

    @SuppressWarnings("unchecked")
    public static <WP extends SourcedWrappedPacket<WP>> ByteInWrappedPacket<WP> initWrappedPacket(PacketType type, PacketContainer.PipelineState state, PacketDataSerializer serializer, PacketContainer<WP> container) {
        if (state == PacketContainer.PipelineState.OBJECT) {
            throw new IllegalArgumentException("Object state");
        }

        WrappedPacketFactory.ByteWrappedPacketFactory<WP> factory;
        Array<WrappedPacketFactory.ByteWrappedPacketFactory<?>> factories = BYTE_WRAPPERS.computeIfAbsent(state, __ -> new Array<>());
        if (factories.has(type)) {
            factory = (WrappedPacketFactory.ByteWrappedPacketFactory<WP>) factories.get(type.getHoneyID());
        } else {
            factory = WrappedPacketFactory.<WP>acquireHolder().BYTE_FACTORY;
        }
        return factory.createPacket(serializer, container);
    }
}
