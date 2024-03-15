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
        HoneyPlugin.get().getNotificationService().showCancellingPackets(player);

        ByteBuf readByteBuf;
        if (byteBuf.refCnt() > 0)
            (readByteBuf = byteBuf.readBytes(byteBuf.readableBytes()))
                    .clear()
                    .release(Math.max(readByteBuf.refCnt() - 1, 1));

        byteBuf.clear();
        byteBuf.release(Math.max(byteBuf.refCnt() - 1, 1));
    }
}
