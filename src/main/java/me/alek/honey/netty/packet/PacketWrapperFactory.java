package me.alek.honey.netty.packet;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.packetwrappers.handshake.client.WrappedHandshakeInSetProtocol;
import me.alek.honey.packetwrappers.login.client.WrappedLoginInLoginStart;
import me.alek.honey.packetwrappers.play.client.*;
import me.alek.honey.packetwrappers.play.server.WrappedPlayOutChat;
import me.alek.honey.packetwrappers.play.server.WrappedPlayOutKickDisconnect;
import me.alek.honey.packetwrappers.play.server.WrappedPlayOutPosition;
import me.alek.honey.packetwrappers.play.server.WrappedPlayOutTabComplete;
import me.alek.honey.util.Array;

public class PacketWrapperFactory {

    private final static Array<PacketInvoker<? extends WrappedPacket<?>>> WRAPPERS = new Array<>();

    @FunctionalInterface
    public interface PacketInvoker<WP extends WrappedPacket<WP>> {

        WrappedPacket<WP> create(Object raw, PacketContainer<WP> container);
    }

    public static void loadWrappers() {
        loadWrapper(PacketTypes.Play.Client.PLAYER_DIGGING, WrappedPlayInBlockDig::new);
        loadWrapper(PacketTypes.Play.Client.PLAYER_BLOCK_PLACEMENT, WrappedPlayInBlockPlace::new);
        loadWrapper(PacketTypes.Play.Client.CHAT_MESSAGE, WrappedPlayInChat::new);
        loadWrapper(PacketTypes.Play.Client.CHAT_COMMAND, WrappedPlayInChat::new);
        loadWrapper(PacketTypes.Play.Client.CREATIVE_INVENTORY_ACTION, WrappedPlayInSetCreativeSlot::new);
        loadWrapper(PacketTypes.Play.Client.HELD_ITEM_CHANGE, WrappedPlayInHeldItemSlot::new);
        loadWrapper(PacketTypes.Play.Client.PLAYER_POSITION, WrappedPlayInPosition::new);
        loadWrapper(PacketTypes.Play.Client.PLAYER_POS_AND_ROT, WrappedPlayInPositionLook::new);
        loadWrapper(PacketTypes.Play.Client.PLAYER_ROTATION, WrappedPlayInLook::new);
        loadWrapper(PacketTypes.Play.Client.PLAYER_MOVEMENT, WrappedPlayInFlying.Status::new);
        loadWrapper(PacketTypes.Play.Client.GROUND, WrappedPlayInGround::new);
        loadWrapper(PacketTypes.Play.Client.CUSTOM_PAYLOAD, WrappedPlayInCustomPayload::new);
        loadWrapper(PacketTypes.Play.Client.UPDATE_SIGN, WrappedPlayInUpdateSign::new);
        loadWrapper(PacketTypes.Play.Client.TAB_COMPLETE, WrappedPlayInTabComplete::new);
        loadWrapper(PacketTypes.Play.Client.CLICK_WINDOW, WrappedPlayInWindowClick::new);
        loadWrapper(PacketTypes.Login.Client.LOGIN_START, WrappedLoginInLoginStart::new);
        loadWrapper(PacketTypes.Handshake.Client.SET_PROTOCOL, WrappedHandshakeInSetProtocol::new);

        loadWrapper(PacketTypes.Play.Server.PLAYER_CHAT, WrappedPlayOutChat::new);
        loadWrapper(PacketTypes.Play.Server.DISCONNECT, WrappedPlayOutKickDisconnect::new);
        loadWrapper(PacketTypes.Play.Server.PLAYER_POSITION, WrappedPlayOutPosition::new);
        loadWrapper(PacketTypes.Play.Server.TAB_COMPLETE, WrappedPlayOutTabComplete::new);
    }

    private static <WP extends WrappedPacket<WP>> void loadWrapper(PacketType type, PacketInvoker<WP> invoker) {
        WRAPPERS.set(type.getHoneyID(), invoker);
    }

    @SuppressWarnings("unchecked")
    private static <WP extends WrappedPacket<WP>> PacketInvoker<WP> getInvoker(PacketType type) {
        return (PacketInvoker<WP>) WRAPPERS.get(type.getHoneyID());
    }

    public static <WP extends WrappedPacket<WP>> WrappedPacket<WP> initWrappedPacket(PacketType type, Object raw, PacketContainer<WP> container) {
        if (!WRAPPERS.has(type.getHoneyID())) {
            return new WrappedPacket<>(raw, container);
        }

        PacketInvoker<WP> invoker = getInvoker(type);
        return invoker.create(raw, container);
    }
}
