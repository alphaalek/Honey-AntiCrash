package me.alek.honey.netty.handler.codec;

import me.alek.honey.netty.handler.codec.impl.PacketPlayInBlockPlace_WithoutNBT;
import me.alek.honey.netty.handler.codec.impl.PacketPlayInWindowClick_WithoutNBT;

public class DisabledNBTCodec {

    public static Codec BLOCK_PLACE = new PacketPlayInBlockPlace_WithoutNBT();
    public static Codec WINDOW_CLICK = new PacketPlayInWindowClick_WithoutNBT();
}
