package me.alek.honey.netty.packet;

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;

public class CancellableByteBuf implements Cancellable {

    private final ByteBuf buf;
    private final HoneyPlayer player;
    private boolean cancelled;

    public CancellableByteBuf(HoneyPlayer player, ByteBuf byteBuf) {
        this.player = player;
        this.buf = byteBuf;
    }

    @Override
    public void cancel() {
        if (cancelled) {
            return;
        }
        cancelled = true;

        buf.clear();
        if (buf.refCnt() > 1) {
            buf.release(buf.refCnt() - 1);
        }

        HoneyPlugin.get().getNotificationService().showPacketFilterActionBar(player, true, true);
    }

    public ByteBuf buf() {
        return buf;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }
}
