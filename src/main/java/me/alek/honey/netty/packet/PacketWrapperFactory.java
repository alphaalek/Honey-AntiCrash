package me.alek.honey.netty.packet;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.type.PacketTypes;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.packetwrappers.login.client.WrappedLoginInEncryptionBegin;
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
        loadWrapper(PacketTypes.Play.Client.BLOCK_DIG, WrappedPlayInBlockDig::new);
        loadWrapper(PacketTypes.Play.Client.BLOCK_PLACE, WrappedPlayInBlockPlace::new);
        loadWrapper(PacketTypes.Play.Client.CHAT, WrappedPlayInChat::new);
        loadWrapper(PacketTypes.Play.Client.SET_CREATIVE_SLOT, WrappedPlayInSetCreativeSlot::new);
        loadWrapper(PacketTypes.Play.Client.HELD_ITEM_SLOT, WrappedPlayInHeldItemSlot::new);
        loadWrapper(PacketTypes.Play.Client.POSITION, WrappedPlayInPosition::new);
        loadWrapper(PacketTypes.Play.Client.POSITION_LOOK, WrappedPlayInPositionLook::new);
        loadWrapper(PacketTypes.Play.Client.LOOK, WrappedPlayInLook::new);
        loadWrapper(PacketTypes.Play.Client.FLYING, WrappedPlayInFlying.Status::new);
        loadWrapper(PacketTypes.Play.Client.GROUND, WrappedPlayInGround::new);
        loadWrapper(PacketTypes.Play.Client.CUSTOM_PAYLOAD, WrappedPlayInCustomPayload::new);
        loadWrapper(PacketTypes.Play.Client.UPDATE_SIGN, WrappedPlayInUpdateSign::new);
        loadWrapper(PacketTypes.Play.Client.TAB_COMPLETE, WrappedPlayInTabComplete::new);
        loadWrapper(PacketTypes.Play.Client.WINDOW_CLICK, WrappedPlayInWindowClick::new);
        loadWrapper(PacketTypes.Login.Client.ENCRYPTION_BEGIN, WrappedLoginInEncryptionBegin::new);
        loadWrapper(PacketTypes.Login.Client.LOGIN_START, WrappedLoginInLoginStart::new);

        loadWrapper(PacketTypes.Play.Server.PLAYER_CHAT, WrappedPlayOutChat::new);
        loadWrapper(PacketTypes.Play.Server.KICK_DISCONNECT, WrappedPlayOutKickDisconnect::new);
        loadWrapper(PacketTypes.Play.Server.POSITION, WrappedPlayOutPosition::new);
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
