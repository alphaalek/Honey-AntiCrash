package me.alek.honey.netty.codec.types;

import me.alek.honey.netty.codec.PacketDataSerializer;

public class VarInt implements Type<Integer, PacketDataSerializer> {

    private static final int SEGMENT_BITS = 0x7F;
    private static final int CONTINUE_BIT = 0x80;
    private static final int SHIFT = 7;

    @Override
    public Integer read(PacketDataSerializer buf, Object... params) {
        byte current;
        int value = 0, pos = 0;
        do {
            current = buf.readByte();
            value |= (current & SEGMENT_BITS) << pos++ * SHIFT;
            if (pos > 5) {
                throw new RuntimeException("VarInt too big");
            }
        } while ((current & CONTINUE_BIT) != 0);

        return value;
    }

    @Override
    public PacketDataSerializer write(PacketDataSerializer buf, Integer val, Object... params) {
        while ((val & -CONTINUE_BIT) != 0) {
            buf.writeByte((val & SEGMENT_BITS) | CONTINUE_BIT);
            val >>>= SHIFT;
        }
        buf.writeByte(val);
        return buf;
    }
}
