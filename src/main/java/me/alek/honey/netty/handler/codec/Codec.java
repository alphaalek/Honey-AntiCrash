package me.alek.honey.netty.handler.codec;

import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.modelwrappers.network.WrappedPacketDataSerializer;

public interface Codec {

    void apply(IStructureModifier<Object> structure, WrappedPacketDataSerializer serializer);
}
