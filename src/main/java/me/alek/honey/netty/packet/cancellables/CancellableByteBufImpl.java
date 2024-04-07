package me.alek.honey.netty.packet.cancellables;

import io.netty.buffer.ByteBuf;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.packet.CancellablePacketObserver;

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
        if (player != null && player.isOnlineDirect())
            HoneyPlugin.get().getNotificationService().showPacketFilterActionBar(player, true);

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
