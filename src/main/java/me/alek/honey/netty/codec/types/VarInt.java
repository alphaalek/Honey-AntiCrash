package me.alek.honey.netty.codec.types;

import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.codec.TypeSerializer;

public class VarInt implements TypeSerializer.NoContext<Integer> {

    public static final int MAX_BYTES = 5;

    public static final int SEGMENT_BITS = 0x7F;
    public static final int CONTINUE_BIT = 0x80;
    public static final int SHIFT = 7;

    @Override
    public Integer read(PacketDataSerializer buf) {
        byte current;
        int value = 0, pos = 0;
        do {
            current = buf.readByte();
            value |= (current & SEGMENT_BITS) << pos++ * SHIFT;
            if (pos > MAX_BYTES) {
                throw new RuntimeException("VarInt too big");
            }
        } while ((current & CONTINUE_BIT) != 0);

        return value;
    }

    @Override
    public PacketDataSerializer write(PacketDataSerializer buf, Integer val) {
        while ((val & -CONTINUE_BIT) != 0) {
            buf.writeByte((val & SEGMENT_BITS) | CONTINUE_BIT);
            val >>>= SHIFT;
        }
        buf.writeByte(val);
        return buf;
    }
}
