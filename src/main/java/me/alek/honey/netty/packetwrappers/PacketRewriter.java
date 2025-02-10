package me.alek.honey.netty.packetwrappers;

import me.alek.honey.netty.codec.PacketDataSerializer;

public interface PacketRewriter {

    void rewrite(PacketDataSerializer serializer);

    default boolean shouldRewrite() {
         return true;
    }
}
