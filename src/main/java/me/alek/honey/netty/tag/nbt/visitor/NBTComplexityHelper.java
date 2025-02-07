package me.alek.honey.netty.tag.nbt.visitor;

import me.alek.honey.netty.codec.CodecVisitor;
import me.alek.honey.netty.tag.nbt.*;
import me.alek.honey.util.protocol.NetProtocol;

import java.io.DataInputStream;
import java.util.Map;

public class NBTComplexityHelper extends TagVisitor.In.Adapter {

    private final CodecVisitor delegate;
    private NBTComplexityHelper child;
    private NBTComplexityHelper parent;
    private String key = NBTFactory.NAMED_COMPOUND_TAG_NAME;

    public NBTComplexityHelper(CodecVisitor delegate) {
        this.delegate = delegate;
        if (delegate instanceof NBTComplexityHelper) {
            this.parent = (NBTComplexityHelper) delegate;
        }
    }

    @Override
    public NetProtocol protocol() {
        return this.delegate.protocol();
    }

    @Override
    public AbstractTag<?, ?, ?> visitAtCompoundInsertTag(DataInputStream stream, TagLimiter limiter, TagType type, String key) throws FallbackDefaultCodecException {
        if (this.child != null) {
            return this.child.visitAtCompoundInsertTag(stream, limiter, type, key);
        }

        if (type.getCollection() == TagTypeCollection.COMPOUND) {
            this.child = new NBTComplexityHelper(this);
            this.child.key = key;
        }

        throw new FallbackDefaultCodecException();
    }

    @Override
    public void visitCompound(Map<String, AbstractTag<?, ?, ?>> tag) {
        if (this.child != null) {
            this.child.visitCompound(tag);
            return;
        }

        if (this.key.equals(NBTFactory.NAMED_COMPOUND_TAG_NAME)) {
            this.parent.child = null; // pointed to this instance
        }
    }
}
