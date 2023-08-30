package me.alek.honey.netty.packet.cancellables;

import io.netty.buffer.ByteBuf;
import me.alek.honey.netty.packet.CancellablePacketObserver;

public class CancellableByteBufImpl implements CancellablePacketObserver {

    private final ByteBuf byteBuf;

    public CancellableByteBufImpl(ByteBuf byteBuf) {
        this.byteBuf = byteBuf;
    }

    @Override
    public void cancel() {
        if (byteBuf.refCnt() > 0) byteBuf.clear();
    }
}
