package me.alek.honey.netty.packetwrappers;

import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.packet.PacketContainer;

public interface PacketRewriter {

    void rewrite(PacketDataSerializer serializer);

    default boolean shouldRewrite(PacketContainer<?> packet) {
        // default only allow rewriting once
        return packet.getLastRewrittenPipelineState() == null;
    }
}
