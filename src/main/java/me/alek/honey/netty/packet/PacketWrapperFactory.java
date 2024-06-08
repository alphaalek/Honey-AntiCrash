package me.alek.honey.netty.packet;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
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
        loadWrapper(PacketType.Play.Client.BLOCK_DIG, WrappedPlayInBlockDig::new);
        loadWrapper(PacketType.Play.Client.BLOCK_PLACE, WrappedPlayInBlockPlace::new);
        loadWrapper(PacketType.Play.Client.CHAT, WrappedPlayInChat::new);
        loadWrapper(PacketType.Play.Client.SET_CREATIVE_SLOT, WrappedPlayInSetCreativeSlot::new);
        loadWrapper(PacketType.Play.Client.HELD_ITEM_SLOT, WrappedPlayInHeldItemSlot::new);
        loadWrapper(PacketType.Play.Client.POSITION, WrappedPlayInPosition::new);
        loadWrapper(PacketType.Play.Client.POSITION_LOOK, WrappedPlayInPosition::new);
        loadWrapper(PacketType.Play.Client.LOOK, WrappedPlayInPosition::new);
        loadWrapper(PacketType.Play.Client.FLYING, WrappedPlayInFlying.Status::new);
        loadWrapper(PacketType.Play.Client.GROUND, WrappedPlayInGround::new);
        loadWrapper(PacketType.Play.Client.CUSTOM_PAYLOAD, WrappedPlayInCustomPayload::new);
        loadWrapper(PacketType.Play.Client.UPDATE_SIGN, WrappedPlayInUpdateSign::new);
        loadWrapper(PacketType.Play.Client.TAB_COMPLETE, WrappedPlayInTabComplete::new);
        loadWrapper(PacketType.Play.Client.WINDOW_CLICK, WrappedPlayInWindowClick::new);
        loadWrapper(PacketType.Login.Client.ENCRYPTION_BEGIN, WrappedLoginInEncryptionBegin::new);
        loadWrapper(PacketType.Login.Client.LOGIN_START, WrappedLoginInLoginStart::new);

        loadWrapper(PacketType.Play.Server.PLAYER_CHAT, WrappedPlayOutChat::new);
        loadWrapper(PacketType.Play.Server.KICK_DISCONNECT, WrappedPlayOutKickDisconnect::new);
        loadWrapper(PacketType.Play.Server.POSITION, WrappedPlayOutPosition::new);
        loadWrapper(PacketType.Play.Server.TAB_COMPLETE, WrappedPlayOutTabComplete::new);
    }

    private static <WP extends WrappedPacket<WP>> void loadWrapper(PacketTypeEnum type, PacketInvoker<WP> invoker) {
        WRAPPERS.set(type.getHoneyID(), invoker);
    }

    @SuppressWarnings("unchecked")
    private static <WP extends WrappedPacket<WP>> PacketInvoker<WP> getInvoker(PacketTypeEnum type) {
        return (PacketInvoker<WP>) WRAPPERS.get(type.getHoneyID());
    }

    public static <WP extends WrappedPacket<WP>> WrappedPacket<WP> initWrappedPacket(PacketTypeEnum type, Object raw, PacketContainer<WP> container) {
        if (!WRAPPERS.has(type.getHoneyID())) {
            return new WrappedPacket<>(raw, container);
        }

        PacketInvoker<WP> invoker = getInvoker(type);
        return invoker.create(raw, container);
    }
}
