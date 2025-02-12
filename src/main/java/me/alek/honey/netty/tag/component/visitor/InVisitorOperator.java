package me.alek.honey.netty.tag.component.visitor;

import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.tag.FallbackDefaultCodecException;

public interface InVisitorOperator<T> {

    T visitAt(ComponentMapVisitor.In in, PacketDataSerializer serializer) throws FallbackDefaultCodecException;
}
