package me.alek.honey.netty.codec;

import me.alek.honey.netty.codec.disablednbtimpl.PacketPlayInBlockPlace_WithoutNBT;
import me.alek.honey.netty.codec.disablednbtimpl.PacketPlayInWindowClick_WithoutNBT;

public class DisabledNBTCodec {

    public static LegacyCodec BLOCK_PLACE = new PacketPlayInBlockPlace_WithoutNBT();
    public static LegacyCodec WINDOW_CLICK = new PacketPlayInWindowClick_WithoutNBT();
}
