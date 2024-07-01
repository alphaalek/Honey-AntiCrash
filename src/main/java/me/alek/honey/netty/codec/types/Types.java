package me.alek.honey.netty.codec.types;

import me.alek.honey.netty.codec.PacketDataSerializer;

public class Types {

    public static Type<Integer, PacketDataSerializer> VAR_INT = new VarInt();
    public static Type<String, PacketDataSerializer> UTF8 = new Utf8String();
}
