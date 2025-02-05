package me.alek.honey.netty.tag.nbt;

import me.alek.honey.netty.codec.CodecVisitor;
import me.alek.honey.util.protocol.NetProtocol;

import java.util.List;
import java.util.Map;

public interface TagVisitor<STREAM> extends CodecVisitor {

    class FallbackDefaultCodecException extends Exception {

    }

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
        public byte visitAtByte(STREAM stream) throws FallbackDefaultCodecException {
            throw new FallbackDefaultCodecException();
        }

        @Override
        public void visitShort(short val) {

        }

        @Override
        public short visitAtShort(STREAM stream) throws FallbackDefaultCodecException {
            throw new FallbackDefaultCodecException();
        }

        @Override
        public void visitInt(int val) {

        }

        @Override
        public int visitAtInt(STREAM stream) throws FallbackDefaultCodecException {
            throw new FallbackDefaultCodecException();
        }

        @Override
        public void visitLong(long val) {

        }

        @Override
        public long visitAtLong(STREAM stream) throws FallbackDefaultCodecException {
            throw new FallbackDefaultCodecException();
        }

        @Override
        public void visitFloat(float val) {

        }

        @Override
        public float visitAtFloat(STREAM stream) throws FallbackDefaultCodecException {
            throw new FallbackDefaultCodecException();
        }

        @Override
        public void visitDouble(double val) {

        }

        @Override
        public double visitAtDouble(STREAM stream) throws FallbackDefaultCodecException {
            throw new FallbackDefaultCodecException();
        }

        @Override
        public void visitByteArray(byte[] val) {

        }

        @Override
        public byte[] visitAtByteArray(STREAM stream) throws FallbackDefaultCodecException {
            throw new FallbackDefaultCodecException();
        }

        @Override
        public void visitIntArray(int[] val) {

        }

        @Override
        public int[] visitAtIntArray(STREAM stream) throws FallbackDefaultCodecException {
            throw new FallbackDefaultCodecException();
        }

        @Override
        public void visitLongArray(long[] val) {

        }

        @Override
        public long[] visitAtLongArray(STREAM stream) throws FallbackDefaultCodecException {
            throw new FallbackDefaultCodecException();
        }

        @Override
        public void visitString(String val) {

        }

        @Override
        public String visitAtString(STREAM stream) throws FallbackDefaultCodecException {
            throw new FallbackDefaultCodecException();
        }

        @Override
        public <TAG extends AbstractTag<?, ?, TAG>> void visitArray(List<TAG> array) {

        }

        @Override
        public <TAG extends AbstractTag<?, ?, TAG>> List<TAG> visitAtArray(STREAM stream) throws FallbackDefaultCodecException {
            throw new FallbackDefaultCodecException();
        }

        @Override
        public void visitCompound(Map<String, AbstractTag<?, ?, ?>> compound) {

        }

        @Override
        public Map<String, AbstractTag<?, ?, ?>> visitAtCompound(STREAM stream) throws FallbackDefaultCodecException {
            throw new FallbackDefaultCodecException();
        }
    }

    void visitEnd();

    void visitAtEnd(STREAM stream);

    void visitByte(byte val);

    byte visitAtByte(STREAM stream) throws FallbackDefaultCodecException;

    void visitShort(short val);

    short visitAtShort(STREAM stream) throws FallbackDefaultCodecException;

    void visitInt(int val);

    int visitAtInt(STREAM stream) throws FallbackDefaultCodecException;

    void visitLong(long val);

    long visitAtLong(STREAM stream) throws FallbackDefaultCodecException;

    void visitFloat(float val);

    float visitAtFloat(STREAM stream) throws FallbackDefaultCodecException;

    void visitDouble(double val);

    double visitAtDouble(STREAM stream) throws FallbackDefaultCodecException;

    void visitByteArray(byte[] val);

    byte[] visitAtByteArray(STREAM stream) throws FallbackDefaultCodecException;

    void visitIntArray(int[] val);

    int[] visitAtIntArray(STREAM stream) throws FallbackDefaultCodecException;

    void visitLongArray(long[] val);

    long[] visitAtLongArray(STREAM stream) throws FallbackDefaultCodecException;

    void visitString(String val);

    String visitAtString(STREAM stream) throws FallbackDefaultCodecException;

    <TAG extends AbstractTag<?, ?, TAG>> void visitArray(List<TAG> array);

    <TAG extends AbstractTag<?, ?, TAG>> List<TAG> visitAtArray(STREAM stream) throws FallbackDefaultCodecException;

    void visitCompound(Map<String, AbstractTag<?, ?, ?>> compound);

    Map<String, AbstractTag<?, ?, ?>> visitAtCompound(STREAM stream) throws FallbackDefaultCodecException;
}
