package me.alek.honey.netty.packet.cancellables;

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import me.alek.honey.netty.packet.CancellablePacketObserver;
import org.bukkit.Bukkit;

public class CancellableByteBufImpl implements CancellablePacketObserver {

    private final ByteBuf byteBuf;

    public CancellableByteBufImpl(ByteBuf byteBuf) {
        this.byteBuf = byteBuf;
    }

    @Override
    public void cancel() {
        Bukkit.broadcastMessage("cancelling bytebuf");
        if (byteBuf.refCnt() > 0) byteBuf.clear();
    }
}
