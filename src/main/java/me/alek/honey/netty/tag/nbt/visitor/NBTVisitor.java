package me.alek.honey.netty.tag.nbt.visitor;

import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import me.alek.honey.compat.via.ViaProtocol;
import me.alek.honey.netty.codec.CodecVisitor;
import me.alek.honey.netty.tag.FallbackDefaultCodecException;
import me.alek.honey.netty.tag.nbt.AbstractTag;
import me.alek.honey.netty.tag.nbt.NBTFactory;
import me.alek.honey.netty.tag.nbt.TagLimiter;
import me.alek.honey.netty.tag.nbt.TagType;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.ItemFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface NBTVisitor extends CodecVisitor {

    abstract class Adapter implements NBTVisitor {

        @Override
        public <T> void visitType(TagType type, T val) {

        }

        @Override
        public void visitEnd() {

        }

        @Override
        public void visitByte(byte val) {

        }

        @Override
        public void visitShort(short val) {

        }

        @Override
        public void visitInt(int val) {

        }

        @Override
        public void visitLong(long val) {

        }

        @Override
        public void visitFloat(float val) {

        }

        @Override
        public void visitDouble(double val) {

        }

        @Override
        public void visitByteArray(byte[] val) {

        }

        @Override
        public void visitIntArray(int[] val) {

        }

        @Override
        public void visitLongArray(long[] val) {

        }

        @Override
        public void visitString(String val) {

        }

        @Override
        public <TAG extends AbstractTag<?, TAG>> void visitArray(List<TAG> array, TagType type) {

        }

        @Override
        public void visitArrayElement(AbstractTag<?, ?> val) {

        }

        @Override
        public void visitCompound(Map<String, AbstractTag<?, ?>> compound) {

        }

        @Override
        public void visitCompoundElement(String key, AbstractTag<?, ?> tag) {

        }

        @Override
        public String visitCompoundKey(String key, TagType type) {
            return key;
        }
    }

    interface In extends NBTVisitor {

        static NBTVisitor.In createDefault(TagLimiter limiter, NetProtocol protocol, NetProtocol protocolCreated, ItemType type) {
            return new Adapter(limiter) {
                @Override
                public ItemType type() {
                    return type;
                }

                @Override
                public NetProtocol protocol() {
                    return protocol;
                }

                @Override
                public NetProtocol protocolCreated() {
                    return protocolCreated;
                }
            };
        }

        static NBTVisitor.In createDefault(NetProtocol protocol, NetProtocol protocolCreated, ItemType type) {
            TagLimiter limiter;
            if (type.getName().getKey().contains("shulker_box")
                    // assume emulated shulker box
                    || ViaProtocol.ValidatorInstances.VIA_BACKWARDS_VALIDATOR.find().get() && protocol.isOlderThan(NetProtocol.v1_11) && type.equals(ItemTypes.DROPPER)) {
                // TODO
                limiter = new TagLimiter.NoOp();
            } else {
                limiter = NBTFactory.createDefaultTagLimiter();
            }

            return createDefault(limiter, protocol, protocolCreated, type);
        }

        abstract class Adapter extends NBTVisitor.Adapter implements In {

            private final TagLimiter limiter;

            public Adapter(TagLimiter limiter) {
                this.limiter = limiter;
            }

            @Override
            public TagLimiter limiter() {
                return this.limiter;
            }

            @Override
            public void visitAtEnd(DataInputStream stream, TagLimiter limiter) {

            }

            @Override
            public byte visitAtByte(DataInputStream stream, TagLimiter limiter) throws FallbackDefaultCodecException, IOException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public short visitAtShort(DataInputStream stream, TagLimiter limiter) throws FallbackDefaultCodecException, IOException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public int visitAtInt(DataInputStream stream, TagLimiter limiter) throws FallbackDefaultCodecException, IOException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public long visitAtLong(DataInputStream stream, TagLimiter limiter) throws FallbackDefaultCodecException, IOException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public float visitAtFloat(DataInputStream stream, TagLimiter limiter) throws FallbackDefaultCodecException, IOException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public double visitAtDouble(DataInputStream stream, TagLimiter limiter) throws FallbackDefaultCodecException, IOException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public byte[] visitAtByteArray(DataInputStream stream, TagLimiter limiter) throws FallbackDefaultCodecException, IOException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public int[] visitAtIntArray(DataInputStream stream, TagLimiter limiter) throws FallbackDefaultCodecException, IOException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public long[] visitAtLongArray(DataInputStream stream, TagLimiter limiter) throws FallbackDefaultCodecException, IOException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public String visitAtString(DataInputStream stream, TagLimiter limiter) throws FallbackDefaultCodecException, IOException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public <TAG extends AbstractTag<?, TAG>> List<TAG> visitAtArray(DataInputStream stream, TagLimiter limiter, TagType type) throws FallbackDefaultCodecException, IOException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public Map<String, AbstractTag<?, ?>> visitAtCompound(DataInputStream stream, TagLimiter limiter) throws FallbackDefaultCodecException, IOException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public Map.Entry<String, AbstractTag<?, ?>> visitAtCompoundInsert(DataInputStream stream, TagType type) throws FallbackDefaultCodecException, IOException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public AbstractTag<?, ?> visitAtCompoundInsertTag(DataInputStream stream, TagLimiter limiter, TagType type, String key) throws FallbackDefaultCodecException, IOException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public String visitAtCompoundInsertKey(DataInputStream stream, TagType type) throws FallbackDefaultCodecException, IOException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public <TAG extends AbstractTag<?, TAG>> TAG visitAtArrayInsert(DataInputStream stream, TagLimiter limiter, TagType type) throws FallbackDefaultCodecException, IOException {
                throw new FallbackDefaultCodecException();
            }
        }

        TagLimiter limiter();

        void visitAtEnd(DataInputStream stream, TagLimiter limiter);

        byte visitAtByte(DataInputStream stream, TagLimiter limiter) throws FallbackDefaultCodecException, IOException;

        short visitAtShort(DataInputStream stream, TagLimiter limiter) throws FallbackDefaultCodecException, IOException;

        int visitAtInt(DataInputStream stream, TagLimiter limiter) throws FallbackDefaultCodecException, IOException;

        long visitAtLong(DataInputStream stream, TagLimiter limiter) throws FallbackDefaultCodecException, IOException;

        float visitAtFloat(DataInputStream stream, TagLimiter limiter) throws FallbackDefaultCodecException, IOException;

        double visitAtDouble(DataInputStream stream, TagLimiter limiter) throws FallbackDefaultCodecException, IOException;

        byte[] visitAtByteArray(DataInputStream stream, TagLimiter limiter) throws FallbackDefaultCodecException, IOException;

        int[] visitAtIntArray(DataInputStream stream, TagLimiter limiter) throws FallbackDefaultCodecException, IOException;

        long[] visitAtLongArray(DataInputStream stream, TagLimiter limiter) throws FallbackDefaultCodecException, IOException;

        String visitAtString(DataInputStream stream, TagLimiter limiter) throws FallbackDefaultCodecException, IOException;

        <TAG extends AbstractTag<?, TAG>> List<TAG> visitAtArray(DataInputStream stream, TagLimiter limiter, TagType type) throws FallbackDefaultCodecException, IOException;

        Map<String, AbstractTag<?, ?>> visitAtCompound(DataInputStream stream, TagLimiter limiter) throws FallbackDefaultCodecException, IOException;

        Map.Entry<String, AbstractTag<?, ?>> visitAtCompoundInsert(DataInputStream stream, TagType type) throws FallbackDefaultCodecException, IOException, IOException;

        AbstractTag<?, ?> visitAtCompoundInsertTag(DataInputStream stream, TagLimiter limiter, TagType type, String key) throws FallbackDefaultCodecException, IOException;

        String visitAtCompoundInsertKey(DataInputStream stream, TagType type) throws FallbackDefaultCodecException, IOException;

        <TAG extends AbstractTag<?, TAG>> TAG visitAtArrayInsert(DataInputStream stream, TagLimiter limiter, TagType type) throws FallbackDefaultCodecException, IOException, IOException;
    }

    interface Out extends NBTVisitor {

        static Out createDefault(NetProtocol protocol, NetProtocol protocolCreated, ItemType type) {
            return new Adapter() {
                @Override
                public ItemType type() {
                    return type;
                }

                @Override
                public NetProtocol protocol() {
                    return protocol;
                }

                @Override
                public NetProtocol protocolCreated() {
                    return protocolCreated;
                }
            };
        }

        abstract class Adapter extends NBTVisitor.Adapter implements Out {

            @Override
            public <TAG extends AbstractTag<?, TAG>> TAG visitAtArrayFlush(DataOutputStream stream, TAG tag) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public Map.Entry<String, AbstractTag<?, ?>> visitAtCompoundFlush(DataOutputStream stream, String key, AbstractTag<?, ?> tag) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }
        }

        <TAG extends AbstractTag<?, TAG>> TAG visitAtArrayFlush(DataOutputStream stream, TAG tag) throws FallbackDefaultCodecException, IOException;

        Map.Entry<String, AbstractTag<?, ?>> visitAtCompoundFlush(DataOutputStream stream, String key, AbstractTag<?, ?> tag) throws FallbackDefaultCodecException, IOException;
    }

    <T> void visitType(TagType type, T val);

    void visitEnd();

    void visitByte(byte val);

    void visitShort(short val);

    void visitInt(int val);

    void visitLong(long val);

    void visitFloat(float val);

    void visitDouble(double val);

    void visitByteArray(byte[] val);

    void visitIntArray(int[] val);

    void visitLongArray(long[] val);

    void visitString(String val);

    <TAG extends AbstractTag<?, TAG>> void visitArray(List<TAG> array, TagType type);

    void visitArrayElement(AbstractTag<?, ?> val);

    void visitCompound(Map<String, AbstractTag<?, ?>> compound);

    void visitCompoundElement(String key, AbstractTag<?, ?> tag);

    String visitCompoundKey(String key, TagType type);
}
