package me.alek.honey.netty.tag.nbt.visitor;

import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import io.netty.handler.codec.DecoderException;
import me.alek.honey.netty.codec.CodecVisitor;
import me.alek.honey.netty.tag.FallbackDefaultCodecException;
import me.alek.honey.netty.tag.nbt.*;
import me.alek.honey.util.protocol.NetProtocol;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ComplexTagVisitor extends NBTVisitor.In.Adapter {

    private final CodecVisitor delegate;
    private ComplexTagVisitor child;
    private ComplexTagVisitor parent;
    private String pointer = NBTFactory.NAMED_COMPOUND_TAG_NAME;
    private final List<String> keys = new ArrayList<>();

    public ComplexTagVisitor(CodecVisitor delegate) {
        this.delegate = delegate;
        if (delegate instanceof ComplexTagVisitor) {
            this.parent = (ComplexTagVisitor) delegate;
        }
    }

    @Override
    public AbstractTag<?, ?, ?> visitAtCompoundInsertTag(DataInputStream stream, TagLimiter limiter, TagType type, String key) throws FallbackDefaultCodecException, IOException {
        if (this.child != null) {
            return this.child.visitAtCompoundInsertTag(stream, limiter, type, key);
        }

        if (this.keys.contains(key)) {
            throw new DecoderException("Invalid item tag: duplicate key: " + key);
        }
        this.keys.add(key);

        // tag collections only exists for complex tag types
        if (type.hasCollection()) {
            this.child = new ComplexTagVisitor(this);
            this.child.pointer = key;
        }

        return super.visitAtCompoundInsertTag(stream, limiter, type, key);
    }

    @Override
    public void visitCompound(Map<String, AbstractTag<?, ?, ?>> compound) {
        if (this.child != null) {
            this.child.visitCompound(compound);
        } else if (this.parent != null) {
            this.parent.child = null; // pointed to this instance
        }

        super.visitCompound(compound);
    }

    @Override
    public <TAG extends AbstractTag<?, ?, TAG>> void visitArray(List<TAG> array, TagType type) {
        if (this.child != null) {
            this.child.visitArray(array, type);
        } else if (this.parent != null) {
            this.parent.child = null; // pointed to this instance
        }

        super.visitArray(array, type);
    }

    @Override
    public ItemType type() {
        return this.delegate.type();
    }

    @Override
    public NetProtocol protocol() {
        return this.delegate.protocol();
    }

    @Override
    public NetProtocol protocolCreated() {
        return this.delegate.protocolCreated();
    }

    public ComplexTagVisitor child() {
        return this.child;
    }

    public ComplexTagVisitor parent() {
        return this.parent;
    }

    public List<String> keys() {
        return this.keys;
    }

    public String pointer() {
        return this.pointer;
    }

    public String currentGrandChild() {
        ComplexTagVisitor child = this;
        String key = this.pointer;
        while (child != null) {
            child = child.child;
            if (child != null) {
                key = child.pointer;
            }
        }
        return key;
    }
}
