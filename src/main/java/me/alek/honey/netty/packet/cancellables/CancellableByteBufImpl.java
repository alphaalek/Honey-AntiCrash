package me.alek.honey.netty.packet.cancellables;

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.packet.CancellablePacketObserver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class CancellableByteBufImpl implements CancellablePacketObserver {

    private final ByteBuf byteBuf;
    private final HoneyPlayer player;
    private boolean cancelled;

    public CancellableByteBufImpl(HoneyPlayer player, ByteBuf byteBuf) {
        this.player = player;
        this.byteBuf = byteBuf;
    }

    @Override
    public void cancel() {
        if (cancelled) return;
        cancelled = true;
        if (player != null && player.isOnline())
            HoneyPlugin.get().getNotificationService().showCancellingPackets(player);

        byteBuf.readBytes(byteBuf.readableBytes()).release();
    }

    public ByteBuf buf() {
        return byteBuf;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }
}
