package me.alek.honey.netty.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufProcessor;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import me.alek.honey.netty.codec.types.TypeSerializer;
import me.alek.honey.netty.codec.types.Types;
import me.alek.honey.netty.codec.validation.CodecTypeValidation;
import me.alek.honey.netty.codec.validation.DecodeContext;
import me.alek.honey.netty.modelwrappers.WrappedBlockPosition;
import me.alek.honey.netty.modelwrappers.WrappedImmutableItemStack;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.packetwrappers.SourcedWrappedPacket;
import me.alek.honey.netty.tag.nbt.TagVisitor;
import me.alek.honey.util.LazyInit;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.ConstructorInvoker;
import me.alek.honey.util.reflection.MinecraftReflection;
import me.alek.honey.util.reflection.Reflection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.*;

public class PacketDataSerializer extends ByteBuf {

    private static final ConstructorInvoker<?> packetDataSerializerConstructor = Reflection.getConstructor(MinecraftReflection.getPacketDataSerializerClass(), ByteBuf.class);

    public static Object toNMSSerializer(ByteBuf bytebuf) {
        return packetDataSerializerConstructor.invoke(bytebuf);
    }

    private final ByteBuf delegate;
    private final LazyInit<Object> minecraftSerializer;

    public PacketDataSerializer() {
        this(Unpooled.buffer());
    }

    public PacketDataSerializer(ByteBuf buf) {
        this.delegate = buf;
        this.minecraftSerializer = new LazyInit<>(() -> toNMSSerializer(buf));
    }

    public ByteBuf getBuf() {
        return this.delegate;
    }

    public Object getSerializer() {
        return this.minecraftSerializer.get();
    }

    public WrappedBlockPosition readBlockPosition() {
        return WrappedBlockPosition.fromLong(readLong());
    }

    public int readVarInt() {
        return Types.VAR_INT.read(this);
    }

    public PacketDataSerializer writeVarInt(int val) {
        return Types.VAR_INT.write(this, val);
    }

    public PacketDataSerializer writeItemStack(NetProtocol protocol, WrappedImmutableItemStack stack) throws EncoderException {
        return Types.ITEM_STACK.write(this, stack, TypeSerializer.TypeContext.OneArg.of(TagVisitor.Out.createDefault(protocol)));
    }

    public WrappedImmutableItemStack readItemStack(CodecVisitor visitor) throws DecoderException {
        return Optional.ofNullable(Types.ITEM_STACK.read(this, TypeSerializer.TypeContext.OneArg.of(visitor)))
                .orElse(WrappedImmutableItemStack.AIR);
    }

    public WrappedImmutableItemStack readItemStack(NetProtocol protocol) throws DecoderException {
        return readItemStack(TagVisitor.In.createDefault(protocol));
    }

    public <WP extends SourcedWrappedPacket<WP>> WrappedImmutableItemStack readItemStackSafely(CodecVisitor visitor, PacketContainer<WP> packet) throws DecoderException {
        WrappedImmutableItemStack stack = readItemStack(visitor);
        if (stack != WrappedImmutableItemStack.AIR) {
            CodecTypeValidation.ITEM_STACK.validate(stack, DecodeContext.from(packet));
        }

        return stack;
    }

    public <WP extends SourcedWrappedPacket<WP>> WrappedImmutableItemStack readItemStackSafely(NetProtocol protocol, PacketContainer<WP> packet) throws DecoderException {
        return readItemStackSafely(TagVisitor.In.createDefault(protocol), packet);
    }

    public <K, V> Map<K, V> readMap(Supplier<K> keyFunction, Supplier<V> valueFunction) {
        int size = readVarInt();
        Map<K, V> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            K key = keyFunction.get();
            V value = valueFunction.get();
            map.put(key, value);
        }
        return map;
    }

    public <K, V> void writeMap(Map<K, V> map, Consumer<K> keyConsumer, Consumer<V> valueConsumer) {
        writeVarInt(map.size());
        for (Map.Entry<K, V> entry : map.entrySet()) {
            K key = entry.getKey();
            V value = entry.getValue();
            keyConsumer.accept(key);
            valueConsumer.accept(value);
        }
    }

    public UUID readUUID() {
        return new UUID(this.readLong(), this.readLong());
    }

    public String readString() {
        return readString(Short.MAX_VALUE);
    }

    public String readString(int i) {
        return Types.UTF8.read(this, TypeSerializer.TypeContext.OneArg.of(i));
    }

    public PacketDataSerializer writeString(String val) {
        return writeString(val, Short.MAX_VALUE);
    }

    public PacketDataSerializer writeString(String val, int max) {
        return Types.UTF8.write(this, val, TypeSerializer.TypeContext.OneArg.of(max));
    }

    public PacketDataSerializer reverse() {
        int readerIndex = readerIndex();
        int writerIndex = writerIndex();

        int length = writerIndex - readerIndex;
        for (int i = 0; i < length / 2; i++) {
            byte temp = getByte(readerIndex + i);
            setByte(readerIndex + i, getByte(writerIndex - 1 - i));
            setByte(writerIndex - 1 - i, temp);
        }
        return this;
    }

    public byte[] readByteArray(int maxLength) {
        int len = this.readVarInt();
        if (len > maxLength) {
            throw new RuntimeException("The received byte array length is longer than maximum allowed (" + len + " > " + maxLength + ")");
        } else {
            return this.readBytes(len).array();
        }
    }

    public int capacity() {
        return this.delegate.capacity();
    }

    public ByteBuf capacity(int i) {
        return this.delegate.capacity(i);
    }

    public int maxCapacity() {
        return this.delegate.maxCapacity();
    }

    public ByteBufAllocator alloc() {
        return this.delegate.alloc();
    }

    public ByteOrder order() {
        return this.delegate.order();
    }

    public ByteBuf order(ByteOrder byteorder) {
        return this.delegate.order(byteorder);
    }

    public ByteBuf unwrap() {
        return this.delegate.unwrap();
    }

    public boolean isDirect() {
        return this.delegate.isDirect();
    }

    public int readerIndex() {
        return this.delegate.readerIndex();
    }

    public ByteBuf readerIndex(int i) {
        return this.delegate.readerIndex(i);
    }

    public int writerIndex() {
        return this.delegate.writerIndex();
    }

    public ByteBuf writerIndex(int i) {
        return this.delegate.writerIndex(i);
    }

    public ByteBuf setIndex(int i, int j) {
        return this.delegate.setIndex(i, j);
    }

    public int readableBytes() {
        return this.delegate.readableBytes();
    }

    public int writableBytes() {
        return this.delegate.writableBytes();
    }

    public int maxWritableBytes() {
        return this.delegate.maxWritableBytes();
    }

    public boolean isReadable() {
        return this.delegate.isReadable();
    }

    public boolean isReadable(int i) {
        return this.delegate.isReadable(i);
    }

    public boolean isWritable() {
        return this.delegate.isWritable();
    }

    public boolean isWritable(int i) {
        return this.delegate.isWritable(i);
    }

    public ByteBuf clear() {
        return this.delegate.clear();
    }

    public ByteBuf markReaderIndex() {
        return this.delegate.markReaderIndex();
    }

    public ByteBuf resetReaderIndex() {
        return this.delegate.resetReaderIndex();
    }

    public ByteBuf markWriterIndex() {
        return this.delegate.markWriterIndex();
    }

    public ByteBuf resetWriterIndex() {
        return this.delegate.resetWriterIndex();
    }

    public ByteBuf discardReadBytes() {
        return this.delegate.discardReadBytes();
    }

    public ByteBuf discardSomeReadBytes() {
        return this.delegate.discardSomeReadBytes();
    }

    public ByteBuf ensureWritable(int i) {
        return this.delegate.ensureWritable(i);
    }

    public int ensureWritable(int i, boolean flag) {
        return this.delegate.ensureWritable(i, flag);
    }

    public boolean getBoolean(int i) {
        return this.delegate.getBoolean(i);
    }

    public byte getByte(int i) {
        return this.delegate.getByte(i);
    }

    public short getUnsignedByte(int i) {
        return this.delegate.getUnsignedByte(i);
    }

    public short getShort(int i) {
        return this.delegate.getShort(i);
    }

    public int getUnsignedShort(int i) {
        return this.delegate.getUnsignedShort(i);
    }

    public int getMedium(int i) {
        return this.delegate.getMedium(i);
    }

    public int getUnsignedMedium(int i) {
        return this.delegate.getUnsignedMedium(i);
    }

    public int getInt(int i) {
        return this.delegate.getInt(i);
    }

    public long getUnsignedInt(int i) {
        return this.delegate.getUnsignedInt(i);
    }

    public long getLong(int i) {
        return this.delegate.getLong(i);
    }

    public char getChar(int i) {
        return this.delegate.getChar(i);
    }

    public float getFloat(int i) {
        return this.delegate.getFloat(i);
    }

    public double getDouble(int i) {
        return this.delegate.getDouble(i);
    }

    public ByteBuf getBytes(int i, ByteBuf bytebuf) {
        return this.delegate.getBytes(i, bytebuf);
    }

    public ByteBuf getBytes(int i, ByteBuf bytebuf, int j) {
        return this.delegate.getBytes(i, bytebuf, j);
    }

    public ByteBuf getBytes(int i, ByteBuf bytebuf, int j, int k) {
        return this.delegate.getBytes(i, bytebuf, j, k);
    }

    public ByteBuf getBytes(int i, byte[] abyte) {
        return this.delegate.getBytes(i, abyte);
    }

    public ByteBuf getBytes(int i, byte[] abyte, int j, int k) {
        return this.delegate.getBytes(i, abyte, j, k);
    }

    public ByteBuf getBytes(int i, ByteBuffer bytebuffer) {
        return this.delegate.getBytes(i, bytebuffer);
    }

    public ByteBuf getBytes(int i, OutputStream outputstream, int j) throws IOException {
        return this.delegate.getBytes(i, outputstream, j);
    }

    public int getBytes(int i, GatheringByteChannel gatheringbytechannel, int j) throws IOException {
        return this.delegate.getBytes(i, gatheringbytechannel, j);
    }

    public ByteBuf setBoolean(int i, boolean flag) {
        return this.delegate.setBoolean(i, flag);
    }

    public ByteBuf setByte(int i, int j) {
        return this.delegate.setByte(i, j);
    }

    public ByteBuf setShort(int i, int j) {
        return this.delegate.setShort(i, j);
    }

    public ByteBuf setMedium(int i, int j) {
        return this.delegate.setMedium(i, j);
    }

    public ByteBuf setInt(int i, int j) {
        return this.delegate.setInt(i, j);
    }

    public ByteBuf setLong(int i, long j) {
        return this.delegate.setLong(i, j);
    }

    public ByteBuf setChar(int i, int j) {
        return this.delegate.setChar(i, j);
    }

    public ByteBuf setFloat(int i, float f) {
        return this.delegate.setFloat(i, f);
    }

    public ByteBuf setDouble(int i, double d0) {
        return this.delegate.setDouble(i, d0);
    }

    public ByteBuf setBytes(int i, ByteBuf bytebuf) {
        return this.delegate.setBytes(i, bytebuf);
    }

    public ByteBuf setBytes(int i, ByteBuf bytebuf, int j) {
        return this.delegate.setBytes(i, bytebuf, j);
    }

    public ByteBuf setBytes(int i, ByteBuf bytebuf, int j, int k) {
        return this.delegate.setBytes(i, bytebuf, j, k);
    }

    public ByteBuf setBytes(int i, byte[] abyte) {
        return this.delegate.setBytes(i, abyte);
    }

    public ByteBuf setBytes(int i, byte[] abyte, int j, int k) {
        return this.delegate.setBytes(i, abyte, j, k);
    }

    public ByteBuf setBytes(int i, ByteBuffer bytebuffer) {
        return this.delegate.setBytes(i, bytebuffer);
    }

    public int setBytes(int i, InputStream inputstream, int j) throws IOException {
        return this.delegate.setBytes(i, inputstream, j);
    }

    public int setBytes(int i, ScatteringByteChannel scatteringbytechannel, int j) throws IOException {
        return this.delegate.setBytes(i, scatteringbytechannel, j);
    }

    public ByteBuf setZero(int i, int j) {
        return this.delegate.setZero(i, j);
    }

    public boolean readBoolean() {
        return this.delegate.readBoolean();
    }

    public byte readByte() {
        return this.delegate.readByte();
    }

    public short readUnsignedByte() {
        return this.delegate.readUnsignedByte();
    }

    public short readShort() {
        return this.delegate.readShort();
    }

    public int readUnsignedShort() {
        return this.delegate.readUnsignedShort();
    }

    public int readMedium() {
        return this.delegate.readMedium();
    }

    public int readUnsignedMedium() {
        return this.delegate.readUnsignedMedium();
    }

    public int readInt() {
        return this.delegate.readInt();
    }

    public long readUnsignedInt() {
        return this.delegate.readUnsignedInt();
    }

    public long readLong() {
        return this.delegate.readLong();
    }

    public char readChar() {
        return this.delegate.readChar();
    }

    public float readFloat() {
        return this.delegate.readFloat();
    }

    public double readDouble() {
        return this.delegate.readDouble();
    }

    public ByteBuf readBytes(int i) {
        return this.delegate.readBytes(i);
    }

    public ByteBuf readSlice(int i) {
        return this.delegate.readSlice(i);
    }

    public ByteBuf readBytes(ByteBuf bytebuf) {
        return this.delegate.readBytes(bytebuf);
    }

    public ByteBuf readBytes(ByteBuf bytebuf, int i) {
        return this.delegate.readBytes(bytebuf, i);
    }

    public ByteBuf readBytes(ByteBuf bytebuf, int i, int j) {
        return this.delegate.readBytes(bytebuf, i, j);
    }

    public ByteBuf readBytes(byte[] abyte) {
        return this.delegate.readBytes(abyte);
    }

    public ByteBuf readBytes(byte[] abyte, int i, int j) {
        return this.delegate.readBytes(abyte, i, j);
    }

    public ByteBuf readBytes(ByteBuffer bytebuffer) {
        return this.delegate.readBytes(bytebuffer);
    }

    public ByteBuf readBytes(OutputStream outputstream, int i) throws IOException {
        return this.delegate.readBytes(outputstream, i);
    }

    public int readBytes(GatheringByteChannel gatheringbytechannel, int i) throws IOException {
        return this.delegate.readBytes(gatheringbytechannel, i);
    }

    public ByteBuf skipBytes(int i) {
        return this.delegate.skipBytes(i);
    }

    public ByteBuf writeBoolean(boolean flag) {
        return this.delegate.writeBoolean(flag);
    }

    public ByteBuf writeByte(int i) {
        return this.delegate.writeByte(i);
    }

    public ByteBuf writeShort(int i) {
        return this.delegate.writeShort(i);
    }

    public ByteBuf writeMedium(int i) {
        return this.delegate.writeMedium(i);
    }

    public ByteBuf writeInt(int i) {
        return this.delegate.writeInt(i);
    }

    public ByteBuf writeLong(long i) {
        return this.delegate.writeLong(i);
    }

    public ByteBuf writeChar(int i) {
        return this.delegate.writeChar(i);
    }

    public ByteBuf writeFloat(float f) {
        return this.delegate.writeFloat(f);
    }

    public ByteBuf writeDouble(double d0) {
        return this.delegate.writeDouble(d0);
    }

    public ByteBuf writeBytes(ByteBuf bytebuf) {
        return this.delegate.writeBytes(bytebuf);
    }

    public ByteBuf writeBytes(ByteBuf bytebuf, int i) {
        return this.delegate.writeBytes(bytebuf, i);
    }

    public ByteBuf writeBytes(ByteBuf bytebuf, int i, int j) {
        return this.delegate.writeBytes(bytebuf, i, j);
    }

    public ByteBuf writeBytes(byte[] abyte) {
        return this.delegate.writeBytes(abyte);
    }

    public ByteBuf writeBytes(byte[] abyte, int i, int j) {
        return this.delegate.writeBytes(abyte, i, j);
    }

    public ByteBuf writeBytes(ByteBuffer bytebuffer) {
        return this.delegate.writeBytes(bytebuffer);
    }

    public int writeBytes(InputStream inputstream, int i) throws IOException {
        return this.delegate.writeBytes(inputstream, i);
    }

    public int writeBytes(ScatteringByteChannel scatteringbytechannel, int i) throws IOException {
        return this.delegate.writeBytes(scatteringbytechannel, i);
    }

    public ByteBuf writeZero(int i) {
        return this.delegate.writeZero(i);
    }

    public int indexOf(int i, int j, byte b0) {
        return this.delegate.indexOf(i, j, b0);
    }

    public int bytesBefore(byte b0) {
        return this.delegate.bytesBefore(b0);
    }

    public int bytesBefore(int i, byte b0) {
        return this.delegate.bytesBefore(i, b0);
    }

    public int bytesBefore(int i, int j, byte b0) {
        return this.delegate.bytesBefore(i, j, b0);
    }

    @Override
    public int forEachByte(ByteBufProcessor byteBufProcessor) {
        return this.delegate.forEachByte(byteBufProcessor);
    }

    @Override
    public int forEachByte(int i, int i1, ByteBufProcessor byteBufProcessor) {
        return this.delegate.forEachByte(i, i1, byteBufProcessor);
    }

    @Override
    public int forEachByteDesc(ByteBufProcessor byteBufProcessor) {
        return this.delegate.forEachByteDesc(byteBufProcessor);
    }

    @Override
    public int forEachByteDesc(int i, int i1, ByteBufProcessor byteBufProcessor) {
        return this.delegate.forEachByteDesc(i, i1, byteBufProcessor);
    }

    public ByteBuf copy() {
        return this.delegate.copy();
    }

    public ByteBuf copy(int i, int j) {
        return this.delegate.copy(i, j);
    }

    public ByteBuf slice() {
        return this.delegate.slice();
    }

    public ByteBuf slice(int i, int j) {
        return this.delegate.slice(i, j);
    }

    public ByteBuf duplicate() {
        return this.delegate.duplicate();
    }

    public int nioBufferCount() {
        return this.delegate.nioBufferCount();
    }

    public ByteBuffer nioBuffer() {
        return this.delegate.nioBuffer();
    }

    public ByteBuffer nioBuffer(int i, int j) {
        return this.delegate.nioBuffer(i, j);
    }

    public ByteBuffer internalNioBuffer(int i, int j) {
        return this.delegate.internalNioBuffer(i, j);
    }

    public ByteBuffer[] nioBuffers() {
        return this.delegate.nioBuffers();
    }

    public ByteBuffer[] nioBuffers(int i, int j) {
        return this.delegate.nioBuffers(i, j);
    }

    public boolean hasArray() {
        return this.delegate.hasArray();
    }

    public byte[] array() {
        return this.delegate.array();
    }

    public int arrayOffset() {
        return this.delegate.arrayOffset();
    }

    public boolean hasMemoryAddress() {
        return this.delegate.hasMemoryAddress();
    }

    public long memoryAddress() {
        return this.delegate.memoryAddress();
    }

    public String toString(Charset charset) {
        return this.delegate.toString(charset);
    }

    public String toString(int i, int j, Charset charset) {
        return this.delegate.toString(i, j, charset);
    }

    public int hashCode() {
        return this.delegate.hashCode();
    }

    public boolean equals(Object object) {
        return this.delegate.equals(object);
    }

    public int compareTo(ByteBuf bytebuf) {
        return this.delegate.compareTo(bytebuf);
    }

    public String toString() {
        return this.delegate.toString();
    }

    public ByteBuf retain(int i) {
        return this.delegate.retain(i);
    }

    public ByteBuf retain() {
        return this.delegate.retain();
    }

    public int refCnt() {
        return this.delegate.refCnt();
    }

    public boolean release() {
        return this.delegate.release();
    }

    public boolean release(int i) {
        return this.delegate.release(i);
    }
}
