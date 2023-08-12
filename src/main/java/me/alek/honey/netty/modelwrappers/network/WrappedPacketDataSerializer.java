package me.alek.honey.netty.modelwrappers.network;

import com.google.common.base.Charsets;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufProcessor;
import io.netty.handler.codec.DecoderException;
import me.alek.honey.netty.modelwrappers.WrappedObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;
import java.util.UUID;

public class WrappedPacketDataSerializer extends WrappedObject<WrappedPacketDataSerializer> {

    private final ByteBuf handle;

    public WrappedPacketDataSerializer(Object handle, ByteBuf byteBufHandle) {
        super(handle);
        this.handle = byteBufHandle;
    }

    public WrappedPacketDataSerializer(Object handle) {
        super(handle);
        this.handle = getModifier(ByteBuf.class).readField(0);
    }

    public int readVarInt() {
        byte b0;
        int i = 0;
        int j = 0;
        do {
            b0 = readByte();
            i |= (b0 & Byte.MAX_VALUE) << j++ * 7;
            if (j > 5)
                throw new RuntimeException("VarInt too big");
        } while ((b0 & 0x80) == 128);
        return i;
    }

    public long readVarLong() {
        byte b0;
        long i = 0L;
        int j = 0;
        do {
            b0 = readByte();
            i |= (long) (b0 & Byte.MAX_VALUE) << j++ * 7;
            if (j > 10)
                throw new RuntimeException("VarLong too big");
        } while ((b0 & 0x80) == 128);
        return i;
    }

    public UUID readUUID() {
        return new UUID(this.readLong(), this.readLong());
    }

    public String readString(int i) {
        int j = this.readVarInt();
        if (j > i * 4) {
            throw new DecoderException("The received encoded string buffer length is longer than maximum allowed (" + j + " > " + i * 4 + ")");
        }
        if (j < 0) {
            throw new DecoderException("The received encoded string buffer length is less than zero! Weird string!");
        }
        String s = new String(this.readBytes(j).array(), Charsets.UTF_8);
        if (s.length() > i) {
            throw new DecoderException("The received string length is longer than maximum allowed (" + j + " > " + i + ")");
        }
        return s;
    }

    public int capacity() {
        return this.handle.capacity();
    }

    public ByteBuf capacity(int i) {
        return this.handle.capacity(i);
    }

    public int maxCapacity() {
        return this.handle.maxCapacity();
    }

    public ByteBufAllocator alloc() {
        return this.handle.alloc();
    }

    public ByteOrder order() {
        return this.handle.order();
    }

    public ByteBuf order(ByteOrder byteorder) {
        return this.handle.order(byteorder);
    }

    public ByteBuf unwrap() {
        return this.handle.unwrap();
    }

    public boolean isDirect() {
        return this.handle.isDirect();
    }

    public int readerIndex() {
        return this.handle.readerIndex();
    }

    public ByteBuf readerIndex(int i) {
        return this.handle.readerIndex(i);
    }

    public int writerIndex() {
        return this.handle.writerIndex();
    }

    public ByteBuf writerIndex(int i) {
        return this.handle.writerIndex(i);
    }

    public ByteBuf setIndex(int i, int j) {
        return this.handle.setIndex(i, j);
    }

    public int readableBytes() {
        return this.handle.readableBytes();
    }

    public int writableBytes() {
        return this.handle.writableBytes();
    }

    public int maxWritableBytes() {
        return this.handle.maxWritableBytes();
    }

    public boolean isReadable() {
        return this.handle.isReadable();
    }

    public boolean isReadable(int i) {
        return this.handle.isReadable(i);
    }

    public boolean isWritable() {
        return this.handle.isWritable();
    }

    public boolean isWritable(int i) {
        return this.handle.isWritable(i);
    }

    public ByteBuf clear() {
        return this.handle.clear();
    }

    public ByteBuf markReaderIndex() {
        return this.handle.markReaderIndex();
    }

    public ByteBuf resetReaderIndex() {
        return this.handle.resetReaderIndex();
    }

    public ByteBuf markWriterIndex() {
        return this.handle.markWriterIndex();
    }

    public ByteBuf resetWriterIndex() {
        return this.handle.resetWriterIndex();
    }

    public ByteBuf discardReadBytes() {
        return this.handle.discardReadBytes();
    }

    public ByteBuf discardSomeReadBytes() {
        return this.handle.discardSomeReadBytes();
    }

    public ByteBuf ensureWritable(int i) {
        return this.handle.ensureWritable(i);
    }

    public int ensureWritable(int i, boolean flag) {
        return this.handle.ensureWritable(i, flag);
    }

    public boolean getBoolean(int i) {
        return this.handle.getBoolean(i);
    }

    public byte getByte(int i) {
        return this.handle.getByte(i);
    }

    public short getUnsignedByte(int i) {
        return this.handle.getUnsignedByte(i);
    }

    public short getShort(int i) {
        return this.handle.getShort(i);
    }

    public int getUnsignedShort(int i) {
        return this.handle.getUnsignedShort(i);
    }

    public int getMedium(int i) {
        return this.handle.getMedium(i);
    }

    public int getUnsignedMedium(int i) {
        return this.handle.getUnsignedMedium(i);
    }

    public int getInt(int i) {
        return this.handle.getInt(i);
    }

    public long getUnsignedInt(int i) {
        return this.handle.getUnsignedInt(i);
    }

    public long getLong(int i) {
        return this.handle.getLong(i);
    }

    public char getChar(int i) {
        return this.handle.getChar(i);
    }

    public float getFloat(int i) {
        return this.handle.getFloat(i);
    }

    public double getDouble(int i) {
        return this.handle.getDouble(i);
    }

    public ByteBuf getBytes(int i, ByteBuf bytebuf) {
        return this.handle.getBytes(i, bytebuf);
    }

    public ByteBuf getBytes(int i, ByteBuf bytebuf, int j) {
        return this.handle.getBytes(i, bytebuf, j);
    }

    public ByteBuf getBytes(int i, ByteBuf bytebuf, int j, int k) {
        return this.handle.getBytes(i, bytebuf, j, k);
    }

    public ByteBuf getBytes(int i, byte[] abyte) {
        return this.handle.getBytes(i, abyte);
    }

    public ByteBuf getBytes(int i, byte[] abyte, int j, int k) {
        return this.handle.getBytes(i, abyte, j, k);
    }

    public ByteBuf getBytes(int i, ByteBuffer bytebuffer) {
        return this.handle.getBytes(i, bytebuffer);
    }

    public ByteBuf getBytes(int i, OutputStream outputstream, int j) throws IOException {
        return this.handle.getBytes(i, outputstream, j);
    }

    public int getBytes(int i, GatheringByteChannel gatheringbytechannel, int j) throws IOException {
        return this.handle.getBytes(i, gatheringbytechannel, j);
    }

    public ByteBuf setBoolean(int i, boolean flag) {
        return this.handle.setBoolean(i, flag);
    }

    public ByteBuf setByte(int i, int j) {
        return this.handle.setByte(i, j);
    }

    public ByteBuf setShort(int i, int j) {
        return this.handle.setShort(i, j);
    }

    public ByteBuf setMedium(int i, int j) {
        return this.handle.setMedium(i, j);
    }

    public ByteBuf setInt(int i, int j) {
        return this.handle.setInt(i, j);
    }

    public ByteBuf setLong(int i, long j) {
        return this.handle.setLong(i, j);
    }

    public ByteBuf setChar(int i, int j) {
        return this.handle.setChar(i, j);
    }

    public ByteBuf setFloat(int i, float f) {
        return this.handle.setFloat(i, f);
    }

    public ByteBuf setDouble(int i, double d0) {
        return this.handle.setDouble(i, d0);
    }

    public ByteBuf setBytes(int i, ByteBuf bytebuf) {
        return this.handle.setBytes(i, bytebuf);
    }

    public ByteBuf setBytes(int i, ByteBuf bytebuf, int j) {
        return this.handle.setBytes(i, bytebuf, j);
    }

    public ByteBuf setBytes(int i, ByteBuf bytebuf, int j, int k) {
        return this.handle.setBytes(i, bytebuf, j, k);
    }

    public ByteBuf setBytes(int i, byte[] abyte) {
        return this.handle.setBytes(i, abyte);
    }

    public ByteBuf setBytes(int i, byte[] abyte, int j, int k) {
        return this.handle.setBytes(i, abyte, j, k);
    }

    public ByteBuf setBytes(int i, ByteBuffer bytebuffer) {
        return this.handle.setBytes(i, bytebuffer);
    }

    public int setBytes(int i, InputStream inputstream, int j) throws IOException {
        return this.handle.setBytes(i, inputstream, j);
    }

    public int setBytes(int i, ScatteringByteChannel scatteringbytechannel, int j) throws IOException {
        return this.handle.setBytes(i, scatteringbytechannel, j);
    }

    public ByteBuf setZero(int i, int j) {
        return this.handle.setZero(i, j);
    }

    public boolean readBoolean() {
        return this.handle.readBoolean();
    }

    public byte readByte() {
        return this.handle.readByte();
    }

    public short readUnsignedByte() {
        return this.handle.readUnsignedByte();
    }

    public short readShort() {
        return this.handle.readShort();
    }

    public int readUnsignedShort() {
        return this.handle.readUnsignedShort();
    }

    public int readMedium() {
        return this.handle.readMedium();
    }

    public int readUnsignedMedium() {
        return this.handle.readUnsignedMedium();
    }

    public int readInt() {
        return this.handle.readInt();
    }

    public long readUnsignedInt() {
        return this.handle.readUnsignedInt();
    }

    public long readLong() {
        return this.handle.readLong();
    }

    public char readChar() {
        return this.handle.readChar();
    }

    public float readFloat() {
        return this.handle.readFloat();
    }

    public double readDouble() {
        return this.handle.readDouble();
    }

    public ByteBuf readBytes(int i) {
        return this.handle.readBytes(i);
    }

    public ByteBuf readSlice(int i) {
        return this.handle.readSlice(i);
    }

    public ByteBuf readBytes(ByteBuf bytebuf) {
        return this.handle.readBytes(bytebuf);
    }

    public ByteBuf readBytes(ByteBuf bytebuf, int i) {
        return this.handle.readBytes(bytebuf, i);
    }

    public ByteBuf readBytes(ByteBuf bytebuf, int i, int j) {
        return this.handle.readBytes(bytebuf, i, j);
    }

    public ByteBuf readBytes(byte[] abyte) {
        return this.handle.readBytes(abyte);
    }

    public ByteBuf readBytes(byte[] abyte, int i, int j) {
        return this.handle.readBytes(abyte, i, j);
    }

    public ByteBuf readBytes(ByteBuffer bytebuffer) {
        return this.handle.readBytes(bytebuffer);
    }

    public ByteBuf readBytes(OutputStream outputstream, int i) throws IOException {
        return this.handle.readBytes(outputstream, i);
    }

    public int readBytes(GatheringByteChannel gatheringbytechannel, int i) throws IOException {
        return this.handle.readBytes(gatheringbytechannel, i);
    }

    public ByteBuf skipBytes(int i) {
        return this.handle.skipBytes(i);
    }

    public ByteBuf writeBoolean(boolean flag) {
        return this.handle.writeBoolean(flag);
    }

    public ByteBuf writeByte(int i) {
        return this.handle.writeByte(i);
    }

    public ByteBuf writeShort(int i) {
        return this.handle.writeShort(i);
    }

    public ByteBuf writeMedium(int i) {
        return this.handle.writeMedium(i);
    }

    public ByteBuf writeInt(int i) {
        return this.handle.writeInt(i);
    }

    public ByteBuf writeLong(long i) {
        return this.handle.writeLong(i);
    }

    public ByteBuf writeChar(int i) {
        return this.handle.writeChar(i);
    }

    public ByteBuf writeFloat(float f) {
        return this.handle.writeFloat(f);
    }

    public ByteBuf writeDouble(double d0) {
        return this.handle.writeDouble(d0);
    }

    public ByteBuf writeBytes(ByteBuf bytebuf) {
        return this.handle.writeBytes(bytebuf);
    }

    public ByteBuf writeBytes(ByteBuf bytebuf, int i) {
        return this.handle.writeBytes(bytebuf, i);
    }

    public ByteBuf writeBytes(ByteBuf bytebuf, int i, int j) {
        return this.handle.writeBytes(bytebuf, i, j);
    }

    public ByteBuf writeBytes(byte[] abyte) {
        return this.handle.writeBytes(abyte);
    }

    public ByteBuf writeBytes(byte[] abyte, int i, int j) {
        return this.handle.writeBytes(abyte, i, j);
    }

    public ByteBuf writeBytes(ByteBuffer bytebuffer) {
        return this.handle.writeBytes(bytebuffer);
    }

    public int writeBytes(InputStream inputstream, int i) throws IOException {
        return this.handle.writeBytes(inputstream, i);
    }

    public int writeBytes(ScatteringByteChannel scatteringbytechannel, int i) throws IOException {
        return this.handle.writeBytes(scatteringbytechannel, i);
    }

    public ByteBuf writeZero(int i) {
        return this.handle.writeZero(i);
    }

    public int indexOf(int i, int j, byte b0) {
        return this.handle.indexOf(i, j, b0);
    }

    public int bytesBefore(byte b0) {
        return this.handle.bytesBefore(b0);
    }

    public int bytesBefore(int i, byte b0) {
        return this.handle.bytesBefore(i, b0);
    }

    public int bytesBefore(int i, int j, byte b0) {
        return this.handle.bytesBefore(i, j, b0);
    }

    public int forEachByte(ByteBufProcessor bytebufprocessor) {
        return this.handle.forEachByte(bytebufprocessor);
    }

    public int forEachByte(int i, int j, ByteBufProcessor bytebufprocessor) {
        return this.handle.forEachByte(i, j, bytebufprocessor);
    }

    public int forEachByteDesc(ByteBufProcessor bytebufprocessor) {
        return this.handle.forEachByteDesc(bytebufprocessor);
    }

    public int forEachByteDesc(int i, int j, ByteBufProcessor bytebufprocessor) {
        return this.handle.forEachByteDesc(i, j, bytebufprocessor);
    }

    public ByteBuf copy() {
        return this.handle.copy();
    }

    public ByteBuf copy(int i, int j) {
        return this.handle.copy(i, j);
    }

    public ByteBuf slice() {
        return this.handle.slice();
    }

    public ByteBuf slice(int i, int j) {
        return this.handle.slice(i, j);
    }

    public ByteBuf duplicate() {
        return this.handle.duplicate();
    }

    public int nioBufferCount() {
        return this.handle.nioBufferCount();
    }

    public ByteBuffer nioBuffer() {
        return this.handle.nioBuffer();
    }

    public ByteBuffer nioBuffer(int i, int j) {
        return this.handle.nioBuffer(i, j);
    }

    public ByteBuffer internalNioBuffer(int i, int j) {
        return this.handle.internalNioBuffer(i, j);
    }

    public ByteBuffer[] nioBuffers() {
        return this.handle.nioBuffers();
    }

    public ByteBuffer[] nioBuffers(int i, int j) {
        return this.handle.nioBuffers(i, j);
    }

    public boolean hasArray() {
        return this.handle.hasArray();
    }

    public byte[] array() {
        return this.handle.array();
    }

    public int arrayOffset() {
        return this.handle.arrayOffset();
    }

    public boolean hasMemoryAddress() {
        return this.handle.hasMemoryAddress();
    }

    public long memoryAddress() {
        return this.handle.memoryAddress();
    }

    public String toString(Charset charset) {
        return this.handle.toString(charset);
    }

    public String toString(int i, int j, Charset charset) {
        return this.handle.toString(i, j, charset);
    }

    public int hashCode() {
        return this.handle.hashCode();
    }

    public boolean equals(Object object) {
        return this.handle.equals(object);
    }

    public int compareTo(ByteBuf bytebuf) {
        return this.handle.compareTo(bytebuf);
    }

    public String toString() {
        return this.handle.toString();
    }

    public ByteBuf retain(int i) {
        return this.handle.retain(i);
    }

    public ByteBuf retain() {
        return this.handle.retain();
    }

    public int refCnt() {
        return this.handle.refCnt();
    }

    public boolean release() {
        return this.handle.release();
    }

    public boolean release(int i) {
        return this.handle.release(i);
    }
}
