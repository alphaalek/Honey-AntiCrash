package me.alek.honey.netty.tag.nbt;

import me.alek.honey.netty.codec.CodecVisitor;
import me.alek.honey.util.protocol.NetProtocol;

import java.util.Iterator;
import java.util.Map;

public interface TagVisitor<STREAM> extends CodecVisitor {

    static <STREAM> TagVisitor<STREAM> createDefault(NetProtocol protocol) {
        return new Adapter<STREAM>() {
            @Override
            public NetProtocol protocol() {
                return protocol;
            }
        };
    }

    abstract class Adapter<STREAM> implements TagVisitor<STREAM> {

        @Override
        public void visitEnd() {

        }

        @Override
        public void visitAtEnd(STREAM stream) {

        }

        @Override
        public void visitByte(byte val) {

        }

        @Override
        public void visitAtByte(STREAM stream) {

        }

        @Override
        public void visitShort(short val) {

        }

        @Override
        public void visitAtShort(STREAM stream) {

        }

        @Override
        public void visitInt(int val) {

        }

        @Override
        public void visitAtInt(STREAM stream) {

        }

        @Override
        public void visitLong(long val) {

        }

        @Override
        public void visitAtLong(STREAM stream) {

        }

        @Override
        public void visitFloat(float val) {

        }

        @Override
        public void visitAtFloat(STREAM stream) {

        }

        @Override
        public void visitDouble(double val) {

        }

        @Override
        public void visitAtDouble(STREAM stream) {

        }

        @Override
        public void visitByteArray(byte[] val) {

        }

        @Override
        public void visitAtByteArray(STREAM stream) {

        }

        @Override
        public void visitIntArray(int[] val) {

        }

        @Override
        public void visitAtIntArray(STREAM stream) {

        }

        @Override
        public void visitLongArray(long[] val) {

        }

        @Override
        public void visitAtLongArray(STREAM stream) {

        }

        @Override
        public void visitString(String val) {

        }

        @Override
        public void visitAtString(STREAM stream) {

        }

        @Override
        public <TAG extends AbstractTag<?, ?, TAG>> void visitArray(Iterable<TAG> array) {

        }

        @Override
        public void visitAtArray(STREAM stream) {

        }

        @Override
        public void visitCompound(Map<String, AbstractTag<?, ?, ?>> compound) {

        }

        @Override
        public void visitAtCompound(STREAM stream) {

        }
    }

    void visitEnd();

    void visitAtEnd(STREAM stream);

    void visitByte(byte val);

    void visitAtByte(STREAM stream);

    void visitShort(short val);

    void visitAtShort(STREAM stream);

    void visitInt(int val);

    void visitAtInt(STREAM stream);

    void visitLong(long val);

    void visitAtLong(STREAM stream);

    void visitFloat(float val);

    void visitAtFloat(STREAM stream);

    void visitDouble(double val);

    void visitAtDouble(STREAM stream);

    void visitByteArray(byte[] val);

    void visitAtByteArray(STREAM stream);

    void visitIntArray(int[] val);

    void visitAtIntArray(STREAM stream);

    void visitLongArray(long[] val);

    void visitAtLongArray(STREAM stream);

    void visitString(String val);

    void visitAtString(STREAM stream);

    <TAG extends AbstractTag<?, ?, TAG>> void visitArray(Iterable<TAG> array);

    void visitAtArray(STREAM stream);

    void visitCompound(Map<String, AbstractTag<?, ?, ?>> compound);

    void visitAtCompound(STREAM stream);
}
