package me.alek.honey.netty.packetwrappers;

import io.netty.buffer.ByteBuf;

public interface AutoReleasable {

    void release();

    static void doRelease(ByteBuf buf) {
        if (buf.refCnt() > 0) {
            buf.release();
        }
    }
}
