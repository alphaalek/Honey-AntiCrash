package me.alek.honey.netty.codec.types;

import me.alek.honey.netty.codec.PacketDataSerializer;

public class VarInt implements Type<Integer, PacketDataSerializer> {

    @Override
    public Integer read(PacketDataSerializer buf, Object... params) {
        byte b0;
        int i = 0;
        int j = 0;
        do {
            b0 = buf.readByte();
            i |= (b0 & Byte.MAX_VALUE) << j++ * 7;
            if (j > 5)
                throw new RuntimeException("VarInt too big");
        } while ((b0 & 0x80) == 128);
        return i;
    }

    @Override
    public PacketDataSerializer write(PacketDataSerializer buf, Integer val, Object... params) {
        while ((val & (-128)) != 0) {
            buf.writeByte((val & 127) | 128);
            val >>>= 7;
        }
        buf.writeByte(val);
        return buf;
    }
}
