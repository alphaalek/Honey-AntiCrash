package me.alek.honey.netty.packet;

import io.netty.buffer.ByteBuf;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;

public class CancellableByteBuf implements Cancellable {

    private final ByteBuf byteBuf;
    private final HoneyPlayer player;
    private boolean cancelled;

    public CancellableByteBuf(HoneyPlayer player, ByteBuf byteBuf) {
        this.player = player;
        this.byteBuf = byteBuf;
    }

    @Override
    public void cancel() {
        if (cancelled) {
            return;
        }
        cancelled = true;

        if (player != null && player.isOnlineDirect()) {
            HoneyPlugin.get().getNotificationService().showPacketFilterActionBar(player, true, true);
        }

        if (byteBuf.refCnt() > 0) {
            byteBuf.release();
        }
    }

    public ByteBuf buf() {
        return byteBuf;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }
}
